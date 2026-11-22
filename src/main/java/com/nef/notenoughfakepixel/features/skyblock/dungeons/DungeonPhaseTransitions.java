/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

/** Stateless rules for ordering dungeon phases and mapping them to run states. */
public final class DungeonPhaseTransitions {
    private DungeonPhaseTransitions() {}

    public static boolean isForwardTransition(DungeonContext.Phase current, DungeonContext.Phase next) {
        return phaseRank(next) > phaseRank(current);
    }

    public static DungeonContext.RunState stateForPhase(DungeonContext.Phase value) {
        if (value == null) return DungeonContext.RunState.NONE;
        switch (value) {
            case BLOOD: return DungeonContext.RunState.BLOOD;
            case BOSS:
            case MAXOR:
            case STORM:
            case TERMINAL:
            case GOLDOR:
            case NECRON: return DungeonContext.RunState.BOSS;
            case END: return DungeonContext.RunState.ENDING;
            case RUN: return DungeonContext.RunState.RUNNING;
            case NONE:
            default: return DungeonContext.RunState.NONE;
        }
    }

    public static int phaseRank(DungeonContext.Phase value) {
        if (value == null) return 0;
        switch (value) {
            case RUN: return 1;
            case BLOOD: return 2;
            case BOSS: return 3;
            case MAXOR: return 4;
            case STORM: return 5;
            case TERMINAL: return 6;
            case GOLDOR: return 7;
            case NECRON: return 8;
            case END: return 9;
            case NONE:
            default: return 0;
        }
    }
}
