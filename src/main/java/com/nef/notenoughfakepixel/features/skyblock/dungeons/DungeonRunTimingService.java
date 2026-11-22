/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure timing operations used by the dungeon run tracker.
 * This class deliberately has no Minecraft or Forge dependencies.
 */
public final class DungeonRunTimingService {
    private static final Pattern UNIT_CLOCK = Pattern.compile("^(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?$");

    private DungeonRunTimingService() {}

    public static long elapsedSince(long startTimestamp, long now) {
        if (startTimestamp <= 0L) return -1L;
        return Math.max(0L, now - startTimestamp);
    }

    public static long elapsedAt(long runStartTimestamp, long timestamp) {
        if (timestamp < 0L || runStartTimestamp < 0L) return -1L;
        return Math.max(0L, timestamp - runStartTimestamp);
    }

    public static long phaseElapsed(Map<String, Long> phaseStarts, String key, long now) {
        if (phaseStarts == null || key == null || key.trim().isEmpty()) return -1L;
        Long start = phaseStarts.get(key.trim().toUpperCase(Locale.ROOT));
        if (start == null || start <= 0L) return -1L;
        return Math.max(0L, now - start);
    }

    public static Map<String, Long> completedPhaseSnapshot(Map<String, Long> completedPhaseTimes) {
        if (completedPhaseTimes == null || completedPhaseTimes.isEmpty()) return Collections.emptyMap();
        return Collections.unmodifiableMap(new LinkedHashMap<>(completedPhaseTimes));
    }

    public static void completePhase(String phaseName, Map<String, Long> phaseStarts,
                                     Map<String, Long> completedPhaseTimes, long endTimestamp) {
        if (phaseName == null || phaseStarts == null || completedPhaseTimes == null) return;
        Long start = phaseStarts.get(phaseName);
        if (start == null || start <= 0L || completedPhaseTimes.containsKey(phaseName)) return;
        completedPhaseTimes.put(phaseName, Math.max(0L, endTimestamp - start));
    }

    /** Returns the local start timestamp implied by a valid tab-list clock, or -1 when invalid. */
    public static long anchoredStartFromClock(String clock, long now) {
        int seconds = parseClockSeconds(clock);
        if (seconds < 0 || seconds > 7200) return -1L;
        return now - seconds * 1000L;
    }

    /** Parses mm:ss, hh:mm:ss, or compact h/m/s text. */
    public static int parseClockSeconds(String clock) {
        if (clock == null) return -1;
        String text = clock.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) return -1;
        try {
            if (text.contains(":")) {
                String[] parts = text.split(":");
                if (parts.length < 2 || parts.length > 3) return -1;
                int total = 0;
                for (String part : parts) total = total * 60 + Integer.parseInt(part.trim());
                return total;
            }
            Matcher matcher = UNIT_CLOCK.matcher(text);
            if (!matcher.matches()) return -1;
            int hours = matcher.group(1) == null ? 0 : Integer.parseInt(matcher.group(1));
            int minutes = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
            int seconds = matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3));
            return hours * 3600 + minutes * 60 + seconds;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
