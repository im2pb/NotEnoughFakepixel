package com.nef.notenoughfakepixel.variables;

import com.nef.notenoughfakepixel.utils.ListUtils;

import java.util.Arrays;
import java.util.List;

public enum StackingEnchant {
    NONE("", new int[]{}),
    CULTIVATING("CULTIVATING", new int[]{
        1000,
        4000,
        20000,
        75000,
        200000,
        1200000,
        3500000,
        15000000,
        80000000
    }),
    COMPACT("COMPACT", new int[]{
        100,
        400,
        1000,
        3500,
        10000,
        35000,
        100000,
        350000,
        500000
    }),
    EXPERTISE("EXPERTISE", new int[]{
        50,
        50,
        150,
        250,
        500,
        1500,
        3000,
        4500,
        5000
    }),
    CHAMPION("CHAMPION", new int[]{
        50000,
        50000,
        150000,
        250000,
        500000,
        500000,
        500000,
        500000,
        500000
    }),
    TOXOPHILITE("TOXOPHILITE", new int[]{
        50000,
        50000,
        150000,
        250000,
        500000,
        500000,
        500000,
        500000,
        500000
    });

    private final String displayName;
    private final int[] thresholds;

    StackingEnchant(String displayName, int[] thresholds) {
        this.displayName = displayName;
        this.thresholds = thresholds;
    }

    public Integer getNextLevel(int current) {
        for (int threshold : thresholds) if (current < threshold) return threshold;
        return null;
    }

    public static Integer getNextLevel(int current, StackingEnchant enchant) {
        return enchant == null ? null : enchant.getNextLevel(current);
    }

    public static StackingEnchant fromString(String name) {
        if (name == null) return NONE;
        return Arrays.stream(values()).filter(e -> e.displayName.equalsIgnoreCase(name.trim())).findFirst().orElse(NONE);
    }
}