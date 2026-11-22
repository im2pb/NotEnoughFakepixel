/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.performance.PerformanceManager;
import com.nef.notenoughfakepixel.features.skyblock.qol.PlayerVisibility;
import net.minecraft.client.renderer.entity.RenderManager;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.LeapingSwordCobwebs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public abstract class MixinRenderManager {

    @Inject(method = "renderEntityStatic(Lnet/minecraft/entity/Entity;FZ)Z", at = @At("HEAD"), cancellable = true)
    private void nef$performanceCull(Entity entity, float partialTicks, boolean hideDebugBox, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EntityFallingBlock && LeapingSwordCobwebs.shouldSuppressEntity(entity)) {
            cir.setReturnValue(false);
            return;
        }
        if (PlayerVisibility.shouldHidePlayer(entity)) {
            cir.setReturnValue(false);
            return;
        }
        if (PerformanceManager.shouldCullEntity(entity)) {
            cir.setReturnValue(false);
        }
    }
}
