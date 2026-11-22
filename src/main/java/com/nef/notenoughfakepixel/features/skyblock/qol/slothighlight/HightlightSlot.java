/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.InventoryUtils;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;

public abstract class HightlightSlot implements GatedListener {

    public abstract String getLoreLine();

    public abstract String getName();

    public abstract String getContainerName();

    public abstract boolean getConfigOption();

    public boolean onlyInSkyblock() {
        return true;
    }

    public Color getHighlightColor() {
        return new Color(55, 255, 55);
    }

    public boolean highlightOnlyFirst() {
        return false;
    }

    @Override
    public boolean isListenerActive() {
        return getConfigOption() && (!onlyInSkyblock() || SkyblockData.getCurrentGamemode().isSkyblock());
    }

    @SubscribeEvent
    public void onOpen(GuiScreenEvent.BackgroundDrawnEvent e) {
        if (!getConfigOption()
                || (onlyInSkyblock() && !SkyblockData.getCurrentGamemode().isSkyblock())
                || !(e.gui instanceof GuiChest)
                || !checkForEssentials()) return;

        GuiChest chest = (GuiChest) e.gui;
        Container container = chest.inventorySlots;

        if (!(container instanceof ContainerChest)) return;
        String title = ((ContainerChest) container).getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!title.startsWith(getContainerName())) return;

        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;

            ItemStack item = slot.getStack();
            if (item == null || item.getItem() == null) continue;

            boolean matchesLore = getLoreLine() != null && !getLoreLine().isEmpty() && ItemUtils.getLoreLine(item, getLoreLine()) != null;
            boolean matchesName = getName() != null && !getName().isEmpty() && item.getDisplayName().contains(getName());

            if (matchesLore || matchesName) {
                highlightSlot(slot, chest);
                if (highlightOnlyFirst()) return;
            }
        }
    }

    private boolean checkForEssentials() {
        return (getLoreLine() != null && !getLoreLine().isEmpty())
                || (getName() != null && !getName().isEmpty())
                && getContainerName() != null && !getContainerName().isEmpty();
    }

    private void highlightSlot(Slot slot, GuiChest chest) {
        InventoryUtils.highlightSlot(slot, chest, getHighlightColor());
    }
}
