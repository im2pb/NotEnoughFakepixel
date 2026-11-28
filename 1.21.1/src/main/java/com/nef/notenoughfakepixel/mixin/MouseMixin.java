package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.qol.TeleportCamera;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Mouse.class)
abstract class MouseMixin {
    @ModifyArgs(method = "updateMouse", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void nef$etherwarpSensitivity(Args args) {
        double scale = TeleportCamera.INSTANCE.mouseMultiplier();
        if (scale != 1) {
            args.set(0, args.<Double>get(0) * scale);
            args.set(1, args.<Double>get(1) * scale);
        }
    }
}
