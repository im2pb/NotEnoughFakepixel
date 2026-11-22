/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.performance.PerformanceManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public abstract class MixinRender {

    @Inject(method = "renderShadow(Lnet/minecraft/entity/Entity;DDDFF)V", at = @At("HEAD"), cancellable = true)
    private void nef$disableEntityShadow(Entity entity, double x, double y, double z, float shadowAlpha, float partialTicks, CallbackInfo ci) {
        if (PerformanceManager.shouldSkipEntityShadows()) {
            ci.cancel();
        }
    }
}
