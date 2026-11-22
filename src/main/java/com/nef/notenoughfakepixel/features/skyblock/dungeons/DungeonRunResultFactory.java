/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import java.util.Collections;
import java.util.Set;

/**
 * Builds the immutable end-of-run summary from a completed run snapshot and
 * the results of score and personal-best processing. It has no game/event side effects.
 */
public final class DungeonRunResultFactory {
    private DungeonRunResultFactory() {}

    public static DungeonEndStats create(DungeonRunSnapshot snapshot,
                                         long totalTimeMs,
                                         int score,
                                         DungeonEndStats.RecordStatus recordStatus,
                                         Set<String> newRecordKeys) {
        if (snapshot == null) throw new IllegalArgumentException("snapshot cannot be null");

        return new DungeonEndStats(
                snapshot.getFloor(),
                snapshot.getMode(),
                snapshot.getRunId(),
                totalTimeMs,
                snapshot.getElapsedAt(snapshot.getBloodTimestamp()),
                snapshot.getElapsedAt(snapshot.getBossTimestamp()),
                snapshot.getElapsedAt(snapshot.getBossDeadTimestamp()),
                score,
                snapshot.getDungeonXp(),
                snapshot.getCompletedPhaseTimes(),
                recordStatus,
                newRecordKeys == null ? Collections.<String>emptySet() : newRecordKeys
        );
    }
}
