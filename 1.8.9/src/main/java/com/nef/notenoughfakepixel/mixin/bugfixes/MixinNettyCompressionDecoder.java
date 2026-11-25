package com.nef.notenoughfakepixel.mixin.bugfixes;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NettyCompressionDecoder;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;
import java.util.zip.Inflater;

@Mixin(NettyCompressionDecoder.class)
public class MixinNettyCompressionDecoder {

    @Shadow private Inflater inflater;

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void fixCompressionThreshold(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, CallbackInfo ci) throws Exception {
        if (in.readableBytes() == 0) {
            ci.cancel();
            return;
        }
        PacketBuffer buf = new PacketBuffer(in);
        int uncompressedSize = buf.readVarIntFromBuffer();
        if (uncompressedSize == 0) {
            out.add(buf.readBytes(buf.readableBytes()));
        } else {
            if (uncompressedSize > 2097152) {
                throw new IOException("Badly compressed packet - size of " + uncompressedSize + " is larger than protocol maximum of 2097152");
            }
            byte[] compressed = new byte[buf.readableBytes()];
            buf.readBytes(compressed);
            inflater.setInput(compressed);
            byte[] decompressed = new byte[uncompressedSize];
            inflater.inflate(decompressed);
            out.add(Unpooled.wrappedBuffer(decompressed));
            inflater.reset();
        }
        ci.cancel();
    }
}
