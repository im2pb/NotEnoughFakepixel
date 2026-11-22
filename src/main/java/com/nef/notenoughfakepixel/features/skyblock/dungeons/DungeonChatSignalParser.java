/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses dungeon chat payloads into data without mutating the active run context. */
public final class DungeonChatSignalParser {
    private static final Pattern DUNGEON_XP = Pattern.compile(
            "(?:Catacombs|Dungeon) XP\\s*[:+]?\\s*([0-9][0-9,]*)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CLEAR_TIME = Pattern.compile(
            "Defeated\\s+.+?\\s+in\\s+(?:(\\d+)h\\s*)?(?:(\\d+)m\\s*)?(\\d+)s\\b",
            Pattern.CASE_INSENSITIVE
    );

    private DungeonChatSignalParser() {}

    public static Result parse(String chatLine) {
        if (chatLine == null || chatLine.isEmpty()) {
            return new Result(-1L, -1L, DungeonPhaseDetector.Signal.NONE);
        }

        long clearTimeMs = -1L;
        Matcher clearMatcher = CLEAR_TIME.matcher(chatLine);
        if (clearMatcher.find()) {
            try {
                long hours = clearMatcher.group(1) == null ? 0L : Long.parseLong(clearMatcher.group(1));
                long minutes = clearMatcher.group(2) == null ? 0L : Long.parseLong(clearMatcher.group(2));
                long seconds = Long.parseLong(clearMatcher.group(3));
                clearTimeMs = ((hours * 60L + minutes) * 60L + seconds) * 1000L;
            } catch (NumberFormatException ignored) {
            }
        }

        long dungeonXp = -1L;
        Matcher xpMatcher = DUNGEON_XP.matcher(chatLine);
        if (xpMatcher.find()) {
            try {
                dungeonXp = Long.parseLong(xpMatcher.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
            }
        }

        return new Result(clearTimeMs, dungeonXp, DungeonPhaseDetector.detect(chatLine));
    }

    public static final class Result {
        private final long serverClearTimeMs;
        private final long dungeonXp;
        private final DungeonPhaseDetector.Signal phaseSignal;

        private Result(long serverClearTimeMs, long dungeonXp, DungeonPhaseDetector.Signal phaseSignal) {
            this.serverClearTimeMs = serverClearTimeMs;
            this.dungeonXp = dungeonXp;
            this.phaseSignal = phaseSignal == null ? DungeonPhaseDetector.Signal.NONE : phaseSignal;
        }

        public long getServerClearTimeMs() { return serverClearTimeMs; }
        public long getDungeonXp() { return dungeonXp; }
        public DungeonPhaseDetector.Signal getPhaseSignal() { return phaseSignal; }
    }
}
