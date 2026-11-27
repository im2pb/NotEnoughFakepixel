package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nef.notenoughfakepixel.features.skyblock.slotlocking.SlotLocking;
import net.minecraft.entity.player.PlayerInventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInventory.class)
abstract class PlayerInventoryMixin {
    @WrapOperation(method = "scrollInHotbar", at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = Opcodes.PUTFIELD, ordinal = 0))
    private void nef$trackHotbarScroll(PlayerInventory inventory, int next, Operation<Void> original) {
        SlotLocking.INSTANCE.selectingHotbarSlot(inventory, next);
        original.call(inventory, next);
    }
}
