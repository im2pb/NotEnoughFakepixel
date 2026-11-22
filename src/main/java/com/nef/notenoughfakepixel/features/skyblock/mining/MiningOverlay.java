/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.overlays.Overlay;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.features.skyblock.mining.service.MiningOverlayDataController;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

@RegisterEvents
public class MiningOverlay extends Overlay {

    private static final int LINE_HEIGHT = 11;
    private static final int MINIMUM_WIDTH = 20;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final MiningOverlayDataController dataController = MiningOverlayDataController.getInstance();

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!shouldShow()) return;

        draw(
                Config.feature.mining.miningOverlayOffsetX,
                Config.feature.mining.miningOverlayOffsetY,
                Config.feature.mining.miningOverlayScale,
                Config.feature.mining.miningOverlayBackgroundColor
        );
    }

    @Override
    public boolean shouldShow() {
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return false;
        if (!SkyblockData.getCurrentLocation().equals(Location.DWARVEN) && !SkyblockData.getCurrentLocation().equals(Location.CRYSTAL_HOLLOWS))  return false;
        if (Minecraft.getMinecraft().gameSettings.keyBindPlayerList.isKeyDown()) return false;
        if (Minecraft.getMinecraft().gameSettings.showDebugInfo) return false;
        if (Config.feature.mining.miningOverlayHideOnChat && mc.currentScreen instanceof GuiChat) return false;
        if ( mc.gameSettings.showDebugInfo ) return false;
        return Config.feature.mining.miningOverlay;
    }

    @Override
    public List<String> getLines() {
        return dataController.getLines();
    }

    @Override
    public float getWidth(float scale, List<String> lines) {
        float var = Math.max(getLongestLine(lines) * 5, MINIMUM_WIDTH);
        return var * scale;
    }

    @Override
    public float getHeight(float scale, List<String> lines) {
        return lines.size() * LINE_HEIGHT;
    }

}
