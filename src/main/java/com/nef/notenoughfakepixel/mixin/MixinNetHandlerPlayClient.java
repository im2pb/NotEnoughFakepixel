/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.events.ParticlePacketEvent;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.entity.player.EntityPlayer;
import com.nef.notenoughfakepixel.features.skyblock.qol.SmoothAote;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Shadow private Minecraft gameController;

    @Inject(
            method = "handleParticles(Lnet/minecraft/network/play/server/S2APacketParticles;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nef$onHandleParticles(S2APacketParticles packet, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new ParticlePacketEvent(packet))) {
            ci.cancel();
            return;
        }

    }

    @Inject(
            method = "handleUpdateTileEntity(Lnet/minecraft/network/play/server/S35PacketUpdateTileEntity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nef$guardTileEntityUpdate(S35PacketUpdateTileEntity packet, CallbackInfo ci) {
        if (packet == null || packet.getPos() == null || packet.getNbtCompound() == null ||
                gameController == null || gameController.theWorld == null) {
            ci.cancel();
            return;
        }

        if (gameController.theWorld.isBlockLoaded(packet.getPos())) {
            TileEntity tileEntity = gameController.theWorld.getTileEntity(packet.getPos());
            if (tileEntity == null) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "handleEntityEquipment(Lnet/minecraft/network/play/server/S04PacketEntityEquipment;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nef$guardEntityEquipment(S04PacketEntityEquipment packet, CallbackInfo ci) {
        if (packet == null || packet.getEquipmentSlot() < 0 || packet.getEquipmentSlot() > 4 ||
                gameController == null || gameController.theWorld == null) {
            ci.cancel();
        }
    }


    @Redirect(method = "handlePlayerPosLook", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/EntityPlayer;setPositionAndRotation(DDDFF)V"))
    private void smoothAote$positionPacket(EntityPlayer player, double x, double y, double z, float yaw, float pitch) {
        SmoothAote feature = SmoothAote.getInstance();
        if (feature != null) feature.onTeleportApplied();
        player.setPositionAndRotation(x, y, z, yaw, pitch);
    }
}
