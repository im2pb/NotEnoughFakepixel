package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.ElytraFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    @WrapWithCondition(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V"))
    private boolean nef$showArmorLayer(FeatureRenderer<?, ?> feature, MatrixStack matrices, VertexConsumerProvider vertices,
                                       int light, Entity entity, float limbAngle, float limbDistance, float tickDelta,
                                       float age, float headYaw, float headPitch) {
        return !(entity instanceof PlayerEntity && SkyblockData.isSkyblock()
                && ClientConfig.configuration().qol.visualTweaks.qolHidePlayerArmor
                && (feature instanceof ArmorFeatureRenderer<?, ?, ?>
                    || feature instanceof HeadFeatureRenderer<?, ?> || feature instanceof ElytraFeatureRenderer<?, ?>));
    }

    @ModifyReturnValue(method = "isVisible", at = @At("RETURN"))
    private boolean nef$revealGhost(boolean visible, LivingEntity entity) {
        return visible || (entity instanceof CreeperEntity && SkyblockData.isSkyblock()
                && SkyblockData.currentLocation() == Location.DWARVEN
                && ClientConfig.configuration().mining.dwarvenMines.miningShowGhosts);
    }
}
