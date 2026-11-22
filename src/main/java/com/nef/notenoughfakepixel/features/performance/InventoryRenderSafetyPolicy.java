/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.performance;

/**
 * Fail-safe render policy for inventory-bearing and item-display objects.
 * This class is deliberately independent from Minecraft classes so its rules can
 * be regression-tested without starting the game client.
 */
public final class InventoryRenderSafetyPolicy {

    private InventoryRenderSafetyPolicy() {
    }

    /**
     * True when an entity contains inventory, displays an item, or visibly carries gear.
     * Such entities must bypass optional render culling.
     */
    public static boolean mustAlwaysRenderEntity(boolean droppedItem,
                                                 boolean inventoryBearingEntity,
                                                 boolean itemFrameWithDisplayedItem,
                                                 boolean livingEntityWithEquipment) {
        return droppedItem
                || inventoryBearingEntity
                || itemFrameWithDisplayedItem
                || livingEntityWithEquipment;
    }

    /**
     * True when a tile entity is an inventory container or an interactive block model
     * whose disappearance would make a chest/menu interaction misleading.
     */
    public static boolean mustAlwaysRenderTileEntity(boolean inventoryBearingTileEntity,
                                                     boolean enderChest,
                                                     boolean sign,
                                                     boolean interactiveMenuSource,
                                                     boolean beacon) {
        return inventoryBearingTileEntity || enderChest || sign || interactiveMenuSource || beacon;
    }

    /**
     * Do not apply optional world-render culling while any container-style GUI is open.
     */
    public static boolean shouldBypassForContainerGui(boolean containerGuiOpen) {
        return containerGuiOpen;
    }
}
