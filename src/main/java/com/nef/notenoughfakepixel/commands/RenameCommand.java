/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nef.notenoughfakepixel.config.gui.commands.SimpleCommand;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterCommand;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.utils.Logger;
import com.nef.notenoughfakepixel.variables.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RegisterCommand
public class RenameCommand extends SimpleCommand {
    public static final Map<String, String> renamedItems = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File getSaveFile() {
        return new File(Config.configDirectory, "renamed_items.json");
    }

    public static synchronized void load() {
        File file = getSaveFile();
        if (!file.isFile()) return;
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<HashMap<String, String>>() { }.getType();
            Map<String, String> loaded = GSON.fromJson(reader, type);
            if (loaded != null) {
                renamedItems.clear();
                for (Map.Entry<String, String> entry : loaded.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().isEmpty() && entry.getValue() != null)
                        renamedItems.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception error) {
            Logger.logError("Failed to load local item-name overrides: " + error.getMessage());
        }
    }

    public static synchronized void save() {
        File file = getSaveFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return;
        File temp = new File(file.getPath() + ".tmp");
        try {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(temp.toPath()), StandardCharsets.UTF_8)) {
                GSON.toJson(renamedItems, writer);
            }
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception error) {
            temp.delete();
            Logger.logError("Failed to save local item-name overrides: " + error.getMessage());
        }
    }

    @Override public String getName() { return "rename"; }
    @Override public String getUsage() { return "/rename <name>"; }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        // SkyblockData exposes the current mode through its enum-backed getter.
        if (SkyblockData.getCurrentGamemode() == null || !SkyblockData.getCurrentGamemode().isSkyblock()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        if (args == null || args.length == 0) {
            mc.thePlayer.addChatMessage(new ChatComponentText(Constants.PREFIX_INFO + "Usage: /rename <name>"));
            mc.thePlayer.addChatMessage(new ChatComponentText(Constants.PREFIX_INFO + "Use & followed by a Minecraft color/format code; example: /rename &aMy Cool Sword"));
            return;
        }
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            Logger.logErrorPlayers("You must be holding an item to rename it!");
            return;
        }
        String uuid = ItemUtils.getItemUUID(held);
        if (uuid == null || uuid.isEmpty()) {
            Logger.logErrorPlayers("This item has no UUID and cannot receive a persistent local name override.");
            return;
        }
        String raw = String.join(" ", args).replace('&', '§');
        boolean explicitItalic = raw.contains("§o") || raw.contains("§O");
        String name = explicitItalic ? raw : (raw.startsWith("§") ? "§r" + raw : "§r§b" + raw);
        renamedItems.put(uuid, name);
        // Keep the override local to the tooltip; do not mutate the ItemStack NBT.
        save();
        mc.thePlayer.addChatMessage(new ChatComponentText(Constants.PREFIX_INFO + "Local item name set to: §r" + name));
    }
}
