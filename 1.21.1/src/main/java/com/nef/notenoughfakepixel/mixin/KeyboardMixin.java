package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.qol.shortcuts.SkyblockShortcuts;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
abstract class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void nef$handleSkyblockShortcuts(long window, int key, int scanCode, int action, int modifiers,
                                            CallbackInfo callback) {
        if (window == client.getWindow().getHandle() && SkyblockShortcuts.handleKey(client, key, action)) {
            callback.cancel();
        }
    }
}
