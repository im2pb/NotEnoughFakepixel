package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

@RegisterEvents
public class ScrollableTooltips {

    public static int scrollOffset = 0;
    private static ItemStack lastStack = null;

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!Config.feature.misc.qolScrollableTooltips) return;
        if (Minecraft.getMinecraft().currentScreen instanceof GuiChat) return;

        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) return;

        if (!isHoveringItem()) return;

        int scrollSpeed = 10;
        if (dWheel > 0) {
            scrollOffset -= scrollSpeed;
        } else {
            scrollOffset += scrollSpeed;
        }

        event.setCanceled(true);
    }

    private static boolean isHoveringItem() {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) return false;
        GuiContainer container = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        try {
            Field theSlotField;
            try {
                theSlotField = GuiContainer.class.getDeclaredField("theSlot");
            } catch (NoSuchFieldException e) {
                theSlotField = GuiContainer.class.getDeclaredField("field_147006_u");
            }
            theSlotField.setAccessible(true);
            Slot slot = (Slot) theSlotField.get(container);
            return slot != null && slot.getHasStack();
        } catch (Exception e) {
            return false;
        }
    }

    @SubscribeEvent
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!Config.feature.misc.qolScrollableTooltips) return;

        int key = Keyboard.getEventKey();

        if (Keyboard.getEventKeyState()) {
            int scrollSpeed = 5;

            if (key == Keyboard.KEY_UP) {
                scrollOffset += scrollSpeed;
            }
            else if (key == Keyboard.KEY_DOWN) {
                scrollOffset -= scrollSpeed;
            }
        }
    }

    public static void resetScroll() {
        scrollOffset = 0;
    }

    @SubscribeEvent
    public void onTooltipRender(ItemTooltipEvent event) {
        if (event.itemStack != lastStack) {
            scrollOffset = 0;
            lastStack = event.itemStack;
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        ScrollableTooltips.resetScroll();
    }


}
