/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.mining.AbilityNotifier;
import com.nef.notenoughfakepixel.features.skyblock.mining.DrillFuelParsing;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.TablistParser;
import com.nef.notenoughfakepixel.variables.Location;

import java.util.Collections;
import java.util.List;

/** Collects the current mining HUD values without formatting or rendering them. */
public final class MiningOverlayDataProvider {

    private MiningOverlayDataSnapshot cachedSnapshot;

    /**
     * Captures the values and feature gates during the client-tick refresh cycle.
     * This method is not called from the HUD render callback.
     */
    public MiningOverlayDataSnapshot collect() {
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return cacheEmptySnapshot();
        Location location = SkyblockData.getCurrentLocation();
        if (location != Location.DWARVEN && location != Location.CRYSTAL_HOLLOWS) {
            return cacheEmptySnapshot();
        }

        long abilityCooldownRemaining = Config.feature.mining.miningAbilityCooldown
                ? AbilityNotifier.cooldownRemainingMillis() : -1L;
        boolean showMithril = Config.feature.mining.miningMithrilPowder && location == Location.DWARVEN;
        int mithrilPowder = showMithril ? SkyblockData.getMithrilPowder() : 0;
        boolean showGemstone = Config.feature.mining.miningGemstonePowder && location == Location.CRYSTAL_HOLLOWS;
        int gemstonePowder = showGemstone ? SkyblockData.getGemstonePowder() : 0;
        boolean showDrillFuel = Config.feature.mining.miningDrillFuel;
        int drillFuel = showDrillFuel ? DrillFuelParsing.fuel : 0;
        int drillFuelMaximum = showDrillFuel ? DrillFuelParsing.fuelMax : 0;
        List<String> commissions = Config.feature.mining.miningCommissions
                ? TablistParser.commissions
                : Collections.<String>emptyList();

        if (cachedSnapshot != null && cachedSnapshot.hasSameDisplayValues(
                abilityCooldownRemaining,
                showMithril,
                mithrilPowder,
                showGemstone,
                gemstonePowder,
                showDrillFuel,
                drillFuel,
                drillFuelMaximum,
                commissions)) {
            return cachedSnapshot;
        }

        cachedSnapshot = new MiningOverlayDataSnapshot(
                abilityCooldownRemaining,
                showMithril,
                mithrilPowder,
                showGemstone,
                gemstonePowder,
                showDrillFuel,
                drillFuel,
                drillFuelMaximum,
                commissions
        );
        return cachedSnapshot;
    }

    private MiningOverlayDataSnapshot cacheEmptySnapshot() {
        cachedSnapshot = MiningOverlayDataSnapshot.empty();
        return cachedSnapshot;
    }

    /** Drops the last sample after a world transition or explicit lifecycle reset. */
    public void reset() {
        cachedSnapshot = null;
    }
}
