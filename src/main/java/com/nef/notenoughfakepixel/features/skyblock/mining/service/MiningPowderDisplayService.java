/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

/**
 * Formats the powder counters displayed by the mining overlay.
 * Parsing and ownership of live values remain in TablistParser and SkyblockData.
 */
public final class MiningPowderDisplayService {
    private MiningPowderDisplayService() {
    }

    public static String formatMithrilPowder(int value) {
        return String.format("\u00a77Mithril Powder: \u00a72%d", value);
    }

    public static String formatGemstonePowder(int value) {
        return String.format("\u00a77Gemstone Powder: \u00a7d%d", value);
    }
}
