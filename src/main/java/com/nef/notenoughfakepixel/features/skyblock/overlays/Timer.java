/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.overlays;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public abstract class Timer implements GatedListener {

    private long lastBucketSec = Long.MIN_VALUE;

    private String cachedText = "0:00";

    private static final long goal = 0L;

    @SubscribeEvent
    public void onRenderGameOverlayEvent(RenderGameOverlayEvent.Text event) {
        if (!shouldShow()) return;

        updateCache();
        render(event);
    }

    @Override
    public boolean isListenerActive() {
        return shouldShow();
    }

    private void updateCache() {
        long deltaMs = getGoalEpochMs() - System.currentTimeMillis();
        long bucket = deltaMs / 1000L;
        if (bucket != lastBucketSec) {
            lastBucketSec = bucket;
            cachedText = formatDelta(deltaMs);
        }
    }

    public void render(RenderGameOverlayEvent.Text event) {
        preRender();
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;

        long deltaMs = getGoalEpochMs() - System.currentTimeMillis();
        int color = getTextColor(deltaMs);
        float scale = getScale();

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1f);

        ScaledResolution sr = new ScaledResolution(mc);
        int screenW = sr.getScaledWidth();
        int screenH = sr.getScaledHeight();

        int objW = getObjectWidth();
        int objH = getObjectHeight();

        int clampedX = getX();
        int clampedY = getY();
        if (clampedX + objW > screenW) clampedX = Math.max(0, screenW - objW);
        if (clampedY + objH > screenH) clampedY = Math.max(0, screenH - objH);
        if (clampedX < 0) clampedX = 0;
        if (clampedY < 0) clampedY = 0;

        int rx = (int) (clampedX / scale);
        int ry = (int) (clampedY / scale);

        int textW = fr.getStringWidth(cachedText);
        int textH = fr.FONT_HEIGHT;

        int texW = getIconTextureWidth();
        int texH = getIconTextureHeight();
        int iconDrawH = textH;
        int iconDrawW = (int) Math.round((texW / (double) texH) * iconDrawH);

        int cursorX = rx;

        ResourceLocation icon = getIcon();
        if (icon != null) {
            mc.getTextureManager().bindTexture(icon);

            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    770, 771, 1, 0);
            GlStateManager.color(1f, 1f, 1f, 1f);

            GlStateManager.enableBlend();
            Gui.drawModalRectWithCustomSizedTexture(
                    cursorX, ry - 1,
                    0f, 0f,
                    iconDrawW, iconDrawH,
                    iconDrawW, iconDrawH
            );

            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();

            cursorX += iconDrawW + getPadding();
        }

        if (getTextShadow()) {
            fr.drawStringWithShadow(cachedText, cursorX, ry, color);
        } else {
            fr.drawString(cachedText, cursorX, ry, color);
        }

        GlStateManager.popMatrix();
    }

    public int getObjectWidth() {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        int textW = fr.getStringWidth(cachedText);
        int texW = getIconTextureWidth();
        int texH = getIconTextureHeight();
        int textH = fr.FONT_HEIGHT;
        int iconDrawW = (int) Math.round((texW / (double) texH) * textH);
        return (int)(getScale() * (textW + (getIcon() != null ? iconDrawW + getPadding() : 0)));
    }

    public int getObjectHeight() {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRendererObj;
        int textH = fr.FONT_HEIGHT;
        return (int)(getScale() * textH);
    }

    protected int getTextColor(long deltaMs) {
        if (deltaMs < 0) return 0xFFFFFF55;
        if (deltaMs < 5000L) return 0xFF55FF55;
        return 0xFFFFFFFF;
    }

    protected String formatDelta(long deltaMs) {
        if (deltaMs < 0) deltaMs = 0L;
        long totalSec = deltaMs / 1000L;
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        return (min < 10 ? "0" + min : String.valueOf(min)) + ":" +
                (sec < 10 ? "0" + sec : String.valueOf(sec));
    }

    public abstract boolean shouldShow();

    public void preRender() {}

    public abstract ResourceLocation getIcon();
    protected int getIconTextureWidth()  { return 16; }
    protected int getIconTextureHeight() { return 16; }

    public abstract long getGoalEpochMs();

    public int getX() { return 0; }
    public int getY() { return 0; }

    public boolean getTextShadow() { return false; }
    public float getScale() { return 1f; }
    public int getPadding() { return 3; }

}
