/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.commands;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterCommand;
import com.nef.notenoughfakepixel.features.protection.ItemProtection;
import com.nef.notenoughfakepixel.features.inventory.search.SearchBar;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonContext;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonCrushAlert;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonParty;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonsMap;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.LeapingSwordCobwebs;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import com.nef.notenoughfakepixel.utils.Logger;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;
import java.util.List;

@RegisterCommand
public class NefDebugCommand extends CommandBase {
    @Override public String getCommandName() { return "nefdebug"; }
    @Override public List<String> getCommandAliases() { return Arrays.asList("debug-nef"); }
    @Override public String getCommandUsage(ICommandSender sender) { return "/nefdebug <dungeon|map|party|leap|protection|search|crush>"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || "dungeon".equalsIgnoreCase(args[0])) {
            ChatUtils.notifyChat("§7[NEF] dungeon=" + DungeonContext.isInDungeon() + " floor=" + DungeonContext.getFloor()
                    + " mode=" + DungeonContext.getMode() + " state=" + DungeonContext.getRunState()
                    + " phase=" + DungeonContext.getPhase() + " runId=" + DungeonContext.getRunId());
            ChatUtils.notifyChat("§7[NEF] run=" + DungeonContext.getRunStartTimestamp()
                    + " blood=" + DungeonContext.getBloodTimestamp() + " boss=" + DungeonContext.getBossTimestamp()
                    + " bossDead=" + DungeonContext.getBossDeadTimestamp() + " cleared=" + DungeonContext.getClearedTimestamp()
                    + " elapsed=" + DungeonContext.elapsed() + " xp=" + DungeonContext.getDungeonXp());
            return;
        }
        if ("map".equalsIgnoreCase(args[0])) {
            ChatUtils.notifyChat("§7[NEF] mapId=" + DungeonsMap.getCachedMapId()
                    + " renderer=vanilla MapItemRenderer");
            ChatUtils.notifyChat("§7[NEF] markers=vanilla mapDecorations, names=hold key");
            return;
        }
        if ("party".equalsIgnoreCase(args[0])) {
            
            for (String line : DungeonParty.debugDump()) {
                ChatUtils.notifyChat("\u00a77[NEF] " + line);
                Logger.logConsole("[NEF Party] " + line);
            }
            return;
        }
        if ("leap".equalsIgnoreCase(args[0])) {
            boolean on = LeapingSwordCobwebs.toggleDebug();
            ChatUtils.notifyChat("\u00a77[NEF] Leaping Sword debug log " + (on ? "\u00a7aON" : "\u00a7cOFF")
                    + "\u00a77 (right-click the sword; entities within 12 blocks are written to the log)");
            return;
        }
        if ("protection".equalsIgnoreCase(args[0])) {
            ChatUtils.notifyChat("§7[NEF] protected UUIDs=" + ItemProtection.getStore().snapshot().size());
            return;
        }
        if ("search".equalsIgnoreCase(args[0])) {
            ChatUtils.notifyChat("§7[NEF] search=\"" + SearchBar.getSearchText() + "\" calc=\"" + SearchBar.getLastCalculationResult() + "\"");
            return;
        }
        if ("crush".equalsIgnoreCase(args[0])) {
            DungeonCrushAlert alert = DungeonCrushAlert.getInstance();
            int entries = alert == null ? 0 : alert.snapshotPillars().size();
            ChatUtils.notifyChat("§7[NEF] Crush configured pillar entries=" + entries + " active=" + (alert != null && alert.isInsideWarning()));
            return;
        }
        ChatUtils.notifyChat("§c[NEF] Unknown debug target. Use /nefdebug <dungeon|map|party|leap|protection|search|crush>.");
    }
}
