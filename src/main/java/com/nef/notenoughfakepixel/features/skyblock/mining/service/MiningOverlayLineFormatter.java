/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converts a collected mining HUD snapshot into its legacy ordered display lines. */
public final class MiningOverlayLineFormatter {
    private MiningOverlayLineFormatter() {
    }

    public static List<String> format(MiningOverlayDataSnapshot data) {
        if (data == null) return Collections.emptyList();

        List<String> lines = new ArrayList<String>();
        long cooldownRemaining = data.getAbilityCooldownRemainingMillis();
        if (cooldownRemaining >= 0L) {
            String cooldownDisplay = cooldownRemaining == 0L
                    ? "\u00a7aReady"
                    : "\u00a7c" + (cooldownRemaining / 1000L) + "s";
            lines.add("\u00a77Ability Cooldown: \u00a7r" + cooldownDisplay);
        }
        if (data.shouldShowMithrilPowder()) {
            lines.add(MiningPowderDisplayService.formatMithrilPowder(data.getMithrilPowder()));
        }
        if (data.shouldShowGemstonePowder()) {
            lines.add(MiningPowderDisplayService.formatGemstonePowder(data.getGemstonePowder()));
        }
        if (data.shouldShowDrillFuel()) {
            lines.add(DrillFuelDataService.formatFuel(data.getDrillFuel(), data.getDrillFuelMaximum()));
        }
        for (String commission : data.getCommissions()) {
            lines.add(CommissionDataService.formatCommission(commission));
        }
        return lines.isEmpty() ? Collections.<String>emptyList() : Collections.unmodifiableList(lines);
    }
}
