/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Original code and third-party components remain subject to their respective copyrights and license terms.
 */
package com.nef.notenoughfakepixel.features.inventory.search;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.IChatComponent;

import java.util.Locale;

/**
 * Restricts search highlighting to the player's inventory and positively identified item-storage views.
 * Hypixel-style menus are also GuiChest instances, so unknown chest titles stay outside the overlay.
 * This policy never changes rendering, interaction, or contents for unsupported server menus.
 */
public final class SearchContainerPolicy {
    public enum Kind {
        NONE,
        PLAYER_INVENTORY,
        STORAGE,
        ITEM_LIST,
        VANILLA_CHEST
    }

    private static final String[] INTERACTIVE_MENU_MARKERS = {
            "auction", "bazaar", "shop", "menu", "confirm", "confirmation", "select", "selection", "choose",
            "upgrade", "pet", "pets", "wardrobe", "accessory", "accessories", "reforge", "enchant", "salvage",
            "collection", "collections", "slayer", "dungeon", "dungeons", "party", "profile", "quest", "quests",
            "experiment", "experimentation", "rune", "essence", "kuudra", "museum", "npc", "click", "teleport",
            "warp", "travel", "buy", "sell", "manage", "recipe", "craft", "abiphone", "chocolate", "composter",
            "garden", "community", "trade", "bestiary", "reward", "rewards", "settings", "options", "terminal", "terminals"
    };

    private SearchContainerPolicy() {}

    public static Kind resolve(GuiScreen screen) {
        if (screen instanceof GuiInventory) return Kind.PLAYER_INVENTORY;
        if (!(screen instanceof GuiChest)) return Kind.NONE;

        try {
            GuiChest chest = (GuiChest) screen;
            if (!(chest.inventorySlots instanceof ContainerChest)) return Kind.NONE;
            ContainerChest container = (ContainerChest) chest.inventorySlots;
            if (container.getLowerChestInventory() == null) return Kind.NONE;
            IChatComponent displayName = container.getLowerChestInventory().getDisplayName();
            if (displayName == null) return Kind.NONE;
            return classifyTitle(displayName.getUnformattedText());
        } catch (RuntimeException ignored) {
            // Unknown/custom containers are menus by default. Fail closed for search highlighting.
            return Kind.NONE;
        }
    }

    /** Package-visible pure title classifier, also used by focused regression checks. */
    static Kind classifyTitle(String rawTitle) {
        String title = normalizeTitle(rawTitle);
        if (title.isEmpty() || containsInteractiveMarker(title)) return Kind.NONE;

        if (title.equals("item list") || title.matches("^item list (?:page )?[0-9]+(?:\\s+[0-9]+)?$")) {
            return Kind.ITEM_LIST;
        }

        // The unnumbered "Storage" screen is a selector, not an item-storage page. Explicit
        // Vault/Ender Chest/Backpack titles are storage views; Bank Vault remains allowed because
        // "bank" alone is not treated as a menu marker. Titles like "Vault Menu" are rejected above.
        if (containsPhrase(title, "ender chest") || containsWord(title, "vault")
                || containsWord(title, "backpack") || isNumberedStoragePage(title)) {
            return Kind.STORAGE;
        }

        // Keep only ordinary vanilla container names. Arbitrary server chest titles fail closed,
        // preventing the overlay from drawing over SkyBlock menus and terminal screens.
        if ("chest".equals(title) || "large chest".equals(title)
                || "trapped chest".equals(title) || "barrel".equals(title)) {
            return Kind.VANILLA_CHEST;
        }

        return Kind.NONE;
    }

    private static boolean isNumberedStoragePage(String title) {
        if (!title.startsWith("storage ")) return false;
        String suffix = title.substring("storage".length()).trim();
        // Punctuation in titles (for example "Storage (1/18)" or "Storage #2") is normalized
        // to spaces before this check.
        if (suffix.startsWith("page ")) {
            return suffix.substring("page ".length()).matches("^[0-9]+(?:\\s+[0-9]+)?$");
        }
        return suffix.matches("^[0-9]+(?:\\s+[0-9]+)?$");
    }

    private static boolean containsInteractiveMarker(String title) {
        for (String marker : INTERACTIVE_MENU_MARKERS) {
            if (containsWord(title, marker)) return true;
        }
        return false;
    }

    private static boolean containsPhrase(String value, String phrase) {
        return (" " + value + " ").contains(" " + phrase + " ");
    }

    private static boolean containsWord(String value, String word) {
        return (" " + value + " ").contains(" " + word + " ");
    }

    private static String normalizeTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isEmpty()) return "";
        StringBuilder clean = new StringBuilder(rawTitle.length());
        for (int i = 0; i < rawTitle.length(); i++) {
            char c = rawTitle.charAt(i);
            if (c == '\u00a7' && i + 1 < rawTitle.length()) {
                i++;
                continue;
            }
            clean.append(c);
        }
        // Canonicalize punctuation so markers still match "Buy!" and "Click-in-Order" and
        // storage pages remain parseable regardless of brackets, hashes, slashes or separators.
        return clean.toString().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
}
