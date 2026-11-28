package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.features.skyblock.qol.CombatVisuals;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LightningEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void nef$skipHiddenEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                                      MatrixStack matrices, VertexConsumerProvider vertices, int light, CallbackInfo callback) {
        if ((entity instanceof FallingBlockEntity && ClientConfig.configuration().qol.visualTweaks.qolHideFallingBlocks)
                || (entity instanceof LightningEntity && CombatVisuals.hideLightning(entity.getWorld()))) {
            callback.cancel();
        }
    }
}
