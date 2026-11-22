/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin.bugfixes;

import com.nef.notenoughfakepixel.events.NefPacketBlockChange;
import com.nef.notenoughfakepixel.features.skyblock.slayers.VoidgloomSeraph;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class NetHandlerPlayClientMixin {
    
    @Redirect(
            method = "handleUpdateSign",
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=Unable to locate sign at ", ordinal = 0)),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;addChatMessage(Lnet/minecraft/util/IChatComponent;)V", ordinal = 0)
    )
    private void removeSignDebugMessage(EntityPlayerSP instance, IChatComponent component) {
        
    }

    @Inject(method = "handleBlockChange", at = @At("HEAD"))
    public void handleBlockChange(S23PacketBlockChange packetIn, CallbackInfo ci) {
        VoidgloomSeraph.processBlockChange(new NefPacketBlockChange(packetIn));

    }
}
