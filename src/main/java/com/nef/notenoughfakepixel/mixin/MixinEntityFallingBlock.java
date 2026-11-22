/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.dungeons.LeapingSwordCobwebs;
import net.minecraft.entity.item.EntityFallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityFallingBlock.class)
public abstract class MixinEntityFallingBlock {

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void nef$disableLeapingWebInteraction(CallbackInfoReturnable<Boolean> cir) {
        if (LeapingSwordCobwebs.shouldSuppressEntity((EntityFallingBlock) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
