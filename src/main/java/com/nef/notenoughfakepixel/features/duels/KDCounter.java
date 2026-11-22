/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.duels;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Gamemode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class KDCounter implements GatedListener {

    @Override
    public boolean isListenerActive() {
        return SkyblockData.getCurrentGamemode() == Gamemode.DUELS;
    }

    private static int kills = 0;
    private static int deaths = 0;
    private String opponent = "";
    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (SkyblockData.getCurrentGamemode() != Gamemode.DUELS) return;
        String msg = e.message.getUnformattedText();

        if (msg.contains("Opponents: ")) {
            opponent = msg.replace("Opponents: ", "").trim();
        }

        String[] parts = msg.split(" ");
        if (parts.length > 0) {
            String firstWord = parts[0];
            if (opponent.equals(firstWord)) {
                kills++;
            } else if (mc.thePlayer.getName().equals(firstWord)) {
                deaths++;
            }
        }

        if (msg.contains("WINNER")) {
            kills = 0;
            deaths = 0;
            opponent = "";
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!Config.feature.duels.kdCounterEnabled) return;
        if (SkyblockData.getCurrentGamemode() != Gamemode.DUELS) return;

        draw(Config.feature.duels.kdCounterOffsetX, Config.feature.duels.kdCounterOffsetY, Config.feature.duels.kdCounterScale, false);
    }

    private void draw(float x, float y, float scale, boolean example) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, scale);

        String text = example ? "K/D: 3/2" : "K/D: " + kills + "/" + deaths;
        mc.fontRendererObj.drawString(text, 0, 0, -1);

        GlStateManager.popMatrix();
    }

    public void renderDummy() {
        draw(Config.feature.duels.kdCounterOffsetX, Config.feature.duels.kdCounterOffsetY, Config.feature.duels.kdCounterScale, true);
    }

    public float getWidth() {
        return 45 * Config.feature.duels.kdCounterScale; 
    }

    public float getHeight() {
        return 11 * Config.feature.duels.kdCounterScale; 
    }
}
