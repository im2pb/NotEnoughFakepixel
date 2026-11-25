package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows.waypoints.gui;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.utils.KeybindHelper;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
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
        handleWaypointGUI(Config.feature.mining.crystalHollowsMap.chWaypointsGUI, new GuiWaypointManager());
        handleWaypointGUI(Config.feature.mining.crystalHollowsMap.chNewWaypointGUI, new GuiWaypointCreate());
    }

    private void handleWaypointGUI(int key, GuiScreen guiScreen) {
        // ignore if Chat is open
        if (lastExecuted == 0) lastExecuted = System.currentTimeMillis();
        if (System.currentTimeMillis() - lastExecuted < 200) return;
        // If another screen is open, do nothing
        if (mc.currentScreen != null) return;

        if (KeybindHelper.isKeyDown(key)) {
            if (!SkyblockData.getCurrentLocation().equals(Location.CRYSTAL_HOLLOWS)) {
                mc.thePlayer.addChatMessage(new ChatComponentText("§cYou can only use waypoints in Crystal Hollows"));
                lastExecuted = System.currentTimeMillis();
                return;
            }
            if (mc.currentScreen == null) {
                mc.displayGuiScreen(guiScreen);
            }
        }
    }

}

