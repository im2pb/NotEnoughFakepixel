package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.ClientConfig;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
abstract class LightmapTextureManagerMixin {
    @Shadow private boolean dirty;
    @Unique private boolean nef$fullbright;

    @Inject(method = "update", at = @At("HEAD"))
    private void nef$refreshLightmap(float tickDelta, CallbackInfo callback) {
        boolean enabled = ClientConfig.configuration().qol.visualTweaks.qolFullbright;
        if (enabled != nef$fullbright) {
            nef$fullbright = enabled;
            dirty = true;
        }
    }

    @ModifyArg(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/texture/NativeImage;setColor(III)V"), index = 2)
    private int nef$fullbrightPixel(int color) {
        return nef$fullbright ? 0xFFFFFFFF : color;
    }
}
