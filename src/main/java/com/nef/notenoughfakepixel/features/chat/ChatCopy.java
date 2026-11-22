/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.chat;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

@RegisterEvents
public class ChatCopy {

    private static final long FLASH_MS = 350L;
    private static long copiedAt;

    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        
        if (!(event.gui instanceof GuiChat)) return;
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return;
        if (!Config.feature.misc.qolCopyChatMsg) return;
        if (!GuiScreen.isShiftKeyDown()) return; 

        GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
        if (!(chat instanceof ChatCopyAccess)) return;

        int mouseX = Mouse.getX();
        int mouseY = Mouse.getY();
        ChatLine line = ((ChatCopyAccess) chat).nef$getChatLineAt(mouseX, mouseY);
        if (line == null) return;

        boolean withColors = GuiScreen.isCtrlKeyDown();

        IChatComponent message = line instanceof ChatLineAccess ? ((ChatLineAccess) line).nef$getFullMessage() : null;
        if (message == null) message = line.getChatComponent(); 

        String text = withColors ? withColorCodes(message) : withoutColorCodes(message);
        if (text.isEmpty()) return;

        GuiScreen.setClipboardString(text);
        copiedAt = System.currentTimeMillis();
        
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) return;
        if (!Config.feature.misc.qolCopyChatMsg || !Config.feature.misc.qolCopyChatIcon) return;
        if (!GuiScreen.isShiftKeyDown()) return; 

        GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
        if (!(chat instanceof ChatCopyAccess)) return;

        int row = ((ChatCopyAccess) chat).nef$getVisibleRowAt(Mouse.getX(), Mouse.getY());
        if (row < 0) return;

        ChatCopyAccess access = (ChatCopyAccess) chat;
        int firstRow = access.nef$getMessageFirstRow(row);
        int lastRow = access.nef$getMessageLastRow(row);

        float scale = chat.getChatScale();
        int left = 2;
        int right = Math.round(2 + scale * (MathHelper.ceiling_float_int(chat.getChatWidth() / scale) + 4));
        int top = Math.round(event.gui.height - 28 - scale * (lastRow + 1) * 9);
        int bottom = Math.round(event.gui.height - 28 - scale * firstRow * 9);

        boolean flash = System.currentTimeMillis() - copiedAt < FLASH_MS;
        Gui.drawRect(left, top, right, bottom, flash ? 0x5055FF55 : 0x30FFFFFF);

        int iconBottom = Math.round(event.gui.height - 28 - scale * lastRow * 9);
        if (iconBottom - top >= 8) { 
            int color = GuiScreen.isCtrlKeyDown() ? 0xFFFFAA00 : 0xFFFFFFFF;
            int x = right - 11;
            int y = top + (iconBottom - top - 9) / 2;
            
            Gui.drawRect(x, y, x + 5, y + 1, color);
            Gui.drawRect(x, y, x + 1, y + 6, color);
            
            Gui.drawRect(x + 2, y + 2, x + 8, y + 9, 0xFF202020);
            Gui.drawRect(x + 2, y + 2, x + 8, y + 3, color);
            Gui.drawRect(x + 2, y + 8, x + 8, y + 9, color);
            Gui.drawRect(x + 2, y + 2, x + 3, y + 9, color);
            Gui.drawRect(x + 7, y + 2, x + 8, y + 9, color);
            
            Gui.drawRect(x + 4, y + 4, x + 6, y + 5, color);
            Gui.drawRect(x + 4, y + 6, x + 6, y + 7, color);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F); 
    }

    private static String withoutColorCodes(IChatComponent message) {
        return StringUtils.stripControlCodes(message.getUnformattedText()).trim();
    }

    private static String withColorCodes(IChatComponent message) {
        String text = message.getFormattedText();
        while (text.endsWith("§r")) text = text.substring(0, text.length() - 2);
        return text.trim();
    }
}
