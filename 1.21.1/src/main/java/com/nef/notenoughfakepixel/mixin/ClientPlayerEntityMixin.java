package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.features.skyblock.slotlocking.SlotLocking;
import com.nef.notenoughfakepixel.utils.Logger;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
abstract class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void nef$protectItemDrop(boolean entireStack, CallbackInfoReturnable<Boolean> callback) {
        var player = (ClientPlayerEntity) (Object) this;
        if (SlotLocking.INSTANCE.blocksDrop(player.getInventory())) {
            callback.setReturnValue(false);
            Logger.feedbackError("Slot locking blocked the item drop.");
        }
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"), require = 2)
    private boolean nef$alwaysSprint(boolean pressed) {
        return pressed || ClientConfig.configuration().misc.qolAlwaysSprint;
    }
}
