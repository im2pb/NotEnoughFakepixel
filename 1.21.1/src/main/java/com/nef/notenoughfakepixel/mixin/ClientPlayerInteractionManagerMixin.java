package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.nef.notenoughfakepixel.features.skyblock.accessories.MissingAccessories;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.GiftWaypoints;
import com.nef.notenoughfakepixel.features.skyblock.slotlocking.SlotLocking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.slot.SlotActionType;
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
    @WrapMethod(method = "clickSlot")
    private void nef$protectLockedSlots(int syncId, int slotId, int button, SlotActionType action,
                                         PlayerEntity player, Operation<Void> original) {
        var click = SlotLocking.INSTANCE.checkClick(syncId, slotId, button, action, player);
        if (click != null) {
            original.call(syncId, click.slotId(), click.button(), click.action(), player);
            MissingAccessories.INSTANCE.clickedSlot(MinecraftClient.getInstance(), syncId, click.slotId());
        }
    }

    @Inject(method = "pickFromInventory", at = @At("HEAD"), cancellable = true)
    private void nef$protectInventoryPick(int slot, CallbackInfo callback) {
        var player = MinecraftClient.getInstance().player;
        if (player != null && (SlotLocking.INSTANCE.protectsInventoryIndex(slot)
                || SlotLocking.INSTANCE.protectsInventoryIndex(player.getInventory().getSwappableHotbarSlot()))) {
            callback.cancel();
        }
    }

    @Inject(method = "clickRecipe", at = @At("HEAD"), cancellable = true)
    private void nef$protectRecipeFill(int syncId, RecipeEntry<?> recipe, boolean craftAll, CallbackInfo callback) {
        var player = MinecraftClient.getInstance().player;
        if (player != null && SlotLocking.INSTANCE.blocksRecipeFill(player.getInventory(), recipe)) {
            callback.cancel();
        }
    }

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
