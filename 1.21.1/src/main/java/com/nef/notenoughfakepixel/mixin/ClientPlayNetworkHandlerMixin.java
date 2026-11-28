package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nef.notenoughfakepixel.features.skyblock.crimson.AshfangHelper;
import com.nef.notenoughfakepixel.features.skyblock.enchanting.ExperimentationSolver;
import com.nef.notenoughfakepixel.features.skyblock.fishing.FishingAlert;
import com.nef.notenoughfakepixel.features.skyblock.inventory.PagedMenuTracker;
import com.nef.notenoughfakepixel.features.skyblock.mining.DrillFix;
import com.nef.notenoughfakepixel.features.skyblock.slayers.SlayerAlerts;
import com.nef.notenoughfakepixel.features.skyblock.slayers.VoidgloomBeacons;
import com.nef.notenoughfakepixel.features.skyblock.qol.CombatVisuals;
import com.nef.notenoughfakepixel.features.skyblock.qol.TeleportCamera;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onPlaySound", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V"),
            cancellable = true)
    private void nef$muteAshfangSound(PlaySoundS2CPacket packet, CallbackInfo callback) {
        if (AshfangHelper.muteSound(MinecraftClient.getInstance(), packet)) callback.cancel();
    }

    @Inject(method = "onPlaySound", at = @At("RETURN"))
    private void nef$observeSlayerSound(PlaySoundS2CPacket packet, CallbackInfo callback) {
        SlayerAlerts.INSTANCE.observeSound(MinecraftClient.getInstance(), packet);
    }

    @Inject(method = "onBlockUpdate", at = @At("RETURN"))
    private void nef$observeSlayerBeacon(BlockUpdateS2CPacket packet, CallbackInfo callback) {
        VoidgloomBeacons.INSTANCE.observeBlock(MinecraftClient.getInstance(), packet.getPos(), packet.getState());
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void nef$observeSlayerBeacons(ChunkDeltaUpdateS2CPacket packet, CallbackInfo callback) {
        VoidgloomBeacons.INSTANCE.observeChunkDelta(MinecraftClient.getInstance(), packet);
    }

    @WrapOperation(method = "onPlayerPositionLook", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;setPosition(DDD)V"))
    private void nef$animateAspectTeleport(PlayerEntity player, double x, double y, double z, Operation<Void> original) {
        var before = player.getPos();
        original.call(player, x, y, z);
        TeleportCamera.INSTANCE.positionChanged(player, before);
    }

    @Inject(method = "onParticle", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V"),
            cancellable = true)
    private void nef$skipImplosionParticles(ParticleS2CPacket packet, CallbackInfo callback) {
        if (CombatVisuals.hideExplosionParticle(packet)) callback.cancel();
    }

    @Inject(method = "onParticle", at = @At("RETURN"))
    private void nef$observeFishingParticle(ParticleS2CPacket packet, CallbackInfo callback) {
        FishingAlert.INSTANCE.observeParticle(MinecraftClient.getInstance(), packet);
    }

    @WrapOperation(method = "onEntityPosition", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;updateTrackedPositionAndAngles(DDDFFI)V"))
    private void nef$snapEndermanTeleport(Entity entity, double x, double y, double z, float yaw, float pitch,
                                         int interpolationSteps, Operation<Void> original) {
        if (CombatVisuals.hideEndermanTeleport(entity) && MathHelper.squaredMagnitude(
                x - entity.getLerpTargetX(), y - entity.getLerpTargetY(), z - entity.getLerpTargetZ()) >= 4) {
            // Keep the server destination and skip rendering the trip between the two positions.
            original.call(entity, x, y, z, yaw, pitch, 0);
            entity.refreshPositionAndAngles(x, y, z, yaw, pitch);
        } else {
            original.call(entity, x, y, z, yaw, pitch, interpolationSteps);
        }
    }

    @Inject(method = "onInventory", at = @At("RETURN"))
    private void nef$readMenuContents(InventoryS2CPacket packet, CallbackInfo callback) {
        var client = MinecraftClient.getInstance();
        PagedMenuTracker.contentsReceived(client, packet.getSyncId(), packet.getContents());
        ExperimentationSolver.INSTANCE.contentsReceived(client, packet.getSyncId(), packet.getContents());
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    private void nef$readMenuSlot(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo callback) {
        var client = MinecraftClient.getInstance();
        PagedMenuTracker.slotReceived(client, packet.getSyncId(), packet.getSlot(), packet.getStack());
        ExperimentationSolver.INSTANCE.slotReceived(client, packet.getSyncId(), packet.getSlot(), packet.getStack());
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
