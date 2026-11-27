package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.OptionalDouble;

final class DungeonBoxes implements AutoCloseable {
    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate consumers;

    void draw(WorldRenderContext context, Collection<Marker> markers, boolean filled, boolean throughWalls) {
        if (markers.isEmpty() || context.matrixStack() == null) return;
        if (allocator == null) {
            allocator = new BufferAllocator(4096);
            consumers = VertexConsumerProvider.immediate(allocator);
        }

        RenderLayer layer = filled
                ? (throughWalls ? Layers.THROUGH_WALL_FILL : Layers.FILL)
                : (throughWalls ? Layers.THROUGH_WALL_LINES : Layers.LINES);
        var matrices = context.matrixStack();
        var camera = context.camera().getPos();
        boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try {
            if (throughWalls) RenderSystem.disableDepthTest();
            else RenderSystem.enableDepthTest();

            var vertices = consumers.getBuffer(layer);
            for (Marker marker : markers) {
                Box box = marker.box().offset(-camera.x, -camera.y, -camera.z);
                int color = marker.color();
                float red = channel(color, 16);
                float green = channel(color, 8);
                float blue = channel(color, 0);
                float alpha = channel(color, 24);
                if (filled) {
                    WorldRenderer.renderFilledBox(matrices, vertices, box.minX, box.minY, box.minZ,
                            box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
                } else {
                    WorldRenderer.drawBox(matrices, vertices, box, red, green, blue, alpha);
                }
            }
            consumers.draw();
        } finally {
            if (depthWasEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }

    private static float channel(int color, int shift) {
        return ((color >>> shift) & 0xFF) / 255.0F;
    }

    @Override
    public void close() {
        if (allocator != null) {
            allocator.close();
            allocator = null;
            consumers = null;
        }
    }

    record Marker(Box box, int color) {
    }

    private static final class Layers {
        private static final RenderLayer FILL = create("nef_dungeon_fill", true, false);
        private static final RenderLayer THROUGH_WALL_FILL = create("nef_dungeon_fill_through_walls", true, true);
        private static final RenderLayer LINES = create("nef_dungeon_lines", false, false);
        private static final RenderLayer THROUGH_WALL_LINES = create("nef_dungeon_lines_through_walls", false, true);

        private static RenderLayer create(String name, boolean filled, boolean throughWalls) {
            var phases = RenderLayer.MultiPhaseParameters.builder()
                    .program(filled ? RenderPhase.COLOR_PROGRAM : RenderPhase.LINES_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .depthTest(throughWalls ? RenderPhase.ALWAYS_DEPTH_TEST : RenderPhase.LEQUAL_DEPTH_TEST)
                    .writeMaskState(RenderPhase.COLOR_MASK);
            if (!filled) phases.lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(3)));
            return RenderLayer.of(name, filled ? VertexFormats.POSITION_COLOR : VertexFormats.LINES,
                    filled ? VertexFormat.DrawMode.TRIANGLE_STRIP : VertexFormat.DrawMode.LINES,
                    4096, false, false, phases.build(false));
        }
    }
}
