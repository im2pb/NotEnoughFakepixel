/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.records;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonEndStats;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonPBTracker;
import com.nef.notenoughfakepixel.variables.DungeonFloor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Owns the policy for deciding whether a completed dungeon run can update the
 * persisted total-time personal best. Run lifecycle and presentation stay in
 * DungeonContext and DungeonStatsPrinter respectively.
 */
public final class DungeonPersonalBestService {
    private static final long MIN_TOTAL_DURATION_MS = 30_000L;

    private DungeonPersonalBestService() {}

    public static boolean isTrustworthyTotal(long durationMs, boolean hasServerClearTime, boolean hasBossTimestamp) {
        return durationMs >= MIN_TOTAL_DURATION_MS && (hasServerClearTime || hasBossTimestamp);
    }

    public static Result recordTotal(DungeonFloor floor, long durationMs, boolean trustworthy) {
        if (!Config.feature.dungeonSystems.personalBest || !trustworthy
                || floor == null || floor == DungeonFloor.NONE || durationMs < 0L) {
            return Result.none();
        }

        String key = floor.name().toLowerCase(Locale.ROOT) + "_total";
        if (!DungeonPBTracker.updateIfBetter(key, durationMs)) return Result.none();

        Set<String> keys = new LinkedHashSet<>();
        keys.add(key);
        return new Result(DungeonEndStats.RecordStatus.NEW_TOTAL, keys, true);
    }

    public static void save() {
        DungeonPBTracker.save();
    }

    public static final class Result {
        private final DungeonEndStats.RecordStatus status;
        private final Set<String> newRecordKeys;
        private final boolean newTotalRecord;

        private Result(DungeonEndStats.RecordStatus status, Set<String> newRecordKeys, boolean newTotalRecord) {
            this.status = status == null ? DungeonEndStats.RecordStatus.NONE : status;
            this.newRecordKeys = Collections.unmodifiableSet(new LinkedHashSet<>(newRecordKeys));
            this.newTotalRecord = newTotalRecord;
        }

        private static Result none() {
            return new Result(DungeonEndStats.RecordStatus.NONE, Collections.<String>emptySet(), false);
        }

        public DungeonEndStats.RecordStatus getStatus() { return status; }
        public Set<String> getNewRecordKeys() { return newRecordKeys; }
        public boolean isNewTotalRecord() { return newTotalRecord; }
    }
}
