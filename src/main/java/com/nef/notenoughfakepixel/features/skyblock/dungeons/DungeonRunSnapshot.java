/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.variables.DungeonFloor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable point-in-time view of a dungeon run.
 * This object contains run data only; it performs no game or event operations.
 */
public final class DungeonRunSnapshot {
    private final boolean inDungeon;
    private final DungeonFloor floor;
    private final DungeonContext.Mode mode;
    private final DungeonContext.RunState runState;
    private final DungeonContext.Phase phase;
    private final long runStartTimestamp;
    private final long bloodTimestamp;
    private final long bossTimestamp;
    private final long bossDeadTimestamp;
    private final long clearedTimestamp;
    private final long dungeonXp;
    private final long runId;
    private final boolean startAnchored;
    private final String tabClockAtBegin;
    private final long serverClearMs;
    private final Map<String, Long> phaseStarts;
    private final Map<String, Long> completedPhaseTimes;

    DungeonRunSnapshot(boolean inDungeon, DungeonFloor floor, DungeonContext.Mode mode,
                       DungeonContext.RunState runState, DungeonContext.Phase phase,
                       long runStartTimestamp, long bloodTimestamp, long bossTimestamp,
                       long bossDeadTimestamp, long clearedTimestamp, long dungeonXp,
                       long runId, boolean startAnchored, String tabClockAtBegin,
                       long serverClearMs, Map<String, Long> phaseStarts,
                       Map<String, Long> completedPhaseTimes) {
        this.inDungeon = inDungeon;
        this.floor = floor == null ? DungeonFloor.NONE : floor;
        this.mode = mode == null ? DungeonContext.Mode.NONE : mode;
        this.runState = runState == null ? DungeonContext.RunState.NONE : runState;
        this.phase = phase == null ? DungeonContext.Phase.NONE : phase;
        this.runStartTimestamp = runStartTimestamp;
        this.bloodTimestamp = bloodTimestamp;
        this.bossTimestamp = bossTimestamp;
        this.bossDeadTimestamp = bossDeadTimestamp;
        this.clearedTimestamp = clearedTimestamp;
        this.dungeonXp = dungeonXp;
        this.runId = runId;
        this.startAnchored = startAnchored;
        this.tabClockAtBegin = tabClockAtBegin == null ? "" : tabClockAtBegin;
        this.serverClearMs = serverClearMs;
        this.phaseStarts = immutableCopy(phaseStarts);
        this.completedPhaseTimes = immutableCopy(completedPhaseTimes);
    }

    private static Map<String, Long> immutableCopy(Map<String, Long> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source == null
                ? Collections.<String, Long>emptyMap() : source));
    }

    public boolean isInDungeon() { return inDungeon; }
    public DungeonFloor getFloor() { return floor; }
    public DungeonContext.Mode getMode() { return mode; }
    public DungeonContext.RunState getRunState() { return runState; }
    public DungeonContext.Phase getPhase() { return phase; }
    public long getRunStartTimestamp() { return runStartTimestamp; }
    public long getBloodTimestamp() { return bloodTimestamp; }
    public long getBossTimestamp() { return bossTimestamp; }
    public long getBossDeadTimestamp() { return bossDeadTimestamp; }
    public long getClearedTimestamp() { return clearedTimestamp; }
    public long getDungeonXp() { return dungeonXp; }
    public long getRunId() { return runId; }
    public boolean isStartAnchored() { return startAnchored; }
    public String getTabClockAtBegin() { return tabClockAtBegin; }
    public long getServerClearMs() { return serverClearMs; }
    public Map<String, Long> getPhaseStarts() { return phaseStarts; }
    public Map<String, Long> getCompletedPhaseTimes() { return completedPhaseTimes; }

    public long getElapsedAt(long timestamp) {
        if (timestamp < 0L || runStartTimestamp < 0L) return -1L;
        return Math.max(0L, timestamp - runStartTimestamp);
    }
}
