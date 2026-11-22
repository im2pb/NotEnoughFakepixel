/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.commands;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterCommand;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonPBTracker;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@RegisterCommand
public class PBCommand extends CommandBase {
    @Override public String getCommandName() { return "pb"; }
    @Override public List<String> getCommandAliases() { return Arrays.asList("nef-pb"); }
    @Override public String getCommandUsage(ICommandSender sender) { return "/pb <f1-f7|m1-m7> [reset]"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            ChatUtils.notifyChat("§c[NEF] Usage: /pb m7  (fastest full run)  |  /pb m7 reset");
            return;
        }
        String floor = args[0].toUpperCase(Locale.ROOT);
        if (DungeonFloor.getFloor(floor) == DungeonFloor.NONE) {
            ChatUtils.notifyChat("§c[NEF] Unknown dungeon floor.");
            return;
        }
        String key = floor.toLowerCase(Locale.ROOT) + "_total";

        if (args.length >= 2 && args[1].equalsIgnoreCase("reset")) {
            ChatUtils.notifyChat(DungeonPBTracker.remove(key)
                    ? "§7[NEF] Fastest §f" + floor + " §7run cleared. The next finished run will set it again."
                    : "§7[NEF] No fastest " + floor + " run was saved.");
            return;
        }

        long value = DungeonPBTracker.get(key);
        ChatUtils.notifyChat(value < 0
                ? "§7[NEF] No " + floor + " run saved yet."
                : "§a[NEF] Fastest §f" + floor + " §arun: §f" + format(value));
    }

    private static String format(long ms) {
        long seconds = ms / 1000L;
        return String.format(Locale.ROOT, "%d:%02d.%03d", seconds / 60L, seconds % 60L, ms % 1000L);
    }
}
