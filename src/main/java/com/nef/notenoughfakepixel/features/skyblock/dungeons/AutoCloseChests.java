/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class AutoCloseChests implements GatedListener {

    @Override
    public boolean isListenerActive() {
        return Config.feature.dungeons.dungeonsAutoCloseChests && SkyblockData.getCurrentLocation().isDungeon();
    }

    private static final String TARGET_CHEST_NAME = "Secret Chest";

    @SubscribeEvent
    public void onGuiBackgroundRender(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!Config.feature.dungeons.dungeonsAutoCloseChests) return; 
        if (!SkyblockData.getCurrentLocation().isDungeon()) return; 
        if (event.gui == null) return;

        if (event.gui instanceof GuiChest) {
            GuiChest chestGui = (GuiChest) event.gui;
            ContainerChest chestContainer = (ContainerChest) chestGui.inventorySlots;
            
            String chestName = chestContainer.getLowerChestInventory().getDisplayName().getUnformattedText();
            
            if (TARGET_CHEST_NAME.equals(chestName)) {
                Minecraft.getMinecraft().thePlayer.closeScreen(); 
            }
        }
    }

}
