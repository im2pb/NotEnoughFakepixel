/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.qol.EtherwarpZoom;
import org.lwjgl.util.vector.Vector3f;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.GlStateManager;
import com.nef.notenoughfakepixel.features.skyblock.qol.SmoothAote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer implements IResourceManagerReloadListener {

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void onHurtCam(float partialTicks, CallbackInfo ci) {
        if (Config.feature.qol.qolNoHurtCam) ci.cancel();
    }

    @Inject(method = "addRainParticles", at = @At("HEAD"), cancellable = true)
    private void disableRainRendering(CallbackInfo ci) {
        if (Config.feature.qol.qolDisableRain) {
            ci.cancel();
        }
    }

    @Shadow private Minecraft mc;

    @Redirect(method = "updateCameraAndRender", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/settings/GameSettings;mouseSensitivity:F",
            opcode = Opcodes.GETFIELD
    ))
    public float updateCameraAndRender_mouseSensitivity(GameSettings gameSettings) {
        return gameSettings.mouseSensitivity * EtherwarpZoom.getSensMultiplier();
    }

    @Inject(
            method = "getFOVModifier",
            at = @At("RETURN"),
            cancellable = true
    )
    public void getFOVModifier_override(float partialTicks, boolean useFOVSetting, CallbackInfoReturnable<Float> cir) {
        float currentFov = cir.getReturnValueF();
        cir.setReturnValue(currentFov * EtherwarpZoom.getFovMultiplier(partialTicks));
    }


    @Redirect(method = "renderWorldPass", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;dispatchRenderLast(Lnet/minecraft/client/renderer/RenderGlobal;F)V",
            remap = false))
    private void smoothAote$renderLast(RenderGlobal context, float partialTicks) {
        SmoothAote feature = SmoothAote.getInstance();
        Vector3f current = feature == null ? null : feature.getCurrentPosition();
        if (current == null) {
            ForgeHooksClient.dispatchRenderLast(context, partialTicks);
            return;
        }
        Entity view = Minecraft.getMinecraft().getRenderViewEntity();
        if (view == null) {
            ForgeHooksClient.dispatchRenderLast(context, partialTicks);
            return;
        }
        double x = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double y = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double z = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(-current.x + x, -current.y + y, -current.z + z);
            ForgeHooksClient.dispatchRenderLast(context, partialTicks);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE"), ordinal = 0)
    private double smoothAote$cameraX(double original) { Vector3f p = smoothAotePosition(); return p == null ? original : p.x; }
    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE"), ordinal = 1)
    private double smoothAote$cameraY(double original) { Vector3f p = smoothAotePosition(); return p == null ? original : p.y; }
    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE"), ordinal = 2)
    private double smoothAote$cameraZ(double original) { Vector3f p = smoothAotePosition(); return p == null ? original : p.z; }

    @ModifyVariable(method = "renderWorldPass", at = @At(value = "STORE"), ordinal = 0)
    private double smoothAote$worldX(double original) { Vector3f p = smoothAotePosition(); return p == null ? original : p.x; }
    @ModifyVariable(method = "renderWorldPass", at = @At(value = "STORE"), ordinal = 1)
    private double smoothAote$worldY(double original) { Vector3f p = smoothAotePosition(); return p == null ? original : p.y; }
    @ModifyVariable(method = "renderWorldPass", at = @At(value = "STORE"), ordinal = 2)
    private double smoothAote$worldZ(double original) { Vector3f p = smoothAotePosition(); return p == null ? original : p.z; }

    private Vector3f smoothAotePosition() {
        SmoothAote feature = SmoothAote.getInstance();
        return feature == null ? null : feature.getCurrentPosition();
    }
}
