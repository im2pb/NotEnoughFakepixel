/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.variables.DungeonFloor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class DungeonEndStats {
    public enum RecordStatus {
        NONE, NEW_TOTAL, NEW_PHASE, NEW_TOTAL_AND_PHASE
    }

    private final DungeonFloor floor;
    private final DungeonContext.Mode mode;
    private final long runId;
    private final long totalTime;
    private final long bloodTime;
    private final long bossTime;
    private final long bossDeadTime;
    private final int score;
    private final long xp;
    private final Map<String, Long> phaseTimes;
    private final RecordStatus recordStatus;
    private final Set<String> newRecordKeys;

    public DungeonEndStats(DungeonFloor floor, DungeonContext.Mode mode, long runId, long totalTime,
                           long bloodTime, long bossTime, long bossDeadTime, int score, long xp,
                           Map<String, Long> phaseTimes, RecordStatus recordStatus, Set<String> newRecordKeys) {
        this.floor = floor == null ? DungeonFloor.NONE : floor;
        this.mode = mode == null ? DungeonContext.Mode.NONE : mode;
        this.runId = runId;
        this.totalTime = Math.max(0, totalTime);
        this.bloodTime = bloodTime;
        this.bossTime = bossTime;
        this.bossDeadTime = bossDeadTime;
        this.score = score;
        this.xp = xp;
        this.phaseTimes = Collections.unmodifiableMap(new LinkedHashMap<>(phaseTimes == null
                ? Collections.<String, Long>emptyMap() : phaseTimes));
        this.recordStatus = recordStatus == null ? RecordStatus.NONE : recordStatus;
        this.newRecordKeys = Collections.unmodifiableSet(new LinkedHashSet<>(newRecordKeys == null
                ? Collections.<String>emptySet() : newRecordKeys));
    }

    public DungeonFloor getFloor() { return floor; }
    public DungeonContext.Mode getMode() { return mode; }
    public long getRunId() { return runId; }
    public long getTotalTime() { return totalTime; }
    public long getBloodTime() { return bloodTime; }
    public long getBossTime() { return bossTime; }
    public long getBossDeadTime() { return bossDeadTime; }
    public int getScore() { return score; }
    public long getXp() { return xp; }
    public Map<String, Long> getPhaseTimes() { return phaseTimes; }
    public RecordStatus getRecordStatus() { return recordStatus; }
    public Set<String> getNewRecordKeys() { return newRecordKeys; }
}
