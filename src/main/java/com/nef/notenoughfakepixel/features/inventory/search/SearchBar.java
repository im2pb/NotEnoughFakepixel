/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Original code and third-party components remain subject to their respective copyrights and license terms.
 */
package com.nef.notenoughfakepixel.features.inventory.search;

import com.nef.notenoughfakepixel.config.features.SearchBarConfig;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.mixin.accesors.AccessorGuiContainer;
import com.nef.notenoughfakepixel.utils.CalculatorEngine;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;

/**
 * Search overlay for the player's inventory and explicitly recognized storage containers.
 * Search is highlight-only: every stack remains rendered in its original slot and every normal
 * container interaction remains available. Matching stacks receive a cosmetic highlight.
 */
public class SearchBar {
    private static final int MAX_TEXT_LENGTH = 120;
    private static final int CLEAR_BUTTON_WIDTH = 16;
    private static final int TEXT_INSET_X = 5;
    private static final int DEFAULT_WIDTH = 140;
    private static final int DEFAULT_HEIGHT = 18;

    private GuiScreen activeScreen;
    private GuiTextField field;
    private int outerX;
    private int outerY;
    private int fieldOuterWidth;
    private int fieldOuterHeight;
    private String activeQuery = "";
    private String activeConsumer = "main";
    private Double cachedCalculation;
    private boolean cachedCalculationExpression;
    private String cachedCalculationInput = "";

    private static GuiContainer cachedContextGui;
    private static Object cachedContextContainer;
    private static SearchContainerPolicy.Kind cachedContextKind = SearchContainerPolicy.Kind.NONE;
    private static String cachedQueryConsumer = "main";
    private static String cachedQueryValue = "";

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (activeScreen != null && activeScreen != event.gui) saveCurrentQuery();
        activeScreen = event.gui;
        field = null;
        activeConsumer = "main";
        SearchMatcher.clearCache();
        invalidateGuiQueryCache();
        invalidateCalculation();

        // Prepare the query before the container's first draw so the highlight state is in sync
        // with the field as soon as a new inventory or storage screen opens.
        if (event.gui instanceof GuiContainer && isSearchEnabled() && isSupportedGui(event.gui)) {
            activeConsumer = resolveConsumer((GuiContainer) event.gui);
            setConsumerQuery(getInitialQuery());
        }
    }

    @SubscribeEvent
    public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiContainer) || !isSupportedGui(event.gui)) {
            deactivateSearchField();
            return;
        }
        if (!isSearchEnabled()) {
            deactivateSearchField();
            return;
        }

        ensureField(event.gui);
        if (field == null) return;
        updateConsumer((GuiContainer) event.gui);
        updateFieldGeometry((GuiContainer) event.gui);
        drawField();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer) || !isSupportedGui(event.gui)) {
            deactivateSearchField();
            return;
        }
        if (!isSearchEnabled()) {
            deactivateSearchField();
            return;
        }
        if (field == null || !field.isFocused()) return;

        // Forge posts KeyboardInputEvent.Pre for both key-down and key-up transitions.
        // GuiTextField expects the same keyTyped semantics as vanilla GuiScreen: process only
        // key-down (including LWJGL repeat) events. Processing key-up as well makes Backspace
        // and Delete run twice and can make ordinary editing feel inconsistent.
        if (!Keyboard.getEventKeyState()) return;

        int key = Keyboard.getEventKey();
        Minecraft mc = Minecraft.getMinecraft();
        // The search field must remain editable even if another mod temporarily leaves the
        // inventory binding unavailable while options are being initialized or reloaded.
        int inventoryKey = mc != null && mc.gameSettings != null && mc.gameSettings.keyBindInventory != null
                ? mc.gameSettings.keyBindInventory.getKeyCode() : 0;

        if (key == Keyboard.KEY_ESCAPE) {
            // First Escape releases text focus. A second Escape retains vanilla GUI behavior.
            field.setFocused(false);
            updateQueryState();
            event.setCanceled(true);
            return;
        }
        boolean isInventoryKey = inventoryKey != 0 && key == inventoryKey;
        char typedCharacter = Keyboard.getEventCharacter();
        boolean printableInventoryKey = isInventoryKey
                && typedCharacter >= 32 && typedCharacter != 127
                && !Character.isISOControl(typedCharacter);
        if (isInventoryKey && !printableInventoryKey) {
            // A non-text inventory key may still close the GUI. Printable keys (normally E)
            // belong to the focused search field and must never trigger GuiContainer.keyTyped.
            field.setFocused(false);
            updateQueryState();
            return;
        }

        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            calculateIfNeeded();
            if (cachedCalculationExpression && cachedCalculation != null) {
                String formatted = CalculatorEngine.format(cachedCalculation);
                SearchBarConfig config = getConfig();
                if (config != null) {
                    config.lastCalculationInput = activeQuery;
                    config.lastCalculationResult = formatted;
                    if (config.calcEnterCopyResult) {
                        try {
                            com.nef.notenoughfakepixel.config.gui.utils.Utils.copyToClipboard(formatted);
                        } catch (RuntimeException ignored) {
                            // Clipboard integration is optional; never interrupt normal text input.
                        }
                    }
                    if (config.calcEnterClearText) {
                        field.setText("");
                        updateQueryState();
                    }
                }
                event.setCanceled(true);
                return;
            }
        }

        String previousText = field.getText();
        field.textboxKeyTyped(Keyboard.getEventCharacter(), key);
        if (!previousText.equals(field.getText())) {
            updateQueryState();
        }
        // The focused search field owns keyboard input so it cannot leak into container shortcuts.
        // We still consume non-editing key-downs (Ctrl/Shift/navigation) to protect container
        // shortcuts, but avoid invalidating the item-search cache when the query did not change.
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiContainer) || !isSupportedGui(event.gui)) {
            deactivateSearchField();
            return;
        }
        if (!isSearchEnabled()) {
            deactivateSearchField();
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (field == null || mc == null || mc.displayWidth <= 0 || mc.displayHeight <= 0) return;

        updateFieldGeometry((GuiContainer) event.gui);
        int button = Mouse.getEventButton();
        if (button < 0 || !Mouse.getEventButtonState()) return;

        int mouseX = getScaledMouseX();
        int mouseY = getScaledMouseY();
        if (isInsideField(mouseX, mouseY)) {
            if (button == 0 && !activeQuery.isEmpty()
                    && mouseX >= outerX + fieldOuterWidth - CLEAR_BUTTON_WIDTH) {
                field.setText("");
                field.setFocused(true);
                updateQueryState();
            } else {
                if (button == 0) {
                    // GuiTextField has background drawing disabled, so its text origin is inset
                    // and vertically centered. Supply a point inside that internal hit region,
                    // while preserving the real mouse X for accurate cursor placement.
                    int clickX = clamp(mouseX, field.xPosition, field.xPosition + field.width);
                    field.mouseClicked(clickX, field.yPosition + 1, button);
                    // The visible bar owns the click even on its inset/border pixels. Keep focus
                    // deterministic instead of relying solely on GuiTextField's internal hit-test.
                    field.setFocused(true);
                }
            }
            // The overlay owns every click inside its visible rectangle; no underlying slot/menu
            // action is allowed to receive a click that was visibly made on the search bar.
            event.setCanceled(true);
        } else if (field.isFocused()) {
            field.setFocused(false);
            updateQueryState();
        }
    }

    public static boolean matches(ItemStack stack) {
        return matches(stack, getSearchText());
    }

    /** Only the player inventory and explicitly recognized storage views receive search highlighting. */
    public static boolean isSupportedGui(GuiScreen screen) {
        return screen instanceof GuiContainer && getContextKind((GuiContainer) screen) != SearchContainerPolicy.Kind.NONE;
    }

    public static boolean matches(ItemStack stack, String query) {
        return SearchMatcher.matches(stack, query);
    }

    public static String getSearchText() {
        SearchBarConfig config = getConfig();
        return config == null || config.searchText == null ? "" : config.searchText;
    }

    public static String getItemListSearchText() {
        SearchBarConfig config = getConfig();
        return config == null || config.itemListSearchText == null ? "" : config.itemListSearchText;
    }

    public static String getStorageSearchText() {
        SearchBarConfig config = getConfig();
        return config == null || config.storageSearchText == null ? "" : config.storageSearchText;
    }

    public static String getLastCalculationInput() {
        SearchBarConfig config = getConfig();
        return config == null || config.lastCalculationInput == null ? "" : config.lastCalculationInput;
    }

    public static String getLastCalculationResult() {
        SearchBarConfig config = getConfig();
        return config == null || config.lastCalculationResult == null ? "" : config.lastCalculationResult;
    }

    public static void renderSlotHighlight(GuiContainer gui, Slot slot, int guiLeft, int guiTop) {
        SearchBarConfig config = getConfig();
        if (config == null || !config.enabled || !config.showItemHighlight) return;
        if (gui == null || slot == null || !isSupportedGui(gui)) return;
        String query = getSearchTextForGui(gui);
        if (query == null || query.trim().isEmpty()) return;
        try {
            ItemStack stack = slot.getStack();
            if (stack == null || !matches(stack, query)) return;
            Color border = ColorUtils.getColor(config.border);
            if (border == null) return;
            int x = slot.xDisplayPosition;
            int y = slot.yDisplayPosition;
            int rgb = border.getRGB() & 0x00FFFFFF;
            int fill = (42 << 24) | rgb;
            int edge = (210 << 24) | rgb;
            // A light tint plus a crisp one-pixel outline makes matching slots easy to find
            // without covering the item icon, stack count, durability bar, or tooltip target.
            Gui.drawRect(x, y, x + 16, y + 16, fill);
            Gui.drawRect(x, y, x + 16, y + 1, edge);
            Gui.drawRect(x, y + 15, x + 16, y + 16, edge);
            Gui.drawRect(x, y + 1, x + 1, y + 15, edge);
            Gui.drawRect(x + 15, y + 1, x + 16, y + 15, edge);
        } catch (RuntimeException ignored) {
            // Highlighting is cosmetic; malformed item metadata must not break container rendering.
        }
    }

    private void ensureField(GuiScreen screen) {
        if (field != null && activeScreen == screen) return;
        activeScreen = screen;
        SearchBarConfig config = getConfig();
        Minecraft mc = Minecraft.getMinecraft();
        if (config == null || mc == null || mc.fontRendererObj == null) return;

        activeConsumer = resolveConsumer((GuiContainer) screen);
        field = new GuiTextField(991, mc.fontRendererObj, 0, 0, 100, DEFAULT_HEIGHT);
        field.setMaxStringLength(MAX_TEXT_LENGTH);
        field.setEnableBackgroundDrawing(false);
        field.setFocused(false);

        String initial = getInitialQuery();
        if (initial == null) initial = "";
        field.setText(initial);
        activeQuery = initial;
        invalidateCalculation();
        updateQueryState();
    }

    private void updateFieldGeometry(GuiContainer gui) {
        SearchBarConfig config = getConfig();
        if (config == null || field == null || gui == null) return;

        int screenWidth = Math.max(1, gui.width);
        int screenHeight = Math.max(1, gui.height);
        int panelLeft = 0;
        int panelTop = 0;
        int panelWidth = screenWidth;

        try {
            AccessorGuiContainer accessor = (AccessorGuiContainer) gui;
            panelLeft = accessor.getGuiLeft();
            panelTop = accessor.getGuiTop();
            panelWidth = accessor.getXSize();
            // A custom GuiContainer may expose zero/uninitialized dimensions during early init.
            if (panelWidth <= 0 || panelWidth > screenWidth * 2) panelWidth = screenWidth;
        } catch (RuntimeException ignored) {
            // Safe fallback for modded screens where the accessor interface is not available.
            panelLeft = 0;
            panelTop = 0;
            panelWidth = screenWidth;
        }

        int screenWidthWithMargins = Math.max(1, screenWidth - 4);
        int minWidth = Math.min(60, screenWidthWithMargins);
        int maxWidth = Math.min(240, screenWidthWithMargins);
        if (panelWidth > 0) {
            int panelMaxWidth = Math.max(minWidth, panelWidth - 4);
            maxWidth = Math.min(maxWidth, panelMaxWidth);
        }
        maxWidth = Math.max(minWidth, maxWidth);

        int width = sanitizeDimension(config.width, DEFAULT_WIDTH, minWidth, maxWidth);
        int maxHeight = Math.max(12, Math.min(30, screenHeight - 4));
        int height = sanitizeDimension(config.height, DEFAULT_HEIGHT, 12, maxHeight);

        int configuredX = sanitizeCoordinate(config.x);
        int configuredY = sanitizeCoordinate(config.y);
        int desiredX = config.centered
                ? panelLeft + (panelWidth - width) / 2 + configuredX
                : panelLeft + configuredX;
        // The field follows the live container panel, not a fixed screen coordinate. This keeps
        // it aligned when switching between the player inventory, vault pages and chest sizes.
        // Lift the bar above the container header. Keep the existing positive-Y setting
        // direction for compatibility with saved configs; the default now leaves a 12px gap.
        int desiredY = panelTop - height - 20 + configuredY;

        int xMargin = screenWidth >= width + 4 ? 2 : 0;
        int yMargin = screenHeight >= height + 4 ? 2 : 0;
        outerX = clamp(desiredX, xMargin, Math.max(xMargin, screenWidth - width - xMargin));
        outerY = clamp(desiredY, yMargin, Math.max(yMargin, screenHeight - height - yMargin));
        fieldOuterWidth = width;
        fieldOuterHeight = height;

        // With GuiTextField background drawing disabled, xPosition/yPosition are the exact
        // glyph and caret origin. Use one shared geometry for placeholder, text, caret and clicks.
        field.xPosition = outerX + TEXT_INSET_X;
        field.yPosition = outerY + Math.max(0, (height - 8) / 2);
        field.width = Math.max(12, width - CLEAR_BUTTON_WIDTH - (TEXT_INSET_X * 2));
        field.height = height;
    }

    private void drawField() {
        SearchBarConfig config = getConfig();
        Minecraft mc = Minecraft.getMinecraft();
        if (config == null || field == null || mc == null || mc.fontRendererObj == null) return;

        try {
            Color bg = safeColor(config.background, new Color(0, 0, 0, 160));
            Color border = safeColor(config.border, new Color(255, 255, 255, 180));
            Color text = safeColor(config.textColor, Color.WHITE);

            int x = outerX;
            int y = outerY;
            int width = fieldOuterWidth;
            int height = fieldOuterHeight;
            if (width <= 0 || height <= 0) return;

            Gui.drawRect(x, y, x + width, y + height, bg.getRGB());
            Gui.drawRect(x, y, x + width, y + 1, border.getRGB());
            Gui.drawRect(x, y + height - 1, x + width, y + height, border.getRGB());
            Gui.drawRect(x, y, x + 1, y + height, border.getRGB());
            Gui.drawRect(x + width - 1, y, x + width, y + height, border.getRGB());
            field.setTextColor(text.getRGB());

            if (field.getText().isEmpty() && !field.isFocused()) {
                String placeholder = config.placeholder == null ? "Search..." : config.placeholder;
                placeholder = mc.fontRendererObj.trimStringToWidth(placeholder, Math.max(0, field.width));
                mc.fontRendererObj.drawStringWithShadow(placeholder, field.xPosition, field.yPosition, text.getRGB());
            } else {
                field.drawTextBox();
            }

            if (!field.getText().isEmpty() && width >= CLEAR_BUTTON_WIDTH + 30) {
                int dividerX = x + width - CLEAR_BUTTON_WIDTH;
                Gui.drawRect(dividerX, y + 3, dividerX + 1, y + height - 3, border.getRGB());
                String clearGlyph = "x";
                int glyphWidth = mc.fontRendererObj.getStringWidth(clearGlyph);
                mc.fontRendererObj.drawStringWithShadow(clearGlyph,
                        dividerX + (CLEAR_BUTTON_WIDTH - glyphWidth) / 2,
                        y + Math.max(2, (height - 8) / 2), text.getRGB());
            }

            calculateIfNeeded();
            if (cachedCalculationExpression && cachedCalculation != null) {
                String formatted = CalculatorEngine.format(cachedCalculation);
                mc.fontRendererObj.drawStringWithShadow("= " + formatted, x + width + 5,
                        y + Math.max(2, (height - 8) / 2), 0xFFFFFFFF);
            }
        } catch (RuntimeException ignored) {
            // A malformed saved color or custom font/render state must not crash an inventory GUI.
        }
    }

    private static Color safeColor(String value, Color fallback) {
        try {
            Color parsed = ColorUtils.getColor(value);
            return parsed == null ? fallback : parsed;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private void calculateIfNeeded() {
        if (activeQuery.equals(cachedCalculationInput)) return;
        cachedCalculationInput = activeQuery;
        cachedCalculation = CalculatorEngine.evaluate(activeQuery);
        cachedCalculationExpression = cachedCalculation != null && CalculatorEngine.looksLikeExpression(activeQuery);
        SearchBarConfig config = getConfig();
        if (cachedCalculationExpression && config != null) {
            config.lastCalculationInput = activeQuery;
            config.lastCalculationResult = CalculatorEngine.format(cachedCalculation);
        }
    }

    private void invalidateCalculation() {
        cachedCalculationInput = null;
        cachedCalculation = null;
        cachedCalculationExpression = false;
    }

    private void deactivateSearchField() {
        if (field != null && field.isFocused()) {
            field.setFocused(false);
            updateQueryState();
        }
    }

    private void setConsumerQuery(String query) {
        activeQuery = query == null ? "" : query;
        SearchBarConfig config = getConfig();
        if (config != null) {
            if ("storage".equals(activeConsumer)) config.storageSearchText = activeQuery;
            else if ("itemList".equals(activeConsumer)) config.itemListSearchText = activeQuery;
            else config.searchText = activeQuery;
        }
        updateCachedQueryValue();
        SearchMatcher.invalidateQuery();
        invalidateCalculation();
    }

    private void updateQueryState() {
        activeQuery = field == null ? activeQuery : field.getText();
        invalidateCalculation();
        SearchMatcher.invalidateQuery();
        SearchBarConfig config = getConfig();
        if (config == null) return;
        if ("storage".equals(activeConsumer)) config.storageSearchText = activeQuery;
        else if ("itemList".equals(activeConsumer)) config.itemListSearchText = activeQuery;
        else config.searchText = activeQuery;
        updateCachedQueryValue();
        calculateIfNeeded();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        SearchMatcher.beginClientTick();
        if (field != null && field.isFocused()) field.updateCursorCounter();
    }

    private void updateConsumer(GuiContainer gui) {
        if (gui == null || field == null) return;
        String next = resolveConsumer(gui);
        if (!next.equals(activeConsumer)) {
            saveCurrentQuery();
            activeConsumer = next;
            String initial = getInitialQuery();
            if (initial == null) initial = "";
            field.setText(initial);
            activeQuery = initial;
            invalidateCalculation();
            updateContextCache(gui);
            updateQueryState();
        }
    }

    private String getInitialQuery() {
        SearchBarConfig config = getConfig();
        if (config == null) return "";
        if ("storage".equals(activeConsumer)) {
            return config.persistStorageSearch ? getStorageSearchText() : "";
        }
        if ("itemList".equals(activeConsumer)) {
            return config.persistItemListSearch ? getItemListSearchText() : "";
        }
        return config.persistSearchText ? getSearchText() : "";
    }

    private void saveCurrentQuery() {
        if (field == null) return;
        updateQueryState();
        SearchBarConfig config = getConfig();
        if (config == null) return;
        if (("main".equals(activeConsumer) && config.persistSearchText)
                || ("itemList".equals(activeConsumer) && config.persistItemListSearch)
                || ("storage".equals(activeConsumer) && config.persistStorageSearch)) {
            try {
                Config.saveConfig();
            } catch (RuntimeException ignored) {
                // Persisting optional UI state must never interrupt opening another container.
            }
        }
    }

    private boolean isInsideField(int mouseX, int mouseY) {
        return field != null && mouseX >= outerX && mouseX < outerX + fieldOuterWidth
                && mouseY >= outerY && mouseY < outerY + fieldOuterHeight;
    }

    private static int getScaledMouseX() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.displayWidth <= 0) return 0;
        ScaledResolution sr = new ScaledResolution(mc);
        if (sr.getScaledWidth() <= 0) return 0;
        return (int) ((long) Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth);
    }

    private static int getScaledMouseY() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.displayHeight <= 0) return 0;
        ScaledResolution sr = new ScaledResolution(mc);
        if (sr.getScaledHeight() <= 0) return 0;
        return sr.getScaledHeight() - (int) ((long) Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight) - 1;
    }

    private static int sanitizeDimension(float value, int fallback, int min, int max) {
        int safeMax = Math.max(min, max);
        if (Float.isNaN(value) || Float.isInfinite(value)) return clamp(fallback, min, safeMax);
        return clamp(Math.round(value), min, safeMax);
    }

    private static int sanitizeCoordinate(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 0;
        return clamp(Math.round(value), -4096, 4096);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static SearchContainerPolicy.Kind getContextKind(GuiContainer gui) {
        if (gui == null) return SearchContainerPolicy.Kind.NONE;
        if (cachedContextGui != gui || cachedContextContainer != gui.inventorySlots) {
            updateContextCache(gui);
        }
        return cachedContextKind;
    }

    private static String getSearchTextForGui(GuiContainer gui) {
        SearchContainerPolicy.Kind kind = getContextKind(gui);
        if (kind == SearchContainerPolicy.Kind.NONE) return "";
        updateCachedQueryValue();
        return cachedQueryValue;
    }

    private static void updateContextCache(GuiContainer gui) {
        cachedContextGui = gui;
        cachedContextContainer = gui == null ? null : gui.inventorySlots;
        cachedContextKind = gui == null ? SearchContainerPolicy.Kind.NONE : SearchContainerPolicy.resolve(gui);
        if (cachedContextKind == SearchContainerPolicy.Kind.STORAGE) cachedQueryConsumer = "storage";
        else if (cachedContextKind == SearchContainerPolicy.Kind.ITEM_LIST) cachedQueryConsumer = "itemList";
        else cachedQueryConsumer = "main";
        cachedQueryValue = queryForConsumer(cachedQueryConsumer);
    }

    private static String resolveConsumer(GuiContainer gui) {
        SearchContainerPolicy.Kind kind = getContextKind(gui);
        if (kind == SearchContainerPolicy.Kind.STORAGE) return "storage";
        if (kind == SearchContainerPolicy.Kind.ITEM_LIST) return "itemList";
        return "main";
    }

    private static String queryForConsumer(String consumer) {
        if ("storage".equals(consumer)) return getStorageSearchText();
        if ("itemList".equals(consumer)) return getItemListSearchText();
        return getSearchText();
    }

    private static void updateCachedQueryValue() {
        String current = queryForConsumer(cachedQueryConsumer);
        if (!current.equals(cachedQueryValue)) cachedQueryValue = current;
    }

    private static void invalidateGuiQueryCache() {
        cachedContextGui = null;
        cachedContextContainer = null;
        cachedContextKind = SearchContainerPolicy.Kind.NONE;
        cachedQueryConsumer = "main";
        cachedQueryValue = "";
    }

    private static SearchBarConfig getConfig() {
        return Config.feature == null ? null : Config.feature.searchBar;
    }

    private static boolean isSearchEnabled() {
        SearchBarConfig config = getConfig();
        return config != null && config.enabled;
    }
}
