package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.qol.TeleportCamera;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private Vec3d pos;
    @Shadow protected abstract void setPos(Vec3d pos);

    @Inject(method = "update", at = @At("RETURN"))
    private void nef$smoothTeleport(BlockView world, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                    float tickDelta, CallbackInfo callback) {
        Vec3d animated = TeleportCamera.INSTANCE.cameraPosition(focusedEntity, pos, thirdPerson);
        if (animated != pos) setPos(animated);
    }
}
