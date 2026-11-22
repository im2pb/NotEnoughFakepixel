/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.enchanting;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.TablistParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

@RegisterEvents
public class PreventMissclicks {

    long lastTimeClicked = System.currentTimeMillis();
    float cooldownClicks = 500;

    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!Mouse.getEventButtonState()) return;
        if (EnchantingSolvers.currentSolverType != EnchantingSolvers.SolverTypes.CHRONOMATRON && EnchantingSolvers.currentSolverType != EnchantingSolvers.SolverTypes.ULTRASEQUENCER)
            return;
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiChest))
            return; 
        GuiChest chestGui = (GuiChest) Minecraft.getMinecraft().currentScreen;
        if (chestGui.getSlotUnderMouse() == null) return;
        if (Config.feature.experimentation.experimentationChronomatronSolver && System.currentTimeMillis() - lastTimeClicked < cooldownClicks && EnchantingSolvers.currentSolverType == EnchantingSolvers.SolverTypes.CHRONOMATRON && EnchantingSolvers.resolving) {
            event.setCanceled(true);
            return;
        }
        lastTimeClicked = System.currentTimeMillis();
        int slotIndex = chestGui.getSlotUnderMouse().getSlotIndex();
        if (Config.feature.experimentation.experimentationChronomatronSolver && EnchantingSolvers.currentSolverType == EnchantingSolvers.SolverTypes.CHRONOMATRON && EnchantingSolvers.resolving && !EnchantingSolvers.chronomatronOrder.isEmpty()) {
            if (slotIndex == EnchantingSolvers.chronomatronOrder.get(0) ||
                    slotIndex == EnchantingSolvers.chronomatronOrder.get(0) + 9 ||
                    (slotIndex == EnchantingSolvers.chronomatronOrder.get(0) + 18 && !TablistParser.currentOpenChestName.contains("Transcendent") && !TablistParser.currentOpenChestName.contains("Metaphysical"))) {
                return; 
            }
            if (Config.feature.experimentation.experimentationPreventMissclicks) event.setCanceled(true);
        } else if (Config.feature.experimentation.experimentationUltraSequencerSolver && EnchantingSolvers.currentSolverType == EnchantingSolvers.SolverTypes.ULTRASEQUENCER && EnchantingSolvers.resolving) {
            for (EnchantingSolvers.UltrasequencerSlot slot : EnchantingSolvers.ultrasequencerSlots) {
                
                ItemStack itemInSlot = chestGui.inventorySlots.getInventory().get(slotIndex);
                if (slot.slot == chestGui.getSlotUnderMouse() && EnchantingSolvers.slotToClickUltrasequencer == slot.quantity) {
                    if (EnchantingSolvers.ultrasequencerSlots.size() == EnchantingSolvers.slotToClickUltrasequencer)
                        EnchantingSolvers.roundUltraSequencerSolver++;
                    EnchantingSolvers.slotToClickUltrasequencer++;
                    return;
                }
                if (itemInSlot == null) continue;
                if (itemInSlot.getItem() == Items.dye) continue;
            }
            if (Config.feature.experimentation.experimentationPreventMissclicks)
                event.setCanceled(true); 
        }
    }
}
