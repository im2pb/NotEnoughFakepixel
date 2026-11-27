package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.ClientConfig;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
abstract class ParticleManagerMixin {
    @Inject(method = {"addBlockBreakParticles", "addBlockBreakingParticles"}, at = @At("HEAD"),
            cancellable = true, require = 2)
    private void nef$hideBlockBreakingParticles(CallbackInfo callback) {
        if (ClientConfig.configuration().qol.qolHideBlockBreakingParticles) callback.cancel();
    }
}
