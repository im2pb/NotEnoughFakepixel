package com.nef.notenoughfakepixel.features.skyblock.enchanting;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class HideEnchantingTooltips {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onTooltipLow(ItemTooltipEvent event) {
        if (!Config.feature.experimentation.experimentationSolvers
                || !Config.feature.experimentation.experimentationHideTooltips
                || !SkyblockData.getCurrentGamemode().isSkyblock() || event.toolTip == null
                || !(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) return;

        GuiChest screen = (GuiChest) Minecraft.getMinecraft().currentScreen;
        ContainerChest chest = (ContainerChest) screen.inventorySlots;
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || slot.inventory != chest.getLowerChestInventory() || slot.getStack() != event.itemStack) return;

        String title = slot.inventory.getDisplayName().getUnformattedText();
        boolean active = (title.startsWith("Chronomatron (") && Config.feature.experimentation.experimentationChronomatronSolver)
                || (title.startsWith("Ultrasequencer (") && Config.feature.experimentation.experimentationUltraSequencerSolver);
        Item item = event.itemStack.getItem();
        if (active && item != Items.clock && item != Item.getItemFromBlock(Blocks.bookshelf)) event.toolTip.clear();
    }
}
