package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.slotlocking.SlotLocking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
abstract class HandledScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void nef$drawSlotLocks(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo callback) {
        SlotLocking.INSTANCE.draw(context, (HandledScreen<?>) (Object) this, mouseX, mouseY);
    }
}
