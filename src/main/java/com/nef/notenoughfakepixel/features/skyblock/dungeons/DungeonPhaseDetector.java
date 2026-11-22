/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import net.minecraft.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DungeonPhaseDetector {
    public enum Signal {
        NONE,
        BLOOD_OPEN,
        BOSS_START,
        MAXOR,
        STORM,
        TERMINAL,
        GOLDOR,
        NECRON,
        EXTRA_STATS
    }

    private static final Pattern EXTRA_STATS = Pattern.compile("> EXTRA STATS <", Pattern.CASE_INSENSITIVE);
    private static final Pattern BLOOD = Pattern.compile("The BLOOD DOOR has been opened!", Pattern.CASE_INSENSITIVE);
    private static final Pattern BOSS = Pattern.compile("\\[BOSS\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAXOR = Pattern.compile("\\[BOSS\\].*\\bmaxor\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STORM = Pattern.compile("\\[BOSS\\].*\\bstorm\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TERMINAL = Pattern.compile("\\bterminal(?:s)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GOLDOR = Pattern.compile("\\[BOSS\\].*\\bgoldor\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NECRON = Pattern.compile("\\[BOSS\\].*\\bnecron\\b", Pattern.CASE_INSENSITIVE);

    private DungeonPhaseDetector() {}

    public static Signal detect(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) return Signal.NONE;
        String message = StringUtils.stripControlCodes(rawMessage);
        if (message == null || message.isEmpty()) return Signal.NONE;
        if (EXTRA_STATS.matcher(message).find()) return Signal.EXTRA_STATS;
        if (BLOOD.matcher(message).find()) return Signal.BLOOD_OPEN;

        if (MAXOR.matcher(message).find()) return Signal.MAXOR;
        if (STORM.matcher(message).find()) return Signal.STORM;
        if (GOLDOR.matcher(message).find()) return Signal.GOLDOR;
        if (NECRON.matcher(message).find()) return Signal.NECRON;

        String clean = message.toLowerCase(Locale.ROOT);
        if (BOSS.matcher(message).find()) return Signal.BOSS_START;
        if (clean.contains("the core entrance is opening")) return Signal.GOLDOR;
        if (TERMINAL.matcher(message).find() && clean.contains("completed")) return Signal.TERMINAL;
        return Signal.NONE;
    }
}
