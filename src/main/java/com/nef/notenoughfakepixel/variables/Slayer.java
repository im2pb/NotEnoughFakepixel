/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.variables;

public enum Slayer {
    NONE("None", new int[0]),
    VOIDGLOOM("Voidgloom", new int[]{10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000}),
    TARANTULA("Tarantula", new int[]{5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000}),
    SVEN("Sven", new int[]{10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000}),
    REVENANT("Revenant", new int[]{5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000}),
    INFERNO("Inferno", new int[]{10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000});

    private final String displayName;
    private final int[] xpTable;

    Slayer(String displayName, int[] xpTable) {
        this.displayName = displayName;
        this.xpTable = xpTable;
    }

    public String getName() { return displayName; }

    public int getNextLevelXp(int currentLevel) {
        if (xpTable.length == 0 || currentLevel < 0 || currentLevel >= xpTable.length) return 0;
        return xpTable[currentLevel];
    }

    public int getAccumulatedXp(int level, int xpTowardsNext) {
        if (xpTable.length == 0) return 0;
        if (level >= xpTable.length) return xpTable[xpTable.length - 1];
        if (level < 0) return 0;
        return Math.max(0, xpTable[level] - Math.max(0, xpTowardsNext));
    }
}
