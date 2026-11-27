package com.nef.notenoughfakepixel.mixin;

import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingBobberEntity.class)
public interface FishingBobberAccessor {
    @Accessor("caughtFish")
    boolean nef$caughtFish();
}
