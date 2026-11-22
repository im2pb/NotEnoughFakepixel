/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.score;

import com.nef.notenoughfakepixel.variables.DungeonFloor;

/**
 * Stateless dungeon score formulas. Runtime state and Minecraft data collection
 * remain owned by ScoreManager; this class only calculates values from inputs.
 */
public final class DungeonScoreCalculator {

    private DungeonScoreCalculator() {
    }

    public static int skillScore(int deaths, int reducedPenalty, int failedPuzzles) {
        return Math.max(Math.min(100 - (deaths - reducedPenalty) * 2 - failedPuzzles * 14, 100), 0);
    }

    public static int explorationClearScore(int clearedPercentage, boolean bloodOpen) {
        if (bloodOpen) {
            clearedPercentage = Math.min(clearedPercentage + 9, 100);
        }
        return (int) Math.max(Math.min(Math.floor(60f * clearedPercentage / 100f), 60), 0);
    }

    public static int explorationSecretScore(int secretPercentage, DungeonFloor floor) {
        int secretNeeded = floor == null ? -1 : floor.getSecretPercentage();
        if (secretNeeded <= 0) return 0;
        return (int) Math.max(Math.min(Math.floor(40f * secretPercentage / secretNeeded), 40f), 0);
    }

    public static int speedScore(int currentSeconds, DungeonFloor floor) {
        if (currentSeconds == -1) {
            return 100;
        }
        if (floor == null || floor == DungeonFloor.NONE) return 0;
        int t = currentSeconds + floor.getT();
        if (t < 480) return 100;
        if (t < 600) return 140 - (int) Math.ceil(t * (1f / 12f));
        if (t < 840) return 115 - (int) Math.ceil(t * (1f / 24f));
        if (t < 1140) return 108 - (int) Math.ceil(t * (1f / 30f));
        if (t < 3940) return (int) Math.ceil(98.5f - (int) Math.ceil(t * (1f / 40f)));
        return 0;
    }

    public static int bonusScore(int crypts, boolean paulActive) {
        return (paulActive ? 10 : 0) + Math.min(5, crypts);
    }

    public static int requiredSecretPercentage(int skillScore, int speedScore, int bonusScore, DungeonFloor floor) {
        int secretScoreNeeded = 300 - 60 - skillScore - speedScore - bonusScore;
        if (secretScoreNeeded > 40) return -1;
        int secretNeeded = floor == null ? -1 : floor.getSecretPercentage();
        if (secretNeeded <= 0) return -1;
        return (int) Math.ceil(secretScoreNeeded * secretNeeded / 40f);
    }
}
