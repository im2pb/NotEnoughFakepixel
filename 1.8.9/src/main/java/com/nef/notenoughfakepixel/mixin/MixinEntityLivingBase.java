package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.gui.Config;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends MixinEntity {

    @Shadow
    public abstract boolean isPotionActive(Potion potionIn);

    @Shadow
    public abstract PotionEffect getActivePotionEffect(Potion potionIn);

    @Inject(method = "getArmSwingAnimationEnd()I", at = @At("HEAD"), cancellable = true)
    public void adjustSwingLength(CallbackInfoReturnable<Integer> cir) {
        if (!Config.feature.qol.itemAnimation.customAnimations) return;
        int length = Config.feature.qol.itemAnimation.ignoreHaste ? 6 : this.isPotionActive(Potion.digSpeed) ?
                6 - (1 + this.getActivePotionEffect(Potion.digSpeed).getAmplifier()) :
                (this.isPotionActive(Potion.digSlowdown) ?
                        6 + (1 + this.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) * 2 :
                        6);
        cir.setReturnValue(Math.max((int) (length * Math.exp(-Config.feature.qol.itemAnimation.customSpeed)), 1));
    }

    @Inject(method = "isChild", at = @At("HEAD"), cancellable = true)
    private void setChildState(CallbackInfoReturnable<Boolean> cir) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (Config.feature.qol.playerSizeSettings.smolPeople && self instanceof EntityPlayer) {
            cir.setReturnValue(true);
        }
    }

}
