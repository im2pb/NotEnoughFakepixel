package com.nef.notenoughfakepixel.serverdata;

import java.util.regex.Pattern;

public record DungeonRunStats(double secretsPercent, int deaths, int elapsedSeconds, int crypts) {
    public static final DungeonRunStats EMPTY = new DungeonRunStats(-1, -1, -1, -1);

    private static final Pattern PERCENT = Pattern.compile("([0-9]{1,3}(?:\\.[0-9]+)?)\\s*%");
    private static final Pattern DEATHS = Pattern.compile("([0-9,]+)(?:\\s*\\(\\s*([0-9,]+)\\s*\\))?");
    private static final Pattern CRYPTS = Pattern.compile("([0-9,]+)(?:\\s*/\\s*[0-9,]+)?");
    private static final Pattern CLOCK = Pattern.compile("(?:(\\d+):)?(\\d+):([0-5]?\\d)");
    private static final Pattern DURATION = Pattern.compile("(?:(\\d+)h\\s*)?(?:(\\d+)m\\s*)?(?:(\\d+)s)?", Pattern.CASE_INSENSITIVE);

    static double parseSecrets(String value) {
        var percent = PERCENT.matcher(value);
        if (!percent.matches()) return -1;
        double number = Double.parseDouble(percent.group(1));
        return number <= 100 ? number : -1;
    }

    static int parseDeaths(String value) {
        var deaths = DEATHS.matcher(value);
        if (!deaths.matches()) return -1;
        return SkyblockText.parseNonnegativeInt(deaths.group(2) == null ? deaths.group(1) : deaths.group(2));
    }

    static int parseCrypts(String value) {
        var crypts = CRYPTS.matcher(value);
        return crypts.matches() ? SkyblockText.parseNonnegativeInt(crypts.group(1)) : -1;
    }

    static int parseElapsed(String value) {
        if (value.isEmpty()) return -1;
        var clock = CLOCK.matcher(value);
        if (clock.matches()) return seconds(clock.group(1), clock.group(2), clock.group(3));

        var duration = DURATION.matcher(value);
        return duration.matches() ? seconds(duration.group(1), duration.group(2), duration.group(3)) : -1;
    }

    private static int seconds(String hours, String minutes, String seconds) {
        int h = hours == null ? 0 : SkyblockText.parseNonnegativeInt(hours);
        int m = minutes == null ? 0 : SkyblockText.parseNonnegativeInt(minutes);
        int s = seconds == null ? 0 : SkyblockText.parseNonnegativeInt(seconds);
        if (h < 0 || m < 0 || s < 0) return -1;
        long total = h * 3600L + m * 60L + s;
        return total <= Integer.MAX_VALUE ? (int) total : -1;
    }
}
