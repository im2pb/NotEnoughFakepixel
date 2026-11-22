/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.qol.SmoothAote;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.util.vector.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    @ModifyVariable(method = "setupTerrain", at = @At(value = "STORE"), ordinal = 4)
    private double smoothAote$terrainX(double original) { Vector3f p = position(); return p == null ? original : p.x; }
    @ModifyVariable(method = "setupTerrain", at = @At(value = "STORE"), ordinal = 5)
    private double smoothAote$terrainY(double original) { Vector3f p = position(); return p == null ? original : p.y; }
    @ModifyVariable(method = "setupTerrain", at = @At(value = "STORE"), ordinal = 6)
    private double smoothAote$terrainZ(double original) { Vector3f p = position(); return p == null ? original : p.z; }

    @ModifyVariable(method = "renderEntities", at = @At(value = "STORE"), ordinal = 3)
    private double smoothAote$entitiesX(double original) { Vector3f p = position(); return p == null ? original : p.x; }
    @ModifyVariable(method = "renderEntities", at = @At(value = "STORE"), ordinal = 4)
    private double smoothAote$entitiesY(double original) { Vector3f p = position(); return p == null ? original : p.y; }
    @ModifyVariable(method = "renderEntities", at = @At(value = "STORE"), ordinal = 5)
    private double smoothAote$entitiesZ(double original) { Vector3f p = position(); return p == null ? original : p.z; }

    @ModifyVariable(method = "drawBlockDamageTexture", at = @At(value = "STORE"), ordinal = 0)
    private double smoothAote$damageX(double original) { Vector3f p = position(); return p == null ? original : p.x; }
    @ModifyVariable(method = "drawBlockDamageTexture", at = @At(value = "STORE"), ordinal = 1)
    private double smoothAote$damageY(double original) { Vector3f p = position(); return p == null ? original : p.y; }
    @ModifyVariable(method = "drawBlockDamageTexture", at = @At(value = "STORE"), ordinal = 2)
    private double smoothAote$damageZ(double original) { Vector3f p = position(); return p == null ? original : p.z; }

    @Inject(method = "drawSelectionBox", at = @At("HEAD"), cancellable = true)
    private void smoothAote$hideSelectionBox(EntityPlayer player, MovingObjectPosition hit, int execute, float partialTicks, CallbackInfo ci) {
        if (position() != null) ci.cancel();
    }

    private Vector3f position() {
        SmoothAote feature = SmoothAote.getInstance();
        return feature == null ? null : feature.getCurrentPosition();
    }
}
