/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.render;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.diana.burrows.ParticleProcessor;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.utils.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

import java.awt.Color;
import java.util.List;

/** Owns all Diana burrow waypoint drawing, independent of packet and detection logic. */
public final class BurrowWaypointRenderer {
    private final ParticleProcessor processor;

    public BurrowWaypointRenderer(ParticleProcessor processor) {
        if (processor == null) throw new IllegalArgumentException("Particle processor cannot be null");
        this.processor = processor;
    }

    /** Takes one waypoint snapshot and performs all enabled passes in a single traversal. */
    public void render(float partialTicks) {
        boolean showWaypoints = Config.feature.diana.dianaShowWaypointsBurrows;
        boolean showTracers = Config.feature.diana.dianaShowTracersWaypoints;
        boolean showLabels = Config.feature.diana.dianaShowLabelsWaypoints;
        if (!showWaypoints && !showTracers && !showLabels) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null) return;

        // CopyOnWriteArrayList supplies a stable, allocation-free iterator snapshot.
        List<Waypoint> snapshot = processor.getWaypoints();
        if (snapshot.isEmpty()) return;

        int playerX = (int) Math.floor(player.posX);
        int playerZ = (int) Math.floor(player.posZ);
        Vec3 eyes = showTracers ? player.getPositionEyes(partialTicks) : null;

        for (Waypoint waypoint : snapshot) {
            if (waypoint == null || waypoint.isHidden()) continue;
            int[] coords = waypoint.getCoordinates();
            if (coords == null || coords.length < 3) continue;

            Color base = burrowColor(waypoint.getType());
            BlockPos block = new BlockPos(coords[0], coords[1] - 1, coords[2]);

            if (showWaypoints) {
                RenderUtils.highlightBlock(block, base, true, partialTicks);
                boolean isClose = Math.abs(playerX - coords[0]) <= 5
                        && Math.abs(playerZ - coords[2]) <= 5;
                if (!isClose) RenderUtils.renderBeaconBeam(block, base.getRGB(), 1.0f, partialTicks);
                GlStateManager.enableTexture2D();
            }

            if (showTracers) {
                Color tracer = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);
                RenderUtils.draw3DLine(new Vec3(coords[0] + 0.5, coords[1], coords[2] + 0.5),
                        eyes, tracer, 4, true, partialTicks);
            }

            if (showLabels) {
                String type = waypoint.getType();
                String label;
                if ("MINOS".equals(type)) label = "Inquisitor";
                else if ("EMPTY".equals(type)) label = "EMPTY";
                else if ("MOB".equals(type)) label = "MOB";
                else if ("TREASURE".equals(type)) label = "TREASURE";
                else label = type;
                RenderUtils.renderWaypointText(label, new BlockPos(coords[0], coords[1] + 1, coords[2]), partialTicks);
            }
        }
    }

    private Color burrowColor(String type) {
        Color color = new Color(255, 255, 255, 100);
        if ("EMPTY".equals(type)) {
            color = ColorUtils.getColor(Config.feature.diana.dianaEmptyBurrowColor);
        } else if ("MOB".equals(type)) {
            color = ColorUtils.getColor(Config.feature.diana.dianaMobBurrowColor);
        } else if ("TREASURE".equals(type)) {
            color = ColorUtils.getColor(Config.feature.diana.dianaTreasureBurrowColor);
        } else if ("MINOS".equals(type)) {
            color = new Color(243, 225, 107);
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
    }
}
