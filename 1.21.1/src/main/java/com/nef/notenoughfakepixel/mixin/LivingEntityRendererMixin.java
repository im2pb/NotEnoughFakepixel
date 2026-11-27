package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    @ModifyReturnValue(method = "isVisible", at = @At("RETURN"))
    private boolean nef$revealGhost(boolean visible, LivingEntity entity) {
        return visible || (entity instanceof CreeperEntity && SkyblockData.isSkyblock()
                && SkyblockData.currentLocation() == Location.DWARVEN
                && ClientConfig.configuration().mining.dwarvenMines.miningShowGhosts);
    }
}
