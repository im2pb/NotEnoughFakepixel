package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.features.skyblock.qol.HidePlayersNearNpcs;
import com.nef.notenoughfakepixel.features.skyblock.qol.TeleportCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @ModifyArg(method = "findCrosshairTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"),
            index = 4)
    private Predicate<Entity> nef$skipHiddenPlayers(Predicate<Entity> targetable) {
        return entity -> targetable.test(entity) && !HidePlayersNearNpcs.INSTANCE.hidden(entity);
    }

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private double nef$etherwarpFov(double fov, Camera camera, float tickDelta, boolean changingFov) {
        return changingFov ? TeleportCamera.INSTANCE.zoomFov(camera, tickDelta, fov) : fov;
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void nef$skipHurtCamera(CallbackInfo callback) {
        if (ClientConfig.configuration().qol.qolNoHurtCam) callback.cancel();
    }
}
