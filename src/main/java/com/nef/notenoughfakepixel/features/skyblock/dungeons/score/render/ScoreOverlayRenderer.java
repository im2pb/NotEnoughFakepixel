/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.score.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.util.List;

/** Pure presentation boundary for the dungeon score HUD. */
public final class ScoreOverlayRenderer {
    private final Minecraft minecraft;

    public ScoreOverlayRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void render(float x, float y, float scale, int backgroundColor, List<String> lines) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0);
            GlStateManager.scale(scale, scale, scale);

            int textWidth = 0;
            for (String line : lines) {
                textWidth = Math.max(textWidth, minecraft.fontRendererObj.getStringWidth(line));
            }
            int width = textWidth + 4;
            int height = lines.size() * 11;
            Gui.drawRect(0, 0, width, height, backgroundColor);
            for (int i = 0; i < lines.size(); i++) {
                minecraft.fontRendererObj.drawString(lines.get(i), 2, i * 11, -1);
            }
        } finally {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }
    }
}
