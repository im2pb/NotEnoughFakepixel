package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nef.notenoughfakepixel.features.skyblock.fishing.FishingAlert;
import com.nef.notenoughfakepixel.features.skyblock.inventory.PagedMenuTracker;
import com.nef.notenoughfakepixel.features.skyblock.mining.DrillFix;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onParticle", at = @At("RETURN"))
    private void nef$observeFishingParticle(ParticleS2CPacket packet, CallbackInfo callback) {
        FishingAlert.INSTANCE.observeParticle(MinecraftClient.getInstance(), packet);
    }

    @Inject(method = "onInventory", at = @At("RETURN"))
    private void nef$readMenuContents(InventoryS2CPacket packet, CallbackInfo callback) {
        PagedMenuTracker.contentsReceived(MinecraftClient.getInstance(), packet.getSyncId(), packet.getContents());
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    private void nef$readMenuSlot(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo callback) {
        PagedMenuTracker.slotReceived(MinecraftClient.getInstance(), packet.getSyncId(), packet.getSlot(), packet.getStack());
    }

    // The vanilla handlers switch to the client thread before reaching these calls.
    @WrapOperation(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerInventory;setStack(ILnet/minecraft/item/ItemStack;)V"))
    private void nef$deferDirectFuelUpdate(PlayerInventory inventory, int slot, ItemStack stack, Operation<Void> original) {
        original.call(inventory, slot, DrillFix.INSTANCE.inventoryStack(MinecraftClient.getInstance(), inventory, slot, stack));
    }

    @WrapOperation(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/screen/PlayerScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V"))
    private void nef$deferHotbarFuelUpdate(PlayerScreenHandler handler, int slot, int revision, ItemStack stack,
                                           Operation<Void> original) {
        original.call(handler, slot, revision, DrillFix.INSTANCE.screenStack(MinecraftClient.getInstance(), handler, slot, stack));
    }

    @WrapOperation(method = "onScreenHandlerSlotUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/screen/ScreenHandler;setStackInSlot(IILnet/minecraft/item/ItemStack;)V"))
    private void nef$deferMenuSlotFuelUpdate(ScreenHandler handler, int slot, int revision, ItemStack stack,
                                             Operation<Void> original) {
        original.call(handler, slot, revision, DrillFix.INSTANCE.screenStack(MinecraftClient.getInstance(), handler, slot, stack));
    }

    @WrapOperation(method = "onInventory", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/screen/PlayerScreenHandler;updateSlotStacks(ILjava/util/List;Lnet/minecraft/item/ItemStack;)V"))
    private void nef$deferInventoryFuelUpdate(PlayerScreenHandler handler, int revision, List<ItemStack> stacks,
                                              ItemStack cursor, Operation<Void> original) {
        original.call(handler, revision, DrillFix.INSTANCE.screenContents(MinecraftClient.getInstance(), handler, stacks), cursor);
    }

    @WrapOperation(method = "onInventory", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/screen/ScreenHandler;updateSlotStacks(ILjava/util/List;Lnet/minecraft/item/ItemStack;)V"))
    private void nef$deferMenuInventoryFuelUpdate(ScreenHandler handler, int revision, List<ItemStack> stacks,
                                                  ItemStack cursor, Operation<Void> original) {
        original.call(handler, revision, DrillFix.INSTANCE.screenContents(MinecraftClient.getInstance(), handler, stacks), cursor);
    }
}
