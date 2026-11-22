/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol.abiphone;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class AbiphonePanel extends GuiScreen {

    private static final ResourceLocation CHEST_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    
    public static final String DEFAULT_CONTACTS = "st_jerry, blacksmith, kat, anita, jacob, rusty, odger, fred, alda, pablo, trevor, talbot, brynmor, plumber_joe, jax, walter, zog, einary, queen_mismyla, ophelia, hoppity, alchemist, jotraeline_greatforge, lumber_merchant, mort, wool_weaver, sirius, maddox, fear_mongerer, elizabeth, dusk, builder, oringo, maxwell";

    private static final Pattern SEPARATOR = Pattern.compile("[,;\\n]");

    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 4;
    private static final int GRID_FIRST_COLUMN = 1;
    private static final int GRID_FIRST_ROW = 1;
    private static final int FAVORITES_SLOT = 5 * COLUMNS + 2;
    private static final int CLOSE_SLOT = 5 * COLUMNS + 4;
    private static final int PREVIOUS_PAGE_SLOT = 5 * COLUMNS;
    private static final int NEXT_PAGE_SLOT = 5 * COLUMNS + 8;
    private static final int PAGE_SIZE = GRID_COLUMNS * GRID_ROWS;

    private static final int MAX_CONTACTS = 200;
    private static final int MAX_NAME_LENGTH = 32;
    private static final int PANEL_WIDTH = 176;
    private static final int TITLE_COLOR = 4210752;

    private final List<String> allContacts;
    private List<String> shown = Collections.emptyList();
    private final ItemStack redDye = new ItemStack(Items.dye, 1, 1);
    private final ItemStack greenDye = new ItemStack(Items.dye, 1, 10);
    private final ItemStack barrier = new ItemStack(Item.getItemFromBlock(Blocks.barrier));
    private final ItemStack arrow = new ItemStack(Items.arrow);
    
    private int page;

    private int left;
    private int top;

    public AbiphonePanel(String configuredContacts) {
        List<String> parsed = parseContacts(configuredContacts);
        this.allContacts = parsed.isEmpty() ? parseContacts(DEFAULT_CONTACTS) : parsed;
        refreshShown();
    }

    private void refreshShown() {
        if (!AbiphoneFavorites.isFavoritesOnly()) {
            shown = allContacts;
        } else {
            List<String> favorites = new ArrayList<>();
            for (String name : allContacts) if (AbiphoneFavorites.isFavorite(name)) favorites.add(name);
            shown = favorites;
        }
        page = Math.min(page, lastPage());
    }

    private int lastPage() {
        return Math.max(0, (shown.size() - 1) / PAGE_SIZE);
    }

    static List<String> parseContacts(String text) {
        if (text == null || text.trim().isEmpty()) return Collections.emptyList();
        Set<String> names = new LinkedHashSet<>();
        for (String raw : SEPARATOR.split(text)) {
            StringBuilder clean = new StringBuilder(raw.length());
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c == '§') { i++; continue; } 
                if (c >= 32 && c != 127) clean.append(c);
            }
            String name = clean.toString().trim();
            if (name.isEmpty()) continue;
            if (name.length() > MAX_NAME_LENGTH) name = name.substring(0, MAX_NAME_LENGTH).trim();
            if (name.equalsIgnoreCase("elizabith")) name = "elizabeth"; 
            names.add(name);
            if (names.size() >= MAX_CONTACTS) break;
        }
        return new ArrayList<>(names);
    }

    static String displayName(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        boolean startOfWord = true;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '_' || c == ' ') {
                if (out.length() > 0 && out.charAt(out.length() - 1) != ' ') out.append(' ');
                startOfWord = true;
            } else {
                out.append(startOfWord ? Character.toUpperCase(c) : c);
                startOfWord = false;
            }
        }
        return out.toString().trim();
    }

    @Override
    public void initGui() {
        super.initGui();
        int totalHeight = ROWS * 18 + 17 + 4;
        left = (width - PANEL_WIDTH) / 2;
        top = (height - totalHeight) / 2;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        GlStateManager.color(1F, 1F, 1F, 1F);
        mc.getTextureManager().bindTexture(CHEST_TEXTURE);
        drawTexturedModalRect(left, top, 0, 0, PANEL_WIDTH, ROWS * 18 + 17);
        drawTexturedModalRect(left, top + ROWS * 18 + 17, 0, 218, PANEL_WIDTH, 4);

        fontRendererObj.drawString("Abiphone", left + 8, top + 6, TITLE_COLOR);

        if (shown.isEmpty()) {
            String[] lines = AbiphoneFavorites.isFavoritesOnly()
                    ? new String[]{"No favorites yet", "Right-click a contact to favorite it", "or switch the dye to see everyone"}
                    : new String[]{"No contacts yet", "Add names in the NEF config:", "Quality of Life > Abiphone Vault"};
            int centerX = left + PANEL_WIDTH / 2;
            int startY = top + 18 + 2 * 18 - 10;
            for (int i = 0; i < lines.length; i++) {
                fontRendererObj.drawString(lines[i], centerX - fontRendererObj.getStringWidth(lines[i]) / 2, startY + i * 12, TITLE_COLOR);
            }
        }

        int hoveredSlot = slotAt(mouseX, mouseY);
        ItemStack favoritesButton = AbiphoneFavorites.isFavoritesOnly() ? greenDye : redDye;

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.enableRescaleNormal();
        zLevel = 100F;
        itemRender.zLevel = 100F;
        try {
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLUMNS; col++) {
                    int index = page * PAGE_SIZE + row * GRID_COLUMNS + col;
                    if (index >= shown.size()) break;
                    itemRender.renderItemAndEffectIntoGUI(AbiphoneHeads.headFor(shown.get(index)),
                            slotX(GRID_FIRST_COLUMN + col), slotY(GRID_FIRST_ROW + row));
                }
            }
            itemRender.renderItemAndEffectIntoGUI(favoritesButton, slotX(FAVORITES_SLOT % COLUMNS), slotY(FAVORITES_SLOT / COLUMNS));
            itemRender.renderItemAndEffectIntoGUI(barrier, slotX(CLOSE_SLOT % COLUMNS), slotY(CLOSE_SLOT / COLUMNS));
            if (page > 0) itemRender.renderItemAndEffectIntoGUI(arrow, slotX(PREVIOUS_PAGE_SLOT % COLUMNS), slotY(PREVIOUS_PAGE_SLOT / COLUMNS));
            if (page < lastPage()) itemRender.renderItemAndEffectIntoGUI(arrow, slotX(NEXT_PAGE_SLOT % COLUMNS), slotY(NEXT_PAGE_SLOT / COLUMNS));
        } finally {
            itemRender.zLevel = 0F;
            zLevel = 0F;
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
        }

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int index = page * PAGE_SIZE + row * GRID_COLUMNS + col;
                if (index >= shown.size()) break;
                if (AbiphoneFavorites.isFavorite(shown.get(index))) {
                    fontRendererObj.drawStringWithShadow("★", slotX(GRID_FIRST_COLUMN + col) + 9, slotY(GRID_FIRST_ROW + row) - 1, 0xFFD700);
                }
            }
        }
        GlStateManager.enableDepth();

        if (hoveredSlot >= 0 && hasItemIn(hoveredSlot)) {
            int x = slotX(hoveredSlot % COLUMNS);
            int y = slotY(hoveredSlot / COLUMNS);

            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            drawRect(x, y, x + 16, y + 16, 0x80FFFFFF);
            GlStateManager.enableDepth();

            drawHoveringText(tooltipFor(hoveredSlot), mouseX, mouseY);
            GlStateManager.disableLighting();
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private List<String> tooltipFor(int slot) {
        if (slot == FAVORITES_SLOT) {
            return AbiphoneFavorites.isFavoritesOnly()
                    ? Arrays.asList("§aFavorites only: ON", "§7Click to show everyone")
                    : Arrays.asList("§cFavorites only: OFF", "§7Click to show only your favorites");
        }
        if (slot == CLOSE_SLOT) return Collections.singletonList("§cClose");
        if (slot == PREVIOUS_PAGE_SLOT) return Arrays.asList("§aPrevious Page", "§7Page " + page + "/" + (lastPage() + 1));
        if (slot == NEXT_PAGE_SLOT) return Arrays.asList("§aNext Page", "§7Page " + (page + 2) + "/" + (lastPage() + 1));
        String name = shown.get(contactIndex(slot));
        return Arrays.asList("§a" + displayName(name),
                AbiphoneFavorites.isFavorite(name) ? "§6★ Favorite" : "§8Not a favorite",
                "§7Click to call",
                "§7Right-click to " + (AbiphoneFavorites.isFavorite(name) ? "unfavorite" : "favorite"));
    }

    private int slotX(int column) {
        return left + 8 + column * 18;
    }

    private int slotY(int row) {
        return top + 18 + row * 18;
    }

    private int slotAt(int mouseX, int mouseY) {
        int relX = mouseX - (left + 8);
        int relY = mouseY - (top + 18);
        if (relX < 0 || relY < 0) return -1;
        int col = relX / 18;
        int row = relY / 18;
        if (col >= COLUMNS || row >= ROWS) return -1;
        if (relX % 18 >= 16 || relY % 18 >= 16) return -1;
        return row * COLUMNS + col;
    }

    private int contactIndex(int slot) {
        int row = slot / COLUMNS - GRID_FIRST_ROW;
        int col = slot % COLUMNS - GRID_FIRST_COLUMN;
        if (row < 0 || row >= GRID_ROWS || col < 0 || col >= GRID_COLUMNS) return -1;
        int index = page * PAGE_SIZE + row * GRID_COLUMNS + col;
        return index < shown.size() ? index : -1;
    }

    private boolean hasItemIn(int slot) {
        return slot == FAVORITES_SLOT || slot == CLOSE_SLOT
                || (slot == PREVIOUS_PAGE_SLOT && page > 0)
                || (slot == NEXT_PAGE_SLOT && page < lastPage())
                || contactIndex(slot) >= 0;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0 && mouseButton != 1) return;

        int slot = slotAt(mouseX, mouseY);
        if (slot < 0) return;

        if (slot == CLOSE_SLOT) {
            if (mouseButton == 0) mc.displayGuiScreen(null);
            return;
        }
        if (slot == FAVORITES_SLOT) {
            if (mouseButton != 0) return;
            AbiphoneFavorites.toggleFavoritesOnly();
            page = 0;
            refreshShown();
            return;
        }
        if (slot == PREVIOUS_PAGE_SLOT || slot == NEXT_PAGE_SLOT) {
            if (mouseButton == 0) changePage(slot == NEXT_PAGE_SLOT ? 1 : -1);
            return;
        }

        int index = contactIndex(slot);
        if (index < 0) return;
        String name = shown.get(index);

        if (mouseButton == 1) {
            AbiphoneFavorites.toggleFavorite(name);
            refreshShown(); 
            return;
        }

        Minecraft minecraft = mc;
        minecraft.displayGuiScreen(null);
        if (minecraft.thePlayer != null) minecraft.thePlayer.sendChatMessage("/call " + name);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        changePage(wheel > 0 ? -1 : 1);
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(lastPage(), page + delta));
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
