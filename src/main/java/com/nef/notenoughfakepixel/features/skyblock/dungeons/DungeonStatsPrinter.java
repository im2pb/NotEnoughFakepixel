/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.utils.ChatUtils;

import java.util.Locale;

public final class DungeonStatsPrinter {
    private DungeonStatsPrinter() {}

    public static void print(DungeonEndStats stats) {
        if (stats == null) return;
        String floor = stats.getFloor().name();
        ChatUtils.notifyChat("§8[NEF] §7Dungeon §f" + floor + " §7finished in §f" + format(stats.getTotalTime())
                + " §7| Score: §f" + stats.getScore());
        if (stats.getBloodTime() >= 0) {
            ChatUtils.notifyChat("§7Blood: §f" + format(stats.getBloodTime())
                    + (stats.getBossTime() >= 0 ? " §7| Boss: §f" + format(stats.getBossTime()) : ""));
        }
        if (stats.getXp() >= 0) ChatUtils.notifyChat("§7Dungeon XP: §f" + String.format(Locale.ROOT, "%,d", stats.getXp()));
        if (stats.getRecordStatus() != DungeonEndStats.RecordStatus.NONE) {
            ChatUtils.notifyChat("§7New PBs: §f" + stats.getNewRecordKeys());
        }
    }

    private static String format(long ms) {
        long minutes = ms / 60000L;
        long seconds = (ms / 1000L) % 60L;
        long millis = ms % 1000L;
        return String.format(Locale.ROOT, "%d:%02d.%03d", minutes, seconds, millis);
    }
}
