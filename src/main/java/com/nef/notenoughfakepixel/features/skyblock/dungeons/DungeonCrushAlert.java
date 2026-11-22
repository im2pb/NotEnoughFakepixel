/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import com.nef.notenoughfakepixel.utils.SkyblockGate;
import com.nef.notenoughfakepixel.utils.SoundUtils;
import com.nef.notenoughfakepixel.utils.TitleUtils;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RegisterEvents
public final class DungeonCrushAlert {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static DungeonCrushAlert instance;
    private final List<BlockPos> pillars = new ArrayList<>();
    private boolean insideWarning;
    private String lastPositionString = "";
    private int tickCounter;

    public DungeonCrushAlert() {
        instance = this;
    }

    public static DungeonCrushAlert getInstance() {
        return instance;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.feature.dungeons.dungeonsCrushAlert || !SkyblockGate.isFakepixelDungeon() || MC.thePlayer == null) {
            insideWarning = false;
            return;
        }
        DungeonFloor floor = DungeonContext.getFloor();
        if (!floor.isF7orM7() || DungeonContext.getPhase() != DungeonContext.Phase.STORM) {
            insideWarning = false;
            return;
        }
        if (++tickCounter < 2) return;
        tickCounter = 0;
        refreshPillars();
        double range = Math.max(0.5D, Config.feature.dungeons.dungeonsCrushWarningRange);
        double rangeSq = range * range;
        boolean inRange = false;
        for (BlockPos pillar : pillars) {
            if (MC.thePlayer.getDistanceSqToCenter(pillar) <= rangeSq) {
                inRange = true;
                break;
            }
        }
        if (inRange && !insideWarning) {
            insideWarning = true;
            String title = EnumChatFormatting.RED + "CRUSH STORM";
            TitleUtils.showTitle(title, 900);
            SoundUtils.playGlobalSound(Config.feature.dungeons.dungeonsCrushAlertSound, 1.0F, 1.0F);
            if (Config.feature.dungeons.dungeonsCrushAlertChat) ChatUtils.notifyChat("§c[NEF] Crush warning!");
        } else if (!inRange) {
            insideWarning = false;
        }
    }

    private void refreshPillars() {
        String config = Config.feature.dungeons.dungeonsCrushPillarPositions == null ? "" : Config.feature.dungeons.dungeonsCrushPillarPositions.trim();
        if (config.equals(lastPositionString)) return;
        lastPositionString = config;
        pillars.clear();
        if (config.isEmpty()) return;
        String[] entries = config.split(";");
        for (String entry : entries) {
            String[] parts = entry.trim().split(",");
            if (parts.length != 3) continue;
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());
                pillars.add(new BlockPos(x, y, z));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            insideWarning = false;
            tickCounter = 0;
            pillars.clear();
            lastPositionString = "";
        }
    }

    public List<BlockPos> snapshotPillars() {
        return Collections.unmodifiableList(new ArrayList<>(pillars));
    }

    public boolean isInsideWarning() {
        return insideWarning;
    }
}
