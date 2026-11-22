/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralizes the policy decisions that govern the beginning and end of a dungeon run.
 * It deliberately contains no Forge event handlers or Minecraft client dependencies.
 */
public final class DungeonRunLifecycleService {
    private static final AtomicLong RUN_SEQUENCE = new AtomicLong();

    private DungeonRunLifecycleService() {
    }

    /** A newly detected dungeon starts a run unless one is already active. */
    public static boolean shouldBeginDetectedRun(boolean inDungeon) {
        return !inDungeon;
    }

    /** A finished run may be replaced once the server's cleared percentage resets. */
    public static boolean shouldStartNextRun(boolean inDungeon, DungeonContext.RunState state, int clearedPercentage) {
        return inDungeon && state == DungeonContext.RunState.FINISHED
                && clearedPercentage >= 0 && clearedPercentage < 10;
    }

    /** Leaving a dungeon after the final phase preserves the completed result. */
    public static boolean shouldFinishWhenLeaving(DungeonContext.Phase phase, DungeonContext.RunState state) {
        return phase == DungeonContext.Phase.END || state == DungeonContext.RunState.ENDING;
    }

    public static boolean shouldFinishOnDisconnect(boolean inDungeon, DungeonContext.Phase phase,
                                                   DungeonContext.RunState state) {
        return inDungeon && shouldFinishWhenLeaving(phase, state);
    }

    public static boolean shouldFinishOnWorldUnload(boolean inDungeon, DungeonContext.Phase phase,
                                                    DungeonContext.RunState state) {
        return inDungeon && shouldFinishWhenLeaving(phase, state);
    }

    /** Monotonic identifier retained across resets, matching the previous context-owned sequence. */
    public static long nextRunId() {
        return RUN_SEQUENCE.incrementAndGet();
    }
}
