/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.crimson;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.config.gui.core.config.Position;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class AshfangOverlay {

    private final Position position;

    public AshfangOverlay() {
        this.position = Config.feature.crimson.ashfangOverlayPos;
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!shouldShow()) return;
        render();
    }

    public void render() {
        if (!shouldShow()) return;

        List<String> lines = new ArrayList<>();
        getLines(lines, false);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int x = position.getAbsX(sr, 100); 
        int y = position.getAbsY(sr, 20);  

        for (String line : lines) {
            Minecraft.getMinecraft().fontRendererObj.drawString(line, x, y, 0xFFFFFF); 
            y += 10;
        }
    }

    public void renderDummy() {
        List<String> lines = new ArrayList<>();
        getLines(lines, true);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int x = position.getAbsX(sr, 100);
        int y = position.getAbsY(sr, 20);

        for (int i = 0; i < lines.size(); i++) {
            Minecraft.getMinecraft().fontRendererObj.drawString(
                    lines.get(i), x, y + i * 10, 0xFFFFFF
            );
        }
    }

    private void getLines(List<String> lines, boolean example) {
        if (example) {
            lines.add("\u00a77Ashfang HP: \u00a7a50.0M");
            lines.add("\u00a77Blazing souls: \u00a7r0 / 25");
            return;
        }
        if (Crimson.checkEssentials()) return;
        if (Config.feature.crimson.crimsonAshfangOverlay) {
            float hp = AshfangHelper.getAshfangHP();
            int hitsNeeded = AshfangHelper.getHitsNeeded();
            String hpText = hp < 0.0f ? "\u00a77N/A" : formatAshfangHP(hp);
            String hitsText = hitsNeeded < 0 ? "--" : Integer.toString(hitsNeeded);
            lines.add("\u00a77Ashfang HP: \u00a7r" + hpText);
            lines.add("\u00a77Blazing souls: \u00a7r" + AshfangHelper.getBlazingSoulsCounter() + " / " + hitsText);
        }
    }

    private boolean shouldShow() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Config.feature.crimson.crimsonAshfangOverlay || mc.thePlayer == null || mc.theWorld == null
                || Crimson.checkEssentials()) return false;
        return Crimson.checkAshfangArea(
                net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posX),
                net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posY),
                net.minecraft.util.MathHelper.floor_double(mc.thePlayer.posZ));
    }

    private static String formatAshfangHP(double hp) {
        double percentage = hp / 50_000_000;
        String prefix = percentage > 0.5 ? "§a" : percentage > 0.1 ? "§e" : "§c";
        return prefix + coolFormat(hp, 0);
    }

    private static final char[] SUFFIXES = {'k', 'M'};

    private static String coolFormat(double n, int iteration) {
        double d = ((double) (long) n / 100) / 10.0;
        boolean isRound = (d * 10) % 10 == 0;
        return d < 1000 ?
                (isRound || d > 9.99 ? (int) d * 10 / 10 : d) + "" + SUFFIXES[iteration] :
                coolFormat(d, iteration + 1);
    }
}
