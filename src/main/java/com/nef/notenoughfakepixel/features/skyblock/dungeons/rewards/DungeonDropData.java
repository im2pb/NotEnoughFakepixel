/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.rewards;

import net.minecraft.item.ItemStack;

public final class DungeonDropData {
    private final ItemStack item;
    private final String identifier;
    private final String displayName;
    private final String rarity;
    private final RewardRarity rarityLevel;
    private final int quantity;

    public DungeonDropData(ItemStack item, String identifier, String displayName, String rarity, int quantity) {
        this.item = item;
        this.identifier = identifier == null ? "" : identifier;
        this.displayName = displayName == null ? "" : displayName;
        this.rarity = rarity == null ? "" : rarity;
        this.rarityLevel = RewardRarity.from(this.rarity);
        this.quantity = Math.max(1, quantity);
    }
    public ItemStack getItem() { return item; }
    public String getIdentifier() { return identifier; }
    public String getDisplayName() { return displayName; }
    public String getRarity() { return rarity; }
    public RewardRarity getRarityLevel() { return rarityLevel; }
    public int getQuantity() { return quantity; }
    public ItemStack getStack() { return item; }
}
