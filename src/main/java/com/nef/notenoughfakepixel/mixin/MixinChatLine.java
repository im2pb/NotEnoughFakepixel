/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.chat.ChatLineAccess;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChatLine.class)
public class MixinChatLine implements ChatLineAccess {

    @Unique
    private IChatComponent nef$fullMessage;

    @Override
    public IChatComponent nef$getFullMessage() {
        return nef$fullMessage;
    }

    @Override
    public void nef$setFullMessage(IChatComponent message) {
        this.nef$fullMessage = message;
    }
}
