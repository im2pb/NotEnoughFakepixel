/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.rewards;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@RegisterEvents
public class DungeonRewardOverlay {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final RenderItem RENDER_ITEM = Minecraft.getMinecraft().getRenderItem();

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !(MC.currentScreen instanceof GuiChest)) return;
        if (!com.nef.notenoughfakepixel.utils.SkyblockGate.isFakepixelDungeon()) return;
        DungeonRewardAnalyzer analyzer = DungeonRewardAnalyzer.getInstance();
        if (analyzer == null || analyzer.getLastDrops().isEmpty()) return;

        List<DungeonDropData> drops = analyzer.getLastDrops();
        ScaledResolution sr = new ScaledResolution(MC);
        int panelWidth = 220;
        int x = sr.getScaledWidth() - panelWidth - 10;
        int y = 10;

        boolean animate = Config.feature.dungeonSystems.rewardAnimation && analyzer.getAnalysisStartedAt() > 0;
        long elapsed = Math.max(0L, System.currentTimeMillis() - analyzer.getAnalysisStartedAt());
        long duration = Math.max(500L, Math.round(Config.feature.dungeonSystems.rewardAnimationTime * 1000f));
        float progress = Math.min(1f, elapsed / (float) duration);
        float eased = 1f - (float) Math.pow(1f - progress, 3);

        int height = animate ? 88 : 38;
        Gui.drawRect(x - 5, y - 4, x + panelWidth, y + height, 0x99000000);
        MC.fontRendererObj.drawStringWithShadow("§eDungeon Rewards", x, y, 0xFFFFFFFF);

        if (animate) {
            drawAnimation(drops, x + 10, y + 20, progress, eased);
        }
    }

    private void drawAnimation(List<DungeonDropData> drops, int x, int y, float progress, float eased) {
        int count = drops.size();
        if (count == 0) return;
        int center = x + 55;
        int spread = 28;
        int selected = Math.min(count - 1, Math.round(eased * (count - 1)));

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        try {
            int from = Math.max(0, selected - 2);
            int to = Math.min(count - 1, selected + 2);
            for (int i = from; i <= to; i++) {
                float offset = (i - selected) * spread - (1f - eased) * spread * 2f;
                int drawX = Math.round(center + offset);
                ItemStackHolder.render(drops.get(i), drawX, y + 2);
            }
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }

        DungeonDropData selectedDrop = drops.get(selected);
        if (Config.feature.dungeonSystems.rewardAnimationShowItemNames) {
            String name = selectedDrop.getDisplayName();
            if (name != null && !name.isEmpty()) {
                float scale = Math.max(0.5f, Math.min(3f, Config.feature.dungeonSystems.rewardAnimationTextScale));
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.scale(scale, scale, scale);
                    float inv = 1f / scale;
                    int width = MC.fontRendererObj.getStringWidth(name);
                    MC.fontRendererObj.drawStringWithShadow(name, (x + 55) * inv - width / 2f, (y + 38) * inv, 0xFFFFFFFF);
                } finally {
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    private static final class ItemStackHolder {
        static void render(DungeonDropData drop, int x, int y) {
            if (drop == null || drop.getStack() == null) return;
            RENDER_ITEM.renderItemAndEffectIntoGUI(drop.getStack(), x, y);
        }
    }
}
