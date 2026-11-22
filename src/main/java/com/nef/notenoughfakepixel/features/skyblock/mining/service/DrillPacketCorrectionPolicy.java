/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

/**
 * Pure policy for filtering the temporary Prismarine Shard slot update used by the drill animation fix.
 * Kept independent of Minecraft and Forge so the packet guards can be regression-tested directly.
 */
public final class DrillPacketCorrectionPolicy {
    private DrillPacketCorrectionPolicy() { }

    public static boolean isDrillInternalName(String internalName) {
        if (internalName == null || internalName.isEmpty()) return false;
        return internalName.contains("_DRILL_") || "DIVAN_DRILL".equals(internalName);
    }

    public static boolean shouldSuppressSingleSlotUpdate(boolean enabledContext,
                                                         boolean attackHeld,
                                                         boolean holdingDrill,
                                                         int selectedHotbarSlot,
                                                         int packetWindowId,
                                                         int packetSlot,
                                                         boolean packetContainsPrismarineShard) {
        return enabledContext && attackHeld && holdingDrill
                && selectedHotbarSlot >= 0 && selectedHotbarSlot < 9
                && packetWindowId == 0
                && packetSlot == selectedHotbarSlot + 36
                && packetContainsPrismarineShard;
    }

    public static boolean shouldRepairWindowItemsUpdate(boolean enabledContext,
                                                         boolean attackHeld,
                                                         boolean holdingDrill,
                                                         int selectedHotbarSlot,
                                                         int itemCount,
                                                         boolean selectedSlotContainsPrismarineShard) {
        return enabledContext && attackHeld && holdingDrill
                && selectedHotbarSlot >= 0 && selectedHotbarSlot < 9
                && itemCount == 45
                && selectedSlotContainsPrismarineShard;
    }
}
