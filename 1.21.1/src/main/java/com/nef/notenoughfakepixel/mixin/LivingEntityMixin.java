package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.features.skyblock.qol.CombatVisuals;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @ModifyReturnValue(method = "getHandSwingDuration", at = @At("RETURN"))
    private int nef$adjustHandSwingDuration(int vanillaDuration) {
        if (!((Object) this instanceof ClientPlayerEntity)) return vanillaDuration;
        var settings = ClientConfig.configuration().qol.itemAnimation;
        if (!settings.customAnimations) return vanillaDuration;

        int baseDuration = settings.ignoreHaste ? 6 : vanillaDuration;
        return Math.max(1, (int) (baseDuration * Math.exp(-settings.customSpeed)));
    }

    @Inject(method = "handleStatus", at = @At("HEAD"), cancellable = true)
    private void nef$skipEndermanTrail(byte status, CallbackInfo callback) {
        if (status == EntityStatuses.ADD_PORTAL_PARTICLES && CombatVisuals.hideEndermanTeleport((LivingEntity) (Object) this)) {
            callback.cancel();
        }
    }
}
