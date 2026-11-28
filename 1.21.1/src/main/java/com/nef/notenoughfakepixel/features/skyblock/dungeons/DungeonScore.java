package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.serverdata.DungeonRunStats;
import com.nef.notenoughfakepixel.variables.DungeonFloor;

record DungeonScore(int skill, int exploration, int speed, int bonus, double secretsPercent,
                    double sPlusSecretTarget, int floorSecretPercent) {
    static final int UNKNOWN = -1;
    static final int INSUFFICIENT_POINTS = -2;
    static final DungeonScore EMPTY = new DungeonScore(UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN);

    static DungeonScore estimate(DungeonFloor floor, int cleared, DungeonRunStats stats, int failedPuzzles,
                                 boolean spiritPet, boolean bloodOpen, boolean paul) {
        int deaths = stats.deaths();
        int skill = deaths < 0 ? UNKNOWN : (int) Math.clamp(
                100 - 2L * deaths - 14L * failedPuzzles + (spiritPet && deaths > 0 ? 1 : 0), 0, 100);
        int secretCap = floor.secretPercentage();
        double secrets = stats.secretsPercent();
        int exploration = UNKNOWN;
        if (cleared >= 0 && secrets >= 0 && secretCap > 0) {
            int clearPoints = 60 * Math.min(100, cleared + (bloodOpen ? 9 : 0)) / 100;
            int secretPoints = (int) Math.min(40, Math.floor(40 * secrets / secretCap));
            exploration = clearPoints + secretPoints;
        }

        int speed = stats.elapsedSeconds() < 0 || floor == DungeonFloor.NONE
                ? UNKNOWN : speedPoints((long) stats.elapsedSeconds() + floor.timeOffsetSeconds());
        int bonus = stats.crypts() < 0 ? UNKNOWN : Math.min(5, stats.crypts()) + (paul ? 10 : 0);
        double target = UNKNOWN;
        if (secretCap > 0 && skill >= 0 && speed >= 0 && bonus >= 0) {
            int pointsNeeded = Math.max(0, 300 - 60 - skill - speed - bonus);
            target = pointsNeeded > 40 ? INSUFFICIENT_POINTS : pointsNeeded * secretCap / 40.0;
        }
        return new DungeonScore(skill, exploration, speed, bonus, secrets, target, secretCap);
    }

    int total() {
        return skill < 0 || exploration < 0 || speed < 0 || bonus < 0 ? UNKNOWN : skill + exploration + speed + bonus;
    }

    static int speedPoints(long seconds) {
        if (seconds < 480) return 100;
        if (seconds < 600) return 140 - (int) Math.ceilDiv(seconds, 12);
        if (seconds < 840) return 115 - (int) Math.ceilDiv(seconds, 24);
        if (seconds < 1140) return 108 - (int) Math.ceilDiv(seconds, 30);
        if (seconds < 3940) return Math.max(0, 99 - (int) Math.ceilDiv(seconds, 40));
        return 0;
    }
}
