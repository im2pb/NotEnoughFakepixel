package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nef.notenoughfakepixel.features.skyblock.slotlocking.SlotLocking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
    @WrapOperation(method = "handleInputEvents", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = Opcodes.PUTFIELD))
    private void nef$trackHotbarKey(PlayerInventory inventory, int next, Operation<Void> original) {
        SlotLocking.INSTANCE.selectingHotbarSlot(inventory, next);
        original.call(inventory, next);
    }

    @WrapWithCondition(method = "handleInputEvents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V",
            ordinal = 0))
    private boolean nef$protectOffhandSwap(ClientPlayNetworkHandler handler, Packet<?> packet) {
        if (!(packet instanceof PlayerActionC2SPacket action)
                || action.getAction() != PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) return true;

        var player = MinecraftClient.getInstance().player;
        return player == null || !SlotLocking.INSTANCE.protectsInventoryIndex(player.getInventory().selectedSlot);
    }
}
