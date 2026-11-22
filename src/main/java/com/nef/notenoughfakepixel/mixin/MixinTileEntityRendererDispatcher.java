/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.performance.PerformanceManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcher {

    @Inject(method = "renderTileEntityAt(Lnet/minecraft/tileentity/TileEntity;DDDF)V", at = @At("HEAD"), cancellable = true)
    private void nef$performanceCullTileEntity(TileEntity tileEntity, double x, double y, double z, float partialTicks, CallbackInfo ci) {
        if (PerformanceManager.shouldCullTileEntity(tileEntity)) {
            ci.cancel();
        }
    }
}
