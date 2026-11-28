package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.nef.notenoughfakepixel.features.skyblock.qol.CombatVisuals;
import net.minecraft.entity.LightningEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightningEntity.class)
abstract class LightningEntityMixin {
    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;setLightningTicksLeft(I)V"))
    private boolean nef$showLightningFlash(World world, int ticks) {
        return !CombatVisuals.hideLightning(world);
    }
}
