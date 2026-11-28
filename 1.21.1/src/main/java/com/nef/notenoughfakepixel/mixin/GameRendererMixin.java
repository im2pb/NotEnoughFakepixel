package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.features.skyblock.qol.TeleportCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private double nef$etherwarpFov(double fov, Camera camera, float tickDelta, boolean changingFov) {
        return changingFov ? TeleportCamera.INSTANCE.zoomFov(camera, tickDelta, fov) : fov;
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void nef$skipHurtCamera(CallbackInfo callback) {
        if (ClientConfig.configuration().qol.qolNoHurtCam) callback.cancel();
    }
}
