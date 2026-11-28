package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.features.skyblock.qol.SwordOffhandShield;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
abstract class HeldItemRendererMixin {
    @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true)
    private void nef$adjustDrinkingAnimation(MatrixStack matrices, float tickDelta, Arm arm, ItemStack stack,
                                             PlayerEntity player, CallbackInfo callback) {
        if (stack.getUseAction() != UseAction.DRINK) return;
        var settings = ClientConfig.configuration().qol.itemAnimation;
        if (!settings.customAnimations) return;

        if (settings.drinkingSelector == 1) {
            float ticksLeft = player.getItemUseTimeLeft() - tickDelta + 1.0F;
            float remaining = ticksLeft / Math.max(1, stack.getMaxUseTime(player));
            if (remaining < 0.8F) {
                float bob = Math.abs(MathHelper.cos(ticksLeft * MathHelper.PI / 4.0F) * 0.1F);
                matrices.translate(0.0F, bob, 0.0F);
            }
            callback.cancel();
        } else if (settings.drinkingSelector == 2) {
            // Apply the legacy pivot offsets before vanilla animates this hand.
            float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
            matrices.translate(side * 0.56F * settings.customX, 0.52F * settings.customY, -0.72F * settings.customZ);
        }
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void nef$hideSwordShield(LivingEntity player, ItemStack stack, ModelTransformationMode mode,
                                    boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertices,
                                    int light, CallbackInfo callback) {
        boolean handModel = mode.isFirstPerson() || mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
        if (handModel && SwordOffhandShield.hidden(player, stack)
                && player.getMainArm() != (leftHanded ? Arm.LEFT : Arm.RIGHT)) {
            callback.cancel();
        }
    }
}
