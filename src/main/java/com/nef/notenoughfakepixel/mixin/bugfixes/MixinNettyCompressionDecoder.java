/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Compression decoder compatibility patch based on the beta 42 behavior.
 * It intentionally accepts compressed packets below the negotiated threshold,
 * while retaining size, stream-integrity, and inflater-lifecycle checks.
 */
package com.nef.notenoughfakepixel.mixin.bugfixes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NettyCompressionDecoder;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

@Mixin(NettyCompressionDecoder.class)
public abstract class MixinNettyCompressionDecoder {

    @Unique
    private static final int BETTERNEF_MAX_UNCOMPRESSED_SIZE = 2097152;

    @Shadow
    @Final
    private Inflater inflater;

    /** Reused one-byte overflow probe; decode runs on the channel event loop. */
    @Unique
    private byte[] betterNef$overflowProbe;

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void betterNef$decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, CallbackInfo ci) throws Exception {
        if (!in.isReadable()) {
            ci.cancel();
            return;
        }

        PacketBuffer packetBuffer = new PacketBuffer(in);
        int uncompressedSize = packetBuffer.readVarIntFromBuffer();
        if (uncompressedSize < 0 || uncompressedSize > BETTERNEF_MAX_UNCOMPRESSED_SIZE) {
            throw new IOException("Badly compressed packet - invalid uncompressed size " + uncompressedSize
                    + " (maximum " + BETTERNEF_MAX_UNCOMPRESSED_SIZE + ")");
        }

        if (uncompressedSize == 0) {
            // Protocol marker for an uncompressed payload; retain the remaining bytes as-is.
            out.add(packetBuffer.readBytes(packetBuffer.readableBytes()));
            ci.cancel();
            return;
        }

        int compressedLength = packetBuffer.readableBytes();
        if (compressedLength == 0) {
            throw new IOException("Badly compressed packet - compressed payload is empty");
        }

        byte[] compressed = new byte[compressedLength];
        packetBuffer.readBytes(compressed);
        byte[] decompressed = new byte[uncompressedSize];

        // An Inflater is stateful. Reset before and in finally so malformed input cannot
        // poison the next packet handled by this decoder instance.
        inflater.reset();
        try {
            inflater.setInput(compressed);
            int offset = 0;
            while (offset < decompressed.length) {
                int written = inflater.inflate(decompressed, offset, decompressed.length - offset);
                if (written > 0) {
                    offset += written;
                    continue;
                }
                if (inflater.needsDictionary()) {
                    throw new IOException("Badly compressed packet - dictionary required");
                }
                if (inflater.needsInput()) break;
                throw new IOException("Badly compressed packet - decompressor made no progress");
            }

            // Probe one byte after the declared output length to reject size mismatches.
            if (betterNef$overflowProbe == null) betterNef$overflowProbe = new byte[1];
            int overflow = inflater.inflate(betterNef$overflowProbe);
            if (overflow > 0) {
                throw new IOException("Badly compressed packet - decompressed data exceeds declared size "
                        + uncompressedSize);
            }
            if (offset != uncompressedSize || !inflater.finished()) {
                throw new IOException("Badly compressed packet - expected " + uncompressedSize
                        + " decompressed bytes but received " + offset);
            }
            if (inflater.getRemaining() != 0) {
                throw new IOException("Badly compressed packet - trailing compressed bytes");
            }

            out.add(Unpooled.wrappedBuffer(decompressed));
        } catch (DataFormatException exception) {
            throw new IOException("Badly compressed packet - invalid deflate stream", exception);
        } finally {
            inflater.reset();
        }

        ci.cancel();
    }
}
