/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

/**
 * Parses and formats commission progress for the mining overlay.
 * This class is independent from Minecraft and Forge.
 */
public final class CommissionDataService {
    private CommissionDataService() {
    }

    /** Formats a tab-list commission line, preserving unknown input unchanged. */
    public static String formatCommission(String commission) {
        if (commission == null) return "";
        String[] split = commission.split(":");
        try {
            if (split.length == 0) return commission;
            double percent = Double.parseDouble(split[1].replaceAll("[ %]", ""));
            String colorCode = percent <= 33 ? "\u00a7c" : percent <= 79 ? "\u00a7e" : "\u00a7a";
            return "\u00a77" + split[0] + ": " + colorCode + percent + "%";
        } catch (Exception ignored) {
            return commission;
        }
    }
}
