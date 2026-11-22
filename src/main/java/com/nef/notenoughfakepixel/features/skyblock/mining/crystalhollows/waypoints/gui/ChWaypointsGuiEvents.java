/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows.waypoints.gui;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.config.gui.core.config.KeybindHelper;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class ChWaypointsGuiEvents {

    private final Minecraft mc = Minecraft.getMinecraft();

    private static long lastExecuted = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        if (mc.currentScreen != null || mc.thePlayer == null) return;

        handleWaypointGUI(Config.feature.mining.chWaypointsGUI, GuiWaypointManager::new);
        handleWaypointGUI(Config.feature.mining.chNewWaypointGUI, GuiWaypointCreate::new);
    }

    private void handleWaypointGUI(int key, java.util.function.Supplier<GuiScreen> screen) {
        
        if (lastExecuted == 0) lastExecuted = System.currentTimeMillis();
        
        if (mc.currentScreen != null) return;
        if (!KeybindHelper.isKeyDown(key)) return;
        if (System.currentTimeMillis() - lastExecuted < 200) return;

        if (!SkyblockData.getCurrentLocation().equals(Location.CRYSTAL_HOLLOWS)) {
            mc.thePlayer.addChatMessage(new ChatComponentText("§cYou can only use waypoints in Crystal Hollows"));
            lastExecuted = System.currentTimeMillis();
            return;
        }
        mc.displayGuiScreen(screen.get());
    }

}
