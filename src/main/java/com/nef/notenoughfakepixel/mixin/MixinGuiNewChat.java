/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.chat.ChatCopyAccess;
import com.nef.notenoughfakepixel.features.chat.ChatLineAccess;
import com.nef.notenoughfakepixel.config.gui.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public abstract class MixinGuiNewChat implements ChatCopyAccess {

    @Shadow @Final private Minecraft mc;
    @Shadow @Final private List<ChatLine> drawnChatLines;
    @Shadow private int scrollPos;

    @Shadow public abstract boolean getChatOpen();
    @Shadow public abstract int getLineCount();
    @Shadow public abstract float getChatScale();
    @Shadow public abstract int getChatWidth();

    @Inject(method = "setChatLine", at = @At("TAIL"))
    private void nef$tagWrappedLines(IChatComponent message, int chatLineId, int updateCounter, boolean displayOnly, CallbackInfo ci) {
        for (int i = 0, n = this.drawnChatLines.size(); i < n; i++) {
            ChatLine line = this.drawnChatLines.get(i);
            if (!(line instanceof ChatLineAccess)) break;
            ChatLineAccess access = (ChatLineAccess) line;
            if (access.nef$getFullMessage() != null || line.getUpdatedCounter() != updateCounter) break;
            access.nef$setFullMessage(message);
        }
    }

    @ModifyConstant(method = "setChatLine", constant = @Constant(intValue = 100))
    private int nef$chatHistoryLimit(int vanillaLimit) {
        int configured = Config.feature.misc.qolChatHistoryLines;
        if (configured < vanillaLimit) return vanillaLimit;
        return Math.min(configured, 5000);
    }

    @Override
    public int nef$getVisibleRowAt(int mouseX, int mouseY) {
        if (!this.getChatOpen()) return -1;

        ScaledResolution resolution = new ScaledResolution(this.mc);
        int scaleFactor = resolution.getScaleFactor();
        float chatScale = this.getChatScale();
        int x = MathHelper.floor_float((float) (mouseX / scaleFactor - 3) / chatScale);
        int y = MathHelper.floor_float((float) (mouseY / scaleFactor - 27) / chatScale);
        if (x < 0 || y < 0) return -1;

        int visibleLines = Math.min(this.getLineCount(), this.drawnChatLines.size());
        int fontHeight = this.mc.fontRendererObj.FONT_HEIGHT;
        if (x > MathHelper.floor_float((float) this.getChatWidth() / chatScale)) return -1;
        if (y >= fontHeight * visibleLines + visibleLines) return -1;

        int row = y / fontHeight;
        int index = row + this.scrollPos;
        if (index < 0 || index >= this.drawnChatLines.size()) return -1;
        return row;
    }

    @Override
    public int nef$getMessageFirstRow(int row) {
        int index = row + this.scrollPos;
        IChatComponent message = this.nef$messageOf(index);
        if (message == null) return row;

        int first = row;
        while (first > 0 && this.nef$messageOf(first - 1 + this.scrollPos) == message) first--;
        return first;
    }

    @Override
    public int nef$getMessageLastRow(int row) {
        int index = row + this.scrollPos;
        IChatComponent message = this.nef$messageOf(index);
        if (message == null) return row;

        int lastVisible = Math.min(this.getLineCount(), this.drawnChatLines.size() - this.scrollPos) - 1;
        int last = row;
        while (last < lastVisible && this.nef$messageOf(last + 1 + this.scrollPos) == message) last++;
        return last;
    }

    @Unique
    private IChatComponent nef$messageOf(int index) {
        if (index < 0 || index >= this.drawnChatLines.size()) return null;
        ChatLine line = this.drawnChatLines.get(index);
        return line instanceof ChatLineAccess ? ((ChatLineAccess) line).nef$getFullMessage() : null;
    }

    @Override
    public ChatLine nef$getChatLineAt(int mouseX, int mouseY) {
        int row = this.nef$getVisibleRowAt(mouseX, mouseY);
        return row < 0 ? null : this.drawnChatLines.get(row + this.scrollPos);
    }
}
