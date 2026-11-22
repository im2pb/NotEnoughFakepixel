/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.commands;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterCommand;
import com.nef.notenoughfakepixel.features.protection.ItemProtection;
import com.nef.notenoughfakepixel.features.protection.ProtectedItemStore;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@RegisterCommand
public class ProtectItemCommand extends CommandBase {
    @Override public String getCommandName() { return "nefprotect"; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/nefprotect [list|clear]"; }
    @Override public int getRequiredPermissionLevel() { return 0; }
    @Override public List<String> getCommandAliases() { return java.util.Arrays.asList("protectitem"); }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0 && "list".equalsIgnoreCase(args[0])) {
            java.util.Set<String> ids = ItemProtection.getStore().snapshot();
            ChatUtils.notifyChat("§7[NEF] Protected items: §f" + ids.size());
            for (String id : ids) ChatUtils.notifyChat("§8- §f" + id);
            return;
        }
        if (args.length > 0 && "clear".equalsIgnoreCase(args[0])) {
            ItemProtection.getStore().clear();
            ChatUtils.notifyChat("§e[NEF] Protected item list cleared.");
            return;
        }
        ItemStack held = net.minecraft.client.Minecraft.getMinecraft().thePlayer == null ? null : net.minecraft.client.Minecraft.getMinecraft().thePlayer.getHeldItem();
        if (!ItemProtection.toggle(held)) ChatUtils.notifyChat("§c[NEF] This item has no SkyBlock UUID and cannot be protected.");
    }
}
