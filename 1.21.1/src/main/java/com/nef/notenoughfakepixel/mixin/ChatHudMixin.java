package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.features.skyblock.qol.CopyChatMessage;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChatHud.class)
abstract class ChatHudMixin {
    @ModifyExpressionValue(method = "addVisibleMessage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHudLine;content()Lnet/minecraft/text/Text;"))
    private Text nef$addCopyButton(Text message) {
        var settings = ClientConfig.configuration().misc;
        return settings.qolCopyChatMsg ? CopyChatMessage.withCopyButton(message, settings.copyChatString) : message;
    }
}
