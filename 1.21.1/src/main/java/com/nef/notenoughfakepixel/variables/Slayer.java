package com.nef.notenoughfakepixel.variables;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Slayer {

    NONE("NONE", new int[0]),
    VOIDGLOOM("Voidgloom", new int[]{10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000}),
    TARANTULA("Tarantula", new int[]{5, 25, 200, 1000, 5000, 20000, 100000, 400000, 1000000}),
    SVEN("Sven", new int[]{10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000}),
    REVENANT("Revenant", new int[]{5, 15, 200, 1000, 5000, 20000, 100000, 400000, 1000000}),
    INFERNO("Inferno", new int[]{10, 30, 250, 1500, 5000, 20000, 100000, 400000, 1000000});

    private final String displayName;
    private final int[] xpTable;

    public int maxLevel() {
        return xpTable.length;
    }

    public int xpForNextLevel(int currentLevel) {
        return xpTable[currentLevel];
    }

    public int accumulatedXp(int level, int xpTowardsNext) {
        if (level >= maxLevel()) return xpTable[maxLevel() - 1];
        return xpForNextLevel(level) - xpTowardsNext;
    }
}
