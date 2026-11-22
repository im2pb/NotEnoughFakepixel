/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.map;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec4b;
import net.minecraft.world.storage.MapData;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DungeonMapRenderer {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");

    private static final float SMOOTHING_SECONDS = 0.06F;
    
    private static final float SNAP_DISTANCE = 24F;
    
    private static final float MAX_FRAME_SECONDS = 0.1F;

    private static final class SmoothMarker {
        float x;
        float z;
        float rotation; 
        byte type;
        int generation;
    }

    private static final Map<String, SmoothMarker> SMOOTH = new HashMap<>();
    private static final Map<String, Vec4b> NO_DECORATIONS = new LinkedHashMap<>();
    private static final IntBuffer SCISSOR_BOX = BufferUtils.createIntBuffer(16);
    private static int generation;
    private static long lastFrameNanos;

    private DungeonMapRenderer() {}

    public static void reset() {
        if (!SMOOTH.isEmpty()) SMOOTH.clear();
        lastFrameNanos = 0L;
    }

    public static void render(MapData mapData, float x, float y, float scale) {
        if (mapData == null || MC.entityRenderer == null || MC.entityRenderer.getMapItemRenderer() == null) return;

        boolean showFullMap = DungeonPlayerTracker.shouldShowPlayerNames();
        Vec4b self = Config.feature.dungeons.dungeonsRotateMap && !showFullMap ? findSelf(mapData) : null;
        boolean rotate = self != null;
        boolean smooth = !rotate && !showFullMap && Config.feature.dungeons.dungeonsMapSmoothMarkers
                && mapData.mapDecorations != null && !mapData.mapDecorations.isEmpty();

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0F);
            GlStateManager.scale(scale, scale, 1F);
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();

            if (rotate) {
                reset();
                renderRotated(mapData, self, x, y, scale);
            } else if (smooth) {
                renderSmooth(mapData);
            } else {
                reset();

                MC.entityRenderer.getMapItemRenderer().renderMap(mapData, false);
            }

            drawBorder();
            renderHeldPlayerNames();
        } finally {

            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.popMatrix();
        }
    }

    private static Vec4b findSelf(MapData mapData) {
        if (mapData.mapDecorations == null) return null;
        for (Vec4b value : mapData.mapDecorations.values()) {
            if (value != null && value.func_176110_a() == 1) return value;
        }
        return null;
    }

    private static void renderRotated(MapData mapData, Vec4b self, float x, float y, float scale) {
        float pivotX = self.func_176112_b() / 2.0F + 64.0F;
        float pivotZ = self.func_176113_c() / 2.0F + 64.0F;
        float angle = 180F - MathHelper.wrapAngleTo180_float(MC.thePlayer.rotationYaw);

        ScaledResolution sr = new ScaledResolution(MC);
        int factor = sr.getScaleFactor();
        int boxX = Math.round(x * factor);
        int boxSize = Math.max(Math.round(128F * scale * factor), 0);
        int boxY = MC.displayHeight - Math.round(y * factor) - boxSize;

        boolean scissorWasOn = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        SCISSOR_BOX.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR_BOX);
        int prevX = SCISSOR_BOX.get(0);
        int prevY = SCISSOR_BOX.get(1);
        int prevW = SCISSOR_BOX.get(2);
        int prevH = SCISSOR_BOX.get(3);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(boxX, boxY, boxSize, boxSize);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(64F, 64F, 0F);
            GlStateManager.rotate(angle, 0F, 0F, 1F);
            GlStateManager.translate(-pivotX, -pivotZ, 0F);

            Map<String, Vec4b> real = mapData.mapDecorations;
            mapData.mapDecorations = NO_DECORATIONS;
            try {
                MC.entityRenderer.getMapItemRenderer().renderMap(mapData, false);
            } finally {
                mapData.mapDecorations = real;
            }

            beginMarkers();
            int index = 0;
            for (Vec4b marker : real.values()) {
                if (marker == null) continue;
                boolean isSelf = marker.func_176110_a() == 1;
                
                float rotation = isSelf ? (180F - angle) * 16F / 360F : marker.func_176111_d();
                drawMarker(marker.func_176112_b() / 2.0F + 64.0F, marker.func_176113_c() / 2.0F + 64.0F,
                        rotation, marker.func_176110_a(), index++);
            }
        } finally {
            GlStateManager.popMatrix();
            if (scissorWasOn) {
                GL11.glScissor(prevX, prevY, prevW, prevH);
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }
    }

    private static void drawBorder() {
        Color color = ColorUtils.getColor(Config.feature.dungeons.dungeonsMapBorderColor);
        if (color.getAlpha() <= 0) return;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
        GL11.glLineWidth(2F);
        try {
            buffer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
            buffer.pos(0.0D, 0.0D, 0.0D).endVertex();
            buffer.pos(128.0D, 0.0D, 0.0D).endVertex();
            buffer.pos(128.0D, 128.0D, 0.0D).endVertex();
            buffer.pos(0.0D, 128.0D, 0.0D).endVertex();
            tessellator.draw();
        } finally {
            GL11.glLineWidth(1F);
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
        }
    }

    private static void renderSmooth(MapData mapData) {
        Map<String, Vec4b> real = mapData.mapDecorations;

        mapData.mapDecorations = NO_DECORATIONS;
        try {
            MC.entityRenderer.getMapItemRenderer().renderMap(mapData, false);
        } finally {
            mapData.mapDecorations = real;
        }

        advance(real);
        drawMarkers();
    }

    private static void advance(Map<String, Vec4b> real) {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? MAX_FRAME_SECONDS : (now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;
        if (dt < 0F) dt = 0F;
        if (dt > MAX_FRAME_SECONDS) dt = MAX_FRAME_SECONDS;
        float blend = 1F - (float) Math.exp(-dt / SMOOTHING_SECONDS);

        int gen = ++generation;
        for (Map.Entry<String, Vec4b> entry : real.entrySet()) {
            Vec4b target = entry.getValue();
            if (target == null) continue;

            float targetX = target.func_176112_b() / 2.0F + 64.0F;
            float targetZ = target.func_176113_c() / 2.0F + 64.0F;
            float targetRotation = target.func_176111_d();

            SmoothMarker marker = SMOOTH.get(entry.getKey());
            if (marker == null || marker.type != target.func_176110_a()
                    || Math.abs(targetX - marker.x) + Math.abs(targetZ - marker.z) > SNAP_DISTANCE) {
                if (marker == null) {
                    marker = new SmoothMarker();
                    SMOOTH.put(entry.getKey(), marker);
                }
                marker.x = targetX;
                marker.z = targetZ;
                marker.rotation = targetRotation;
                marker.type = target.func_176110_a();
            } else {
                marker.x += (targetX - marker.x) * blend;
                marker.z += (targetZ - marker.z) * blend;

                float delta = targetRotation - marker.rotation;
                while (delta > 8F) delta -= 16F;
                while (delta < -8F) delta += 16F;
                marker.rotation += delta * blend;
                if (marker.rotation < 0F) marker.rotation += 16F;
                else if (marker.rotation >= 16F) marker.rotation -= 16F;
            }
            marker.generation = gen;
        }

        for (Iterator<SmoothMarker> it = SMOOTH.values().iterator(); it.hasNext(); ) {
            if (it.next().generation != gen) it.remove();
        }
    }

    private static void beginMarkers() {
        MC.getTextureManager().bindTexture(MAP_ICONS);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private static void drawMarkers() {
        if (SMOOTH.isEmpty()) return;

        beginMarkers();
        int index = 0;
        for (SmoothMarker marker : SMOOTH.values()) {
            drawMarker(marker.x, marker.z, marker.rotation, marker.type, index++);
        }
    }

    private static void drawMarker(float x, float z, float rotation, byte type, int index) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, z, -0.02F);
            GlStateManager.rotate(rotation * 360F / 16F, 0F, 0F, 1F);
            GlStateManager.scale(4F, 4F, 3F);
            GlStateManager.translate(-0.125F, 0.125F, 0F);

            int icon = type & 15;
            float u0 = (icon % 4) / 4F;
            float v0 = (icon / 4) / 4F;
            float u1 = (icon % 4 + 1) / 4F;
            float v1 = (icon / 4 + 1) / 4F;
            float depth = index * -0.001F;

            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(-1.0D, 1.0D, depth).tex(u0, v0).endVertex();
            buffer.pos(1.0D, 1.0D, depth).tex(u1, v0).endVertex();
            buffer.pos(1.0D, -1.0D, depth).tex(u1, v1).endVertex();
            buffer.pos(-1.0D, -1.0D, depth).tex(u0, v1).endVertex();
            tessellator.draw();
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private static void renderHeldPlayerNames() {
        if (!DungeonPlayerTracker.shouldShowPlayerNames()) return;

        List<DungeonPlayerTracker.PlayerMarker> markers = DungeonPlayerTracker.snapshot();
        if (markers == null || markers.isEmpty()) return;

        for (DungeonPlayerTracker.PlayerMarker marker : markers) {
            if (marker == null || !marker.hasMapPosition()) continue;

            String name = StringUtils.stripControlCodes(marker.getName());
            if (name == null || name.isEmpty()) continue;

            float labelX = (float) marker.getMapX();
            float labelZ = (float) marker.getMapZ();
            SmoothMarker shown = marker.getKey() == null ? null : SMOOTH.get(marker.getKey());
            if (shown != null) {
                labelX = shown.x;
                labelZ = shown.z;
            }

            int width = MC.fontRendererObj.getStringWidth(name);
            MC.fontRendererObj.drawStringWithShadow(name, labelX - width / 2F, labelZ - 9F, 0xFFFFFFFF);
        }
    }
}
