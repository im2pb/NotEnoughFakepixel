package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nef.notenoughfakepixel.config.ClientConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
abstract class ClientPlayerEntityMixin {
    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"), require = 2)
    private boolean nef$alwaysSprint(boolean pressed) {
        return pressed || ClientConfig.configuration().misc.qolAlwaysSprint;
    }
}
