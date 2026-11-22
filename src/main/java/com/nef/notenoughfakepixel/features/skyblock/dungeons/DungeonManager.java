/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.serverdata.SkyblockData;

public final class DungeonManager {
    private DungeonManager() {}

    public static boolean checkEssentials() {
        return DungeonContext.isInDungeon() || isDungeonLocation();
    }

    public static boolean checkEssentialsF7() {
        return checkEssentials() && DungeonContext.getFloor() != null && DungeonContext.getFloor().isF7orM7();
    }

    public static boolean isBossStage() {
        DungeonContext.Phase phase = DungeonContext.getPhase();
        return phase == DungeonContext.Phase.BOSS || phase == DungeonContext.Phase.MAXOR
                || phase == DungeonContext.Phase.STORM || phase == DungeonContext.Phase.TERMINAL
                || phase == DungeonContext.Phase.GOLDOR || phase == DungeonContext.Phase.NECRON;
    }

    public static boolean isFinalStage() {
        return DungeonContext.getPhase() == DungeonContext.Phase.END
                || DungeonContext.getRunState() == DungeonContext.RunState.FINISHED;
    }

    private static boolean isDungeonLocation() {
        try {
            return SkyblockData.getCurrentGamemode().isSkyblock()
                    && SkyblockData.getCurrentLocation().isDungeon();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
