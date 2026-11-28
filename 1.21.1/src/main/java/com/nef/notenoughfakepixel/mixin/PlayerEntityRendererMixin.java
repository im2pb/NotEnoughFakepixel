package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.qol.SwordOffhandShield;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
abstract class PlayerEntityRendererMixin {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void nef$hideSwordShieldPose(AbstractClientPlayerEntity player, Hand hand,
                                               CallbackInfoReturnable<BipedEntityModel.ArmPose> callback) {
        if (hand == Hand.OFF_HAND && SwordOffhandShield.hidden(player, player.getOffHandStack())) {
            callback.setReturnValue(BipedEntityModel.ArmPose.EMPTY);
        }
    }
}
