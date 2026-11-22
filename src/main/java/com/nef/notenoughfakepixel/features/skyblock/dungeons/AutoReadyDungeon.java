/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.TablistParser;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemSkull;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class AutoReadyDungeon {

    private static boolean clicked = false;
    private static int timesClicked = 0;

    private static final Pattern nickedNamePattern = Pattern.compile("§r§aYou have successfully changed your nickname to (?<rank>(.+) |§r§7)(?<name>(.+))§r§a!§r");

    @SubscribeEvent()
    public void onGuiOpen(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (clicked) return;
        if (!Config.feature.dungeons.dungeonsAutoReady) return;
        if (!SkyblockData.getCurrentLocation().isDungeon()) return;
        if (event.gui == null) return;
        if (!(event.gui instanceof GuiChest)) return;

        GuiChest chest = (GuiChest) event.gui;
        Container container = chest.inventorySlots;

        String chestName = TablistParser.currentOpenChestName;
        if (chestName == null || chestName.isEmpty()) return;

        if (chestName.startsWith("Catacombs -")) {
            if (timesClicked >= 7) {
                clicked = true;
                return;
            }
            ContainerChest containerChest = (ContainerChest) container;
            
            for (Slot slot : containerChest.inventorySlots) {
                
                if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
                ItemStack item = slot.getStack();
                
                if (item == null) continue;
                
                if (item.getItem() instanceof ItemSkull) {
                    String itemName = item.getDisplayName();
                    
                    if (itemName.contains(Minecraft.getMinecraft().thePlayer.getName()) ||
                            itemName.contains(Config.feature.dungeons.dungeonsAutoReadyName)) {

                        ItemStack itemReady = containerChest.getSlot(slot.getSlotIndex() + 9).getStack();
                        
                        if (itemReady == null) continue;
                        
                        if (itemReady.getMetadata() == 13) {
                            clicked = true;
                            return;
                        }
                        
                        boolean isNotReady = (itemReady.getMetadata() == 14);
                        
                        if (isNotReady) {
                            timesClicked++;
                            Minecraft.getMinecraft().playerController.windowClick(chest.inventorySlots.windowId, slot.getSlotIndex() + 9, 0, 0, Minecraft.getMinecraft().thePlayer);
                            Minecraft.getMinecraft().playerController.windowClick(chest.inventorySlots.windowId, slot.getSlotIndex() + 9, 0, 0, Minecraft.getMinecraft().thePlayer);
                        }
                    }
                }

            }
        }
    }

    @SubscribeEvent()
    public void onWorldUnload(WorldEvent.Unload event) {
        clicked = false;
        timesClicked = 0;
    }

    @SubscribeEvent
    public void onChatRecieve(ClientChatReceivedEvent e) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        if (Minecraft.getMinecraft().theWorld == null) return;
        if (SkyblockData.getCurrentLocation() != Location.NONE) return;

        Matcher matcher = nickedNamePattern.matcher(e.message.getFormattedText());
        if (matcher.matches()) {
            Config.feature.dungeons.dungeonsAutoReadyName = matcher.group("name");
        }

        if (e.message.getFormattedText().startsWith("§r§aYou have successfully reset your nickname!")) {
            Config.feature.dungeons.dungeonsAutoReadyName = "example name";
        }
    }

}
