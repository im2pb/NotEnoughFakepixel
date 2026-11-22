/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/**
 * Samples drill fuel on the legacy 120-ClientTickEvent-dispatch cadence.
 * The previous listener counted both START and END dispatches, so this service
 * deliberately does not add a phase filter. It contains no Forge subscription; DrillFuelParsing dispatches
 * client tick events to it and mirrors its values into the legacy public fields.
 */
public final class DrillFuelSamplingService {
    private static final int SAMPLE_INTERVAL = 120;
    private static final DrillFuelSamplingService INSTANCE = new DrillFuelSamplingService();

    private final DrillFuelDataService dataService = new DrillFuelDataService();
    private int lastRead;

    public static DrillFuelSamplingService getInstance() {
        return INSTANCE;
    }

    /** Performs one sampling opportunity, preserving the original listener cadence and gates. */
    public boolean sampleOnTick() {
        if (!Config.feature.mining.miningDrillFuel) return false;

        if (lastRead < SAMPLE_INTERVAL) {
            lastRead++;
            return false;
        }

        final Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) return false;
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return false;

        final Location location = SkyblockData.getCurrentLocation();
        if (location != Location.DWARVEN && location != Location.CRYSTAL_HOLLOWS) return false;

        final ItemStack heldItem = minecraft.thePlayer.getHeldItem();
        if (heldItem == null) return false;

        final String internalName = ItemUtils.getInternalName(heldItem);
        if (!internalName.contains("_DRILL_") && !internalName.equals("DIVAN_DRILL")) return false;

        dataService.updateFromHeldItem(heldItem);
        lastRead = 0;
        return true;
    }

    public int getLastRead() {
        return lastRead;
    }

    /** Keeps writes through the legacy public DrillFuelParsing.lastRead field observable. */
    public void setLastRead(int value) {
        lastRead = value;
    }

    public int getFuel() {
        return dataService.getFuel();
    }

    public int getFuelMax() {
        return dataService.getFuelMax();
    }
}
