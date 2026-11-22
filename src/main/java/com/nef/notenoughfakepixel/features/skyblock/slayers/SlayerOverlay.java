/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.config.gui.core.config.Position;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.overlays.Overlay;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import com.nef.notenoughfakepixel.variables.Slayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Slayer progress HUD; state is ephemeral and never persisted to the server or player data. */
@RegisterEvents
public class SlayerOverlay extends Overlay {
    private long currentQuestStartMs;

    @Override
    public boolean shouldShow() {
        if (!Config.feature.slayer.slayerOverlay || !SkyblockData.getCurrentGamemode().isSkyblock()) return false;
        if (!SkyblockData.isSlayerActive() || SkyblockData.getCurrentSlayer() == Slayer.NONE) return false;
        if (SkyblockData.getCurrentLocation() == Location.PRIVATE_ISLAND) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (Config.feature.slayer.slayerOverlayHideOnTab && minecraft.gameSettings.keyBindPlayerList.isKeyDown()) return false;
        return !Config.feature.slayer.slayerOverlayHideOnChat || !(minecraft.currentScreen instanceof GuiChat);
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !shouldShow()) return;
        List<String> lines = getLines();
        if (lines.isEmpty()) return;
        float scale = Config.feature.slayer.slayerOverlayScale;
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int width = (int) (getWidth(scale, lines) * scale);
        int height = (int) (getHeight(scale, lines) * scale);
        Position position = Config.feature.slayer.slayerOverlayPos;
        float x = position.getAbsX(sr, width);
        float y = position.getAbsY(sr, height);
        draw(x, y, scale, Config.feature.slayer.slayerOverlayBackgroundColor, lines);
    }

    @Override
    public List<String> getLines() {
        Slayer slayer = SkyblockData.getCurrentSlayer();
        if (slayer == Slayer.NONE) return Collections.emptyList();
        List<String> lines = new ArrayList<>();
        lines.add("§4" + slayer.getName() + " §7Slayer");
        int level = SkyblockData.getSlayerLevel();
        if (level > 0) lines.add("§7Level: " + (level >= 9 ? "§aMAX" : "§c" + level));
        int xp = SkyblockData.getSlayerXp();
        int next = SkyblockData.getSlayerNextLevelXp();
        if (next > 0) lines.add("§7XP: §c" + format(xp) + " §7/ §a" + format(next));
        float meter = SkyblockData.getSlayerRngesusMeter();
        if (meter > 0.0f) lines.add("§dRNGesus Meter§7: §d" + String.format(java.util.Locale.ROOT, "%.1f", meter) + "%");
        int bosses = SkyblockData.getSlayerSessionBosses();
        if (bosses > 0) lines.add("§7Bosses This Session: §e" + bosses);
        long seconds = SkyblockData.getSlayerSessionSeconds();
        if (seconds > 0 && bosses > 0) {
            long average = seconds / bosses;
            lines.add("§7Average Kill: §e" + (average / 60) + "m " + (average % 60) + "s");
        }
        return lines.size() <= 1 ? Collections.<String>emptyList() : lines;
    }

    @Override public float getWidth(float scale, List<String> lines) { return Math.max(getLongestLine(lines) * 6 + 8, MINIMUM_WIDTH); }
    @Override public float getHeight(float scale, List<String> lines) { return Math.max(LINE_HEIGHT, lines.size() * LINE_HEIGHT + 4); }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event == null || event.message == null || !shouldShow()) return;
        String message = event.message.getUnformattedText();
        if (message.contains("SLAYER QUEST STARTED!") || message.contains("Slay the boss!")
                || (message.contains("» Slay ") && message.contains("XP worth of "))) {
            currentQuestStartMs = System.currentTimeMillis();
            if (message.contains("SLAYER QUEST STARTED!") || message.contains("» Slay ")) event.setCanceled(true);
        }
        if (message.contains("SLAYER QUEST COMPLETE!")) {
            SkyblockData.setSlayerSessionBosses(SkyblockData.getSlayerSessionBosses() + 1);
            if (currentQuestStartMs > 0L) {
                long seconds = Math.max(0L, (System.currentTimeMillis() - currentQuestStartMs) / 1000L);
                SkyblockData.setSlayerSessionSeconds(SkyblockData.getSlayerSessionSeconds() + seconds);
                currentQuestStartMs = 0L;
            }
        }
        if (message.contains("RNGesus Meter:")) {
            try {
                String raw = message.substring(message.indexOf(':') + 1).trim().replace("%", "");
                SkyblockData.setSlayerRngesusMeter(Float.parseFloat(raw));
                event.setCanceled(true);
            } catch (RuntimeException ignored) { }
        }
        if (message.contains("Slayer LVL") && SkyblockData.getCurrentSlayer() != Slayer.NONE) {
            try {
                int level = Integer.parseInt(message.split("Slayer LVL")[1].split("-")[0].trim());
                SkyblockData.setSlayerLevel(level);
                if (level >= 9) {
                    SkyblockData.setSlayerXp(0);
                    SkyblockData.setSlayerNextLevelXp(0);
                    SkyblockData.setSlayerXpToNextLevel(0);
                } else {
                    String remaining = message.split("Next LVL in")[1].trim().split(" ")[0].replace(",", "");
                    int xpToNext = Integer.parseInt(remaining);
                    Slayer current = SkyblockData.getCurrentSlayer();
                    SkyblockData.setSlayerXpToNextLevel(xpToNext);
                    SkyblockData.setSlayerNextLevelXp(current.getNextLevelXp(level));
                    SkyblockData.setSlayerXp(current.getAccumulatedXp(level, xpToNext));
                }
                event.setCanceled(true);
            } catch (RuntimeException ignored) { /* Ignore format changes; retain the last valid state. */ }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        currentQuestStartMs = 0L;
        SkyblockData.setSlayerLevel(0);
        SkyblockData.setSlayerXp(0);
        SkyblockData.setSlayerNextLevelXp(0);
        SkyblockData.setSlayerXpToNextLevel(0);
        SkyblockData.setSlayerRngesusMeter(0.0f);
    }

    private String format(int value) { return String.format(java.util.Locale.ROOT, "%,d", Math.max(0, value)); }
}
