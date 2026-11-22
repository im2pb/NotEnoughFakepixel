/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.variables.DungeonFloor;

/**
 * Pure dungeon boss-name and floor policy. Minecraft name cleanup remains at the event boundary;
 * this service receives the normalized, control-code-free name.
 */
public final class DungeonBossDetectionService {
    private DungeonBossDetectionService() {
    }

    /** Classifies a normalized entity name using the same floor-specific final-boss rules. */
    public static Detection detect(String cleanName, DungeonFloor floor) {
        String name = cleanName == null ? "" : cleanName.trim().toLowerCase(java.util.Locale.ROOT);
        boolean tracked = isTrackedBossName(name);
        return new Detection(tracked, tracked && isFinalBossName(name, floor));
    }

    private static boolean isTrackedBossName(String name) {
        return name.equals("maxor") || name.equals("storm") || name.equals("goldor")
                || name.equals("necron") || name.equals("sadan") || name.contains("professor")
                || name.equals("scarf") || name.equals("bonzo") || name.equals("thorn")
                || name.equals("livid");
    }

    private static boolean isFinalBossName(String name, DungeonFloor floor) {
        if (name.equals("necron")) return true;
        switch (floor == null ? DungeonFloor.NONE : floor) {
            case F1:
            case M1:
                return name.equals("bonzo");
            case F2:
            case M2:
                return name.equals("scarf");
            case F3:
            case M3:
                return name.contains("professor");
            case F4:
            case M4:
                return name.equals("thorn");
            case F5:
            case M5:
                return name.equals("livid");
            case F6:
            case M6:
                return name.equals("sadan");
            case F7:
            case M7:
                return false;
            case NONE:
            default:
                return true;
        }
    }

    public static final class Detection {
        private final boolean trackedBoss;
        private final boolean finalBoss;

        private Detection(boolean trackedBoss, boolean finalBoss) {
            this.trackedBoss = trackedBoss;
            this.finalBoss = finalBoss;
        }

        public boolean isTrackedBoss() {
            return trackedBoss;
        }

        public boolean isFinalBoss() {
            return finalBoss;
        }
    }
}
