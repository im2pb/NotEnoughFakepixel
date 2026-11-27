package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.GiftWaypoints;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactEntityAtLocation", at = @At("HEAD"))
    private void nef$recordGiftUse(PlayerEntity player, Entity entity, EntityHitResult hit, Hand hand,
                                   CallbackInfoReturnable<ActionResult> callback) {
        GiftWaypoints.recordInteraction(entity);
    }

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void nef$recordGiftAttack(PlayerEntity player, Entity entity, CallbackInfo callback) {
        GiftWaypoints.recordInteraction(entity);
    }
}
