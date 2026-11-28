package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.QualityOfLife.ItemAnimation;
import com.nef.notenoughfakepixel.features.skyblock.qol.SwordOffhandShield;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
abstract class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER))
    private void nef$positionHeldItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand,
                                      float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices,
                                      VertexConsumerProvider vertices, int light, CallbackInfo callback) {
        if (stack.isEmpty() || stack.isOf(Items.FILLED_MAP)) return;
        var settings = ClientConfig.configuration().qol.itemAnimation;
        if (!settings.customAnimations) return;

        // The drinking mode applies its own position offset while this hand is in use.
        if (stack.getUseAction() == UseAction.DRINK && player.isUsingItem()
                && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) return;

        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        nef$offsetHeldItem(matrices, arm, settings);
    }

    // These translations are the idle swing offsets for crossbows and other held items.
    @WrapOperation(method = "renderFirstPersonItem", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 3),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 12)
    }, require = 2)
    private void nef$scaleSwingOffset(MatrixStack matrices, float x, float y, float z, Operation<Void> original) {
        var settings = ClientConfig.configuration().qol.itemAnimation;
        float scale = settings.customAnimations && settings.doesScaleSwing ? nef$itemScale(settings) : 1.0F;
        original.call(matrices, x * scale, y * scale, z * scale);
    }

    @Unique
    private static void nef$offsetHeldItem(MatrixStack matrices, Arm arm, ItemAnimation settings) {
        float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
        matrices.translate(side * 0.56F * settings.customX, 0.52F * settings.customY, -0.72F * settings.customZ);
    }

    @Unique
    private static float nef$itemScale(ItemAnimation settings) {
        return (float) Math.exp(settings.customSize);
    }

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
            nef$offsetHeldItem(matrices, arm, settings);
        }
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), cancellable = true)
    private void nef$adjustItemRender(LivingEntity player, ItemStack stack, ModelTransformationMode mode,
                                      boolean leftHanded, MatrixStack matrices, VertexConsumerProvider vertices,
                                      int light, CallbackInfo callback) {
        boolean handModel = mode.isFirstPerson() || mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                || mode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
        if (handModel && SwordOffhandShield.hidden(player, stack)
                && player.getMainArm() != (leftHanded ? Arm.LEFT : Arm.RIGHT)) {
            callback.cancel();
            return;
        }
        if (!mode.isFirstPerson() || stack.isEmpty()) return;
        var settings = ClientConfig.configuration().qol.itemAnimation;
        if (!settings.customAnimations) return;

        float side = leftHanded ? -1.0F : 1.0F;
        if (settings.customPitch != 0.0F) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(settings.customPitch));
        if (settings.customYaw != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * settings.customYaw));
        if (settings.customRoll != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * settings.customRoll));
        if (settings.customSize != 0.0F) {
            float scale = nef$itemScale(settings);
            matrices.scale(scale, scale, scale);
        }
    }
}
