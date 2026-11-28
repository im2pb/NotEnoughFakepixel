package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.qol.CombatVisuals;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "handleStatus", at = @At("HEAD"), cancellable = true)
    private void nef$skipEndermanTrail(byte status, CallbackInfo callback) {
        if (status == EntityStatuses.ADD_PORTAL_PARTICLES && CombatVisuals.hideEndermanTeleport((LivingEntity) (Object) this)) {
            callback.cancel();
        }
    }
}
