/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.NotEnoughFakepixel;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.utils.SoundUtils;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;

@Mixin(GuiChest.class)
public abstract class MixinGuiChest extends GuiContainer {
    private Boolean inInventory = false;
    private GuiTextField textField = null;

    @Shadow
    private IInventory lowerChestInventory;

    public MixinGuiChest(Container inventorySlotsIn) {
        super(inventorySlotsIn);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        nef$removeNullWidgets();
        try {
            super.drawScreen(mouseX, mouseY, partialTicks);
        } catch (NullPointerException e) {

            System.err.println("[NEF] NullPointerException while drawing " + this.getClass().getName()
                    + "; closing the screen instead of crashing.");
            e.printStackTrace();
            if (this.mc.thePlayer != null) {
                this.mc.thePlayer.closeScreen();
            } else {
                this.mc.displayGuiScreen(null);
            }
            return;
        }
        if (textField != null) {
            textField.drawTextBox();
        }
    }

    @Unique
    private void nef$removeNullWidgets() {
        if (this.buttonList != null) {
            for (int i = this.buttonList.size() - 1; i >= 0; i--) {
                if (this.buttonList.get(i) == null) this.buttonList.remove(i);
            }
        }
        if (this.labelList != null) {
            for (int i = this.labelList.size() - 1; i >= 0; i--) {
                if (this.labelList.get(i) == null) this.labelList.remove(i);
            }
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        String guiName = lowerChestInventory.getDisplayName().getUnformattedText();
        if (guiName.equals("Reforge Item") && Config.feature.qol.qolReforgeHelper) {
            inInventory = true;
            int xPos = guiLeft - 140;
            int yPos = guiTop + 80;
            textField = new GuiTextField(2, this.fontRendererObj, xPos, yPos, 120, 20);
            textField.setMaxStringLength(50);
            textField.setFocused(true);
            String lockedReforge = NotEnoughFakepixel.instance.getUtils().getLockedEnchantment();
            if (lockedReforge != null) {
                textField.setText(lockedReforge.toLowerCase());
            }
        } else {
            inInventory = false;
            textField = null;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (inInventory) {
            if (keyCode != this.mc.gameSettings.keyBindInventory.getKeyCode()) {
                super.keyTyped(typedChar, keyCode);
            }
            if (textField != null) {
                textField.textboxKeyTyped(typedChar, keyCode);
                NotEnoughFakepixel.instance.getUtils().setLockedEnchantment(textField.getText());
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void handleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType) {
        String lockedEnchantment = NotEnoughFakepixel.instance.getUtils().getLockedEnchantment();
        if (inInventory && lockedEnchantment != null && !lockedEnchantment.isEmpty()) {
            if (slotIn != null && slotIn.getHasStack() && slotIn.getSlotIndex() == 22) {
                Slot itemSlot = inventorySlots.getSlot(13);
                if (itemSlot != null && itemSlot.getHasStack()) {
                    ItemStack item = itemSlot.getStack();
                    if (item.hasDisplayName()) {
                        String[] nameParts = item.getDisplayName().split(" ");
                        if (nameParts.length > 2) {
                            String reforge = NotEnoughFakepixel.instance.getUtils().stripColor(nameParts[0]);
                            if (reforge.toLowerCase().contains(lockedEnchantment.toLowerCase())) {
                                SoundUtils.playGlobalSound("random.orb", 1, 0.1F);
                                return;
                            }
                        }
                    }
                }
            }
        }
        super.handleMouseClick(slotIn, slotId, clickedButton, clickType);
    }
}
