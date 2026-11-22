/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.events.SpawnedParticleEvent;
import com.nef.notenoughfakepixel.features.skyblock.qol.DisableHyperionExplosions;
import com.nef.notenoughfakepixel.features.performance.PerformanceManager;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorld {

    @Inject(method = "spawnParticle(IZDDDDDD[I)V", at = @At("HEAD"), cancellable = true)
    public void spawnParticle(
            int particleID, boolean p_175720_2_, double xCood, double yCoord, double zCoord,
            double xOffset, double yOffset, double zOffset, int[] p_175720_15_, CallbackInfo ci
    ) {
        if (Config.feature.qol.qolDisableHyperionExplosions &&
            System.currentTimeMillis() - DisableHyperionExplosions.lastClickedHyperion < 500) {

            if ( particleID == 1 ) ci.cancel();
        }
    }

    @Inject(
            method = "spawnParticle(Lnet/minecraft/util/EnumParticleTypes;ZDDDDDD[I)V",
            at = @At("HEAD"), cancellable = true
    )
    private void nef$onSpawnParticle(
            EnumParticleTypes type, boolean longDistance,
            double x, double y, double z,
            double xOffset, double yOffset, double zOffset,
            int[] params, CallbackInfo ci
    ) {
        MinecraftForge.EVENT_BUS.post(new SpawnedParticleEvent(
                type, longDistance, x, y, z, xOffset, yOffset, zOffset, params
        ));
        if (PerformanceManager.shouldSuppressParticle(type)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateWeather", at = @At("HEAD"), cancellable = true)
    private void disableRain(CallbackInfo ci) {
        
        if (Config.feature.qol.qolDisableRain) {
            
            World world = (World) (Object) this;

            WorldInfo worldInfo = world.getWorldInfo();

            worldInfo.setRainTime(0);         
            worldInfo.setThunderTime(0);     
            worldInfo.setRaining(false);     
            worldInfo.setThundering(false);  

            ci.cancel();
        }
    }

    @Unique
    private static boolean nef$weatherDisabled() {
        if (Config.feature == null) return false;
        return Config.feature.qol != null && Config.feature.qol.qolDisableRain;
    }

    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true)
    private void nef$noRainStrength(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (nef$weatherDisabled()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderStrength", at = @At("HEAD"), cancellable = true)
    private void nef$noThunderStrength(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (nef$weatherDisabled()) {
            cir.setReturnValue(0.0F);
        }
    }
}
