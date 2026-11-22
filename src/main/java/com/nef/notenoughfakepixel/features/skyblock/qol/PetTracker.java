/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.PetData;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Rarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Client-side Pet menu analyzer. Scans at a throttled interval and never mutates inventory items. */
@RegisterEvents
public class PetTracker {
    private static final Field SLOT_FIELD = findSlotField();
    private static final Method TOOLTIP_METHOD = findTooltipMethod();
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile(
            "(?i)\\bpage\\s*[:#]?\\s*(\\d+)(?:\\s*(?:/|of)\\s*(\\d+))?"
    );
    private static final Pattern PAGE_FRACTION_PATTERN = Pattern.compile("(?i)\\b(\\d+)\\s*/\\s*(\\d+)\\b");
    private final Map<String, Integer> ownedPets = new LinkedHashMap<>();
    /** UUID + SkyBlock profile key for the currently loaded persistent collection. */
    private String loadedProfileKey;
    /** Per-page snapshots are retained for the whole Pet menu session. */
    private final Map<String, Map<String, Integer>> petsByPage = new LinkedHashMap<>();
    private String activePageKey;
    private final List<String> missingPets = new ArrayList<>();
    private final List<String> upgradeablePets = new ArrayList<>();
    private long lastScanMs;
    private IInventory lastInventory;
    private int scrollMissing;
    private int scrollUpgradeable;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // Hypixel can replace the chest screen when moving between Pet pages.
        // Keep the accumulated session when the next screen is still the Pets menu;
        // clear only after leaving the menu (or closing the GUI).
        if (isPetsMenu(event.gui)) {
            lastInventory = null;
            lastScanMs = 0L;
            return;
        }
        saveCurrentProfile();
        clearMenuSession();
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!Config.feature.pets.petTracker || !isPetsMenu(event.gui)) return;
        GuiChest chest = (GuiChest) event.gui;
        IInventory inventory = ((ContainerChest) chest.inventorySlots).getLowerChestInventory();

        long now = System.currentTimeMillis();
        if (inventory != lastInventory || now - lastScanMs >= 250L) {
            scanPets(inventory);
            lastInventory = inventory;
            lastScanMs = now;
        }
        drawSidebar(chest);
        renderHoveredTooltip(chest, event.mouseX, event.mouseY);
    }

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!Config.feature.pets.petTracker || !isPetsMenu(event.gui)) return;
        GuiChest chest = (GuiChest) event.gui;
        IInventory inventory = ((ContainerChest) chest.inventorySlots).getLowerChestInventory();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        Minecraft mc = Minecraft.getMinecraft();
        int mouseX = Mouse.getEventX() * chest.width / Math.max(1, mc.displayWidth);
        int guiLeft = (chest.width - 176) / 2;
        int delta = wheel > 0 ? -1 : 1;
        if (chest.width >= 560 && mouseX < guiLeft) {
            scrollUpgradeable = clampScroll(scrollUpgradeable + delta, upgradeablePets.size());
        } else if (chest.width >= 560) {
            scrollMissing = clampScroll(scrollMissing + delta, missingPets.size());
        } else if (!upgradeablePets.isEmpty()) {
            // The compact layout displays one list at a time. Scroll the list that is
            // actually visible instead of always changing the hidden Missing list.
            scrollUpgradeable = clampScroll(scrollUpgradeable + delta, upgradeablePets.size());
        } else {
            scrollMissing = clampScroll(scrollMissing + delta, missingPets.size());
        }
    }

    private int clampScroll(int value, int size) {
        return Math.max(0, Math.min(Math.max(0, size - 12), value));
    }

    private void scanPets(IInventory inventory) {
        ensureProfileLoaded();
        PetTrackerStore.flushIfDirty();
        Map<String, Rarity> catalogue = PetData.getAllPets();
        if (catalogue.isEmpty()) return;
        List<String> names = new ArrayList<>(catalogue.keySet());
        names.sort(Comparator.comparingInt(String::length).reversed());

        // Build a fresh observation for the visible page, then merge it into both
        // that page's snapshot and the session-wide collection. Never replace the
        // aggregate with only the currently visible page.
        Map<String, Integer> visiblePagePets = new LinkedHashMap<>();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null || !stack.hasDisplayName()) continue;
            String raw = stack.getDisplayName();
            String clean = StringUtils.stripControlCodes(raw).trim();
            if (clean.isEmpty() || clean.equalsIgnoreCase("Pets") || clean.contains("Page")
                    || clean.equalsIgnoreCase("Go Back") || clean.equalsIgnoreCase("Close")) continue;
            clean = clean.replaceAll("\\[Lvl \\d+\\]\\s*", "").trim();
            int score = rarityScore(raw);
            String normalized = clean.toLowerCase(java.util.Locale.ROOT);
            for (String pet : names) {
                if (normalized.contains(pet.toLowerCase(java.util.Locale.ROOT))) {
                    Integer previous = visiblePagePets.get(pet);
                    if (previous == null || score > previous) visiblePagePets.put(pet, score);
                    break;
                }
            }
        }

        if (!visiblePagePets.isEmpty()) {
            String pageKey = resolvePageKey(inventory, visiblePagePets);
            if (!pageKey.equals(activePageKey)) {
                activePageKey = pageKey;
                scrollMissing = 0;
                scrollUpgradeable = 0;
            }
            Map<String, Integer> pageSnapshot = petsByPage.get(pageKey);
            if (pageSnapshot == null) {
                pageSnapshot = new LinkedHashMap<>();
                petsByPage.put(pageKey, pageSnapshot);
            }
            boolean pageChanged = mergePets(pageSnapshot, visiblePagePets);
            boolean collectionChanged = mergePets(ownedPets, visiblePagePets);
            if (pageChanged || collectionChanged) {
                // Persist the page snapshot and global collection as soon as new data appears.
                saveCurrentProfile();
            }
        }

        rebuildSummary(catalogue);
    }

    private boolean mergePets(Map<String, Integer> destination, Map<String, Integer> observed) {
        boolean changed = false;
        for (Entry<String, Integer> entry : observed.entrySet()) {
            Integer previous = destination.get(entry.getKey());
            if (previous == null || entry.getValue() > previous) {
                destination.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private void rebuildSummary(Map<String, Rarity> catalogue) {
        missingPets.clear();
        upgradeablePets.clear();
        for (Entry<String, Rarity> entry : catalogue.entrySet()) {
            int maximum = rarityScore(entry.getValue());
            Integer current = ownedPets.get(entry.getKey());
            if (current == null) {
                // Show the gain this pet can contribute when collected at its catalogue rarity.
                missingPets.add("§c✘ " + entry.getKey() + " §8→ §e+" + maximum + " pts");
            } else if (current < maximum) {
                // Show only this pet's incremental gain; never calculate a combined Pet Score.
                upgradeablePets.add(rarityColor(current) + entry.getKey()
                        + " §8→ §e+" + (maximum - current) + " pts");
            }
        }
        // Newly discovered pets shrink the missing/upgradeable lists; never let a
        // previous scroll offset point beyond the new list and leave the panel blank.
        scrollMissing = clampScroll(scrollMissing, missingPets.size());
        scrollUpgradeable = clampScroll(scrollUpgradeable, upgradeablePets.size());
    }

    /**
     * Prefer a real page number exposed by the chest title or a navigation item's
     * name/lore. If the server does not expose one, use the sorted pet names visible
     * on the page as a stable fallback identity.
     */
    private String resolvePageKey(IInventory inventory, Map<String, Integer> visiblePets) {
        if (inventory.getDisplayName() != null) {
            String title = StringUtils.stripControlCodes(inventory.getDisplayName().getUnformattedText());
            String pageNumber = findPageNumber(title);
            if (pageNumber != null) return "page:" + pageNumber;
        }

        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack == null || !stack.hasDisplayName()) continue;
            String displayName = StringUtils.stripControlCodes(stack.getDisplayName());
            String lowerDisplayName = displayName.toLowerCase(java.util.Locale.ROOT);
            boolean navigationButton = lowerDisplayName.contains("next page")
                    || lowerDisplayName.contains("previous page") || lowerDisplayName.contains("last page");
            String pageNumber = navigationButton ? null : findPageNumber(displayName);
            if (pageNumber != null) return "page:" + pageNumber;
            // Some menu versions put the CURRENT page in the navigation item's lore.
            // Do not skip the whole item just because its display name says Next/Previous Page.
            if (mc.thePlayer != null) {
                try {
                    for (String line : stack.getTooltip(mc.thePlayer, false)) {
                        String cleanLine = StringUtils.stripControlCodes(line);
                        String lowerLine = cleanLine.toLowerCase(java.util.Locale.ROOT);
                        if (lowerLine.contains("next page") || lowerLine.contains("previous page")
                                || lowerLine.contains("last page")) continue;
                        pageNumber = findPageNumber(cleanLine);
                        if (pageNumber != null) return "page:" + pageNumber;
                    }
                } catch (RuntimeException ignored) {
                    // Tooltip format differs between menu versions; use the fingerprint fallback.
                }
            }
        }

        List<String> sortedNames = new ArrayList<>(visiblePets.keySet());
        Collections.sort(sortedNames, String.CASE_INSENSITIVE_ORDER);
        StringBuilder fingerprint = new StringBuilder("contents:");
        for (String name : sortedNames) fingerprint.append(name.length()).append(':').append(name).append(';');
        return fingerprint.toString();
    }

    private String findPageNumber(String value) {
        if (value == null || value.isEmpty()) return null;
        Matcher matcher = PAGE_NUMBER_PATTERN.matcher(value);
        if (matcher.find()) return matcher.group(1) + (matcher.group(2) == null ? "" : "/" + matcher.group(2));
        matcher = PAGE_FRACTION_PATTERN.matcher(value);
        if (matcher.find()) return matcher.group(1) + "/" + matcher.group(2);
        return null;
    }

    private boolean isPetsMenu(GuiScreen screen) {
        if (!(screen instanceof GuiChest)) return false;
        GuiChest chest = (GuiChest) screen;
        if (!(chest.inventorySlots instanceof ContainerChest)) return false;
        IInventory inventory = ((ContainerChest) chest.inventorySlots).getLowerChestInventory();
        return inventory != null && inventory.getDisplayName() != null
                && StringUtils.stripControlCodes(inventory.getDisplayName().getUnformattedText())
                .toLowerCase(java.util.Locale.ROOT).contains("pets");
    }

    private int rarityScore(String raw) {
        int close = raw.indexOf(']');
        String part = close >= 0 && close + 1 < raw.length() ? raw.substring(close + 1) : raw;
        for (int i = 0; i + 1 < part.length(); i++) {
            if (part.charAt(i) != '§') continue;
            char c = Character.toLowerCase(part.charAt(i + 1));
            if (c == 'd' || c == 'b') return 6;
            if (c == '6') return 5;
            if (c == '5') return 4;
            if (c == '9') return 3;
            if (c == 'a') return 2;
        }
        return 1;
    }

    private int rarityScore(Rarity rarity) {
        switch (rarity) {
            case MYTHIC: case DIVINE: return 6;
            case LEGENDARY: return 5;
            case EPIC: return 4;
            case RARE: return 3;
            case UNCOMMON: return 2;
            default: return 1;
        }
    }

    private String rarityColor(int score) {
        switch (score) {
            case 6: return "§d";
            case 5: return "§6";
            case 4: return "§5";
            case 3: return "§9";
            case 2: return "§a";
            default: return "§f";
        }
    }

    private void drawSidebar(GuiChest chest) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.fontRendererObj == null) return;
        int guiLeft = (chest.width - 176) / 2;
        int y = Math.max(8, (chest.height - 166) / 2);
        if (chest.width >= 560) {
            int leftX = Math.max(2, guiLeft - 142);
            int rightX = Math.min(chest.width - 142, guiLeft + 178);
            mc.fontRendererObj.drawStringWithShadow("§aCollected Pets: §f" + ownedPets.size(),
                    leftX, Math.max(0, y - 10), 0xFFFFFF);
            drawPanel(leftX, y, 138, "§bUpgrade Gains", upgradeablePets, scrollUpgradeable);
            drawPanel(rightX, y, 138, "§cMissing: §f" + missingPets.size(), missingPets, scrollMissing);
        } else {
            int x = Math.max(2, (chest.width - 260) / 2);
            List<String> summary = new ArrayList<>();
            summary.add("§aCollected: §f" + ownedPets.size() + "  §bUpgrade: §f" + upgradeablePets.size());
            summary.add("§cMissing: §f" + missingPets.size() + "  §7Gain shown per pet");
            boolean showingUpgradeable = !upgradeablePets.isEmpty();
            List<String> source = showingUpgradeable ? upgradeablePets : missingPets;
            int sourceScroll = showingUpgradeable ? scrollUpgradeable : scrollMissing;
            summary.addAll(source.subList(Math.min(sourceScroll, source.size()),
                    Math.min(source.size(), sourceScroll + 10)));
            drawPanel(x, y, 256, "§bPet Score Gain / Pet", summary, 0);
        }
    }

    private void drawPanel(int x, int y, int width, String heading, List<String> rows, int startIndex) {
        Minecraft mc = Minecraft.getMinecraft();
        int visibleCount = Math.min(12, Math.max(0, rows.size() - startIndex));
        int height = Math.min(174, 22 + visibleCount * 11);
        Gui.drawRect(x, y, x + width, y + height, 0xD0101018);
        Gui.drawRect(x, y, x + width, y + 1, 0xFF4188C8);
        mc.fontRendererObj.drawStringWithShadow(heading, x + 5, y + 5, 0xFFFFFF);
        int top = y + 19;
        for (int i = 0; i < visibleCount; i++) {
            String text = rows.get(startIndex + i);
            int maxWidth = width - 10;
            // Keep the numeric gain visible even when a long pet name must be shortened.
            int gainMarker = text.lastIndexOf(" §8→ §e+");
            if (gainMarker > 0) {
                String suffix = text.substring(gainMarker);
                String namePart = text.substring(0, gainMarker);
                while (mc.fontRendererObj.getStringWidth(namePart + "…" + suffix) > maxWidth
                        && namePart.length() > 5) {
                    namePart = namePart.substring(0, namePart.length() - 1);
                }
                text = namePart + "…" + suffix;
            } else {
                while (mc.fontRendererObj.getStringWidth(text) > maxWidth && text.length() > 5)
                    text = text.substring(0, text.length() - 2) + "…";
            }
            mc.fontRendererObj.drawStringWithShadow(text, x + 5, top + i * 11, 0xFFFFFF);
        }
        if (rows.isEmpty()) mc.fontRendererObj.drawStringWithShadow("§aAll caught up", x + 5, top, 0xFFFFFF);
    }

    /** Restore the vanilla item tooltip after the Pet menu's custom sidebar has rendered. */
    private void renderHoveredTooltip(GuiChest chest, int mouseX, int mouseY) {
        if (SLOT_FIELD == null || TOOLTIP_METHOD == null) return;
        try {
            Object value = SLOT_FIELD.get(chest);
            if (!(value instanceof net.minecraft.inventory.Slot)) return;
            net.minecraft.inventory.Slot slot = (net.minecraft.inventory.Slot) value;
            if (!slot.getHasStack()) return;
            TOOLTIP_METHOD.invoke(chest, slot.getStack(), mouseX, mouseY);
        } catch (Exception ignored) { /* A mapping difference must not break the inventory GUI. */ }
    }

    private static Field findSlotField() {
        try {
            Field field;
            try { field = GuiContainer.class.getDeclaredField("theSlot"); }
            catch (NoSuchFieldException ignored) { field = GuiContainer.class.getDeclaredField("field_147006_u"); }
            field.setAccessible(true);
            return field;
        } catch (Exception ignored) { return null; }
    }

    private static Method findTooltipMethod() {
        try {
            Method method;
            try { method = GuiScreen.class.getDeclaredMethod("renderToolTip", ItemStack.class, int.class, int.class); }
            catch (NoSuchMethodException ignored) { method = GuiScreen.class.getDeclaredMethod("func_146284_a", ItemStack.class, int.class, int.class); }
            method.setAccessible(true);
            return method;
        } catch (Exception ignored) { return null; }
    }

    private void ensureProfileLoaded() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        String playerId = mc.thePlayer.getUniqueID().toString();
        String profile = SkyblockData.getCurrentProfile();
        boolean profileKnown = profile != null && !profile.trim().isEmpty();
        if (!profileKnown && loadedProfileKey != null && loadedProfileKey.startsWith(playerId + "|")) {
            // Scoreboard/profile parsing may briefly clear this value during a GUI transition.
            // Keep the current profile loaded rather than switching to an empty temporary bucket.
            return;
        }
        String normalizedProfile = profileKnown
                ? profile.trim().toLowerCase(Locale.ROOT)
                : "unknown";
        String nextKey = playerId + "|" + normalizedProfile;
        if (nextKey.equals(loadedProfileKey)) return;

        String previousKey = loadedProfileKey;
        Map<String, Integer> previouslyObserved = new LinkedHashMap<>(ownedPets);
        Map<String, Map<String, Integer>> previouslyObservedPages = copyPageSnapshots(petsByPage);
        saveCurrentProfile();

        PetTrackerStore.ProfileData saved = PetTrackerStore.loadProfileData(nextKey);
        ownedPets.clear();
        ownedPets.putAll(saved.ownedPets);
        petsByPage.clear();
        for (Entry<String, Map<String, Integer>> page : saved.petsByPage.entrySet()) {
            petsByPage.put(page.getKey(), new LinkedHashMap<>(page.getValue()));
        }
        loadedProfileKey = nextKey;

        // If the profile name became available after the first menu scan, promote the
        // observations made in the temporary unknown-profile bucket into that profile.
        boolean promoteUnknown = previousKey != null
                && previousKey.equals(playerId + "|unknown") && profileKnown;
        if (promoteUnknown) {
            boolean changed = mergePets(ownedPets, previouslyObserved);
            for (Entry<String, Map<String, Integer>> page : previouslyObservedPages.entrySet()) {
                Map<String, Integer> target = petsByPage.get(page.getKey());
                if (target == null) {
                    target = new LinkedHashMap<>();
                    petsByPage.put(page.getKey(), target);
                }
                changed |= mergePets(target, page.getValue());
            }
            if (changed) saveCurrentProfile();
        }

        activePageKey = null;
        missingPets.clear();
        upgradeablePets.clear();
        lastInventory = null;
        lastScanMs = 0L;
        scrollMissing = 0;
        scrollUpgradeable = 0;
    }

    private Map<String, Map<String, Integer>> copyPageSnapshots(Map<String, Map<String, Integer>> source) {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        for (Entry<String, Map<String, Integer>> page : source.entrySet()) {
            copy.put(page.getKey(), new LinkedHashMap<>(page.getValue()));
        }
        return copy;
    }

    private void saveCurrentProfile() {
        if (loadedProfileKey == null) return;
        PetTrackerStore.saveProfileData(loadedProfileKey, ownedPets, petsByPage);
    }

    /** Clears only transient menu state; the collected-pet database is never session-cleared. */
    private void clearMenuSession() {
        petsByPage.clear();
        activePageKey = null;
        missingPets.clear();
        upgradeablePets.clear();
        lastInventory = null;
        lastScanMs = 0L;
        scrollMissing = 0;
        scrollUpgradeable = 0;
    }
}
