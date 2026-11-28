package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.StarredMobDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
    @Inject(method = {"renderWeather", "tickRainSplashing"}, at = @At("HEAD"), cancellable = true, require = 2)
    private void nef$skipRain(CallbackInfo callback) {
        if (ClientConfig.configuration().qol.visualTweaks.qolDisableRain) callback.cancel();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    private boolean nef$outlineStarredMob(MinecraftClient client, Entity entity, Operation<Boolean> original) {
        return original.call(client, entity) || StarredMobDisplay.outlineColor(entity) != null;
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    private int nef$starredMobOutlineColor(Entity entity, Operation<Integer> original) {
        Integer color = StarredMobDisplay.outlineColor(entity);
        return color != null && !MinecraftClient.getInstance().hasOutline(entity) ? color : original.call(entity);
    }
}
