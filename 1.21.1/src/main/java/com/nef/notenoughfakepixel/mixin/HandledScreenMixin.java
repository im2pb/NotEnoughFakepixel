package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nef.notenoughfakepixel.features.skyblock.enchanting.ExperimentationSolver;
import com.nef.notenoughfakepixel.features.skyblock.slotlocking.SlotLocking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
abstract class HandledScreenMixin {
    @Shadow protected Slot focusedSlot;

    @WrapOperation(method = {"drawSlot", "drawMouseoverTooltip"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/screen/slot/Slot;getStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack nef$showRememberedSuperpairsCard(Slot slot, Operation<ItemStack> original) {
        return ExperimentationSolver.INSTANCE.displayStack(MinecraftClient.getInstance(),
                (HandledScreen<?>) (Object) this, slot, original.call(slot));
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void nef$drawSlotLocks(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        SlotLocking.INSTANCE.draw(context, (HandledScreen<?>) (Object) this, mouseX, mouseY);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void nef$hideExperimentTooltip(DrawContext context, int mouseX, int mouseY, CallbackInfo callback) {
        if (ExperimentationSolver.INSTANCE.hidesTooltip(MinecraftClient.getInstance(),
                (HandledScreen<?>) (Object) this, focusedSlot)) callback.cancel();
    }
}
