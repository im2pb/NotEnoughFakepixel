/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.events.handlers;

import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.events.PacketWriteEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;

public class PacketHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Packet) {
            if (MinecraftForge.EVENT_BUS.post(new PacketReadEvent((Packet) msg))) return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Packet) {
            if (MinecraftForge.EVENT_BUS.post(new PacketWriteEvent((Packet) msg))) return;
        }

        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (containsDecoderException(cause)) {
            ConnectionHandler.onDecoderError();
        }
        super.exceptionCaught(ctx, cause);
    }

    /** Decoder failures can be wrapped by Netty before reaching this handler. */
    private static boolean containsDecoderException(Throwable cause) {
        Throwable current = cause;
        for (int depth = 0; current != null && depth < 16; depth++) {
            if (current instanceof DecoderException) return true;
            Throwable next = current.getCause();
            if (next == current) break;
            current = next;
        }
        return false;
    }

}
