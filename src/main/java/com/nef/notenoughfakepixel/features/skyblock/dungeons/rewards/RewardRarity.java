/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.rewards;

import java.util.Locale;

public enum RewardRarity {
    COMMON(0),
    UNCOMMON(1),
    RARE(2),
    EPIC(3),
    LEGENDARY(4),
    MYTHIC(5),
    DIVINE(6),
    SPECIAL(7),
    UNKNOWN(-1);

    private final int order;

    RewardRarity(int order) {
        this.order = order;
    }

    public int getOrder() { return order; }

    public static RewardRarity from(String value) {
        if (value == null) return UNKNOWN;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("pray rng") || normalized.contains("special")) return SPECIAL;
        if (normalized.contains("divine")) return DIVINE;
        if (normalized.contains("mythic")) return MYTHIC;
        if (normalized.contains("legendary")) return LEGENDARY;
        if (normalized.contains("epic")) return EPIC;
        if (normalized.contains("rare")) return RARE;
        if (normalized.contains("uncommon")) return UNCOMMON;
        if (normalized.contains("common")) return COMMON;
        return UNKNOWN;
    }
}
