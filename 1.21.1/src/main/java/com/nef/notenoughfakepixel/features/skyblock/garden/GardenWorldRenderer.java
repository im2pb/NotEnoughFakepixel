package com.nef.notenoughfakepixel.features.skyblock.garden;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Garden;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

@RegisterEvents
public final class GardenWorldRenderer implements ClientModInitializer {
    private static final Pattern PEST_NAME = Pattern.compile(
            "(?<![\\p{L}\\p{N}_])(?:Fly|Cricket|Locust|Rat|Mosquito|Earthworm|Mite|Moth|Slug|Beetle|Firefly"
                    + "|Dragonfly|Praying Mantis|Field Mouse)(?![\\p{L}\\p{N}_])",
            Pattern.CASE_INSENSITIVE);
    private static final Map<LineStyle, RenderLayer> LINE_LAYERS = new HashMap<>();

    private final List<Entity> pests = new ArrayList<>();
    private final ConfigColor grillColor = new ConfigColor();
    private final ConfigColor boxColor = new ConfigColor();
    private final ConfigColor tracerColor = new ConfigColor();
    private ClientWorld pestWorld;
    private BufferAllocator allocator;
    private VertexConsumerProvider.Immediate consumers;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::updatePests);
        WorldRenderEvents.LAST.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (allocator != null) allocator.close();
        });
    }

    private void updatePests(MinecraftClient client) {
        pests.clear();
        pestWorld = client.world;
        if (pestWorld == null || !GardenOverlay.inGarden()) return;

        Garden garden = ClientConfig.configuration().garden;
        if (!garden.enable || !garden.pestEsp.enabled) return;

        for (Entity entity : pestWorld.getEntities()) {
            if (entity instanceof PlayerEntity || entity instanceof ItemEntity || !entity.isAlive()) continue;
            Text displayName = entity.getDisplayName();
            if (matchesPestName(entity.getName().getString())
                    || (displayName != null && matchesPestName(displayName.getString()))) {
                pests.add(entity);
            }
        }
    }

    static boolean matchesPestName(String name) {
        return PEST_NAME.matcher(Formatting.strip(name)).find();
    }

    private void render(WorldRenderContext context) {
        if (!GardenOverlay.inGarden() || context.matrixStack() == null) return;

        Garden garden = ClientConfig.configuration().garden;
        if (!garden.enable) return;

        boolean showGrill = garden.pestPlotGrill.enabled && !SkyblockData.activePests().isEmpty();
        boolean showPests = garden.pestEsp.enabled && context.world() == pestWorld && !pests.isEmpty();
        if (!showGrill && !showPests) return;

        if (consumers == null) {
            allocator = new BufferAllocator(4096);
            consumers = VertexConsumerProvider.immediate(allocator);
        }

        boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try {
            if (showGrill) {
                var grill = garden.pestPlotGrill;
                double spacing = Float.isFinite(grill.gridSpacing) ? Math.clamp(grill.gridSpacing, 2, 32) : 8;
                GardenPlots.drawInfested(lines(context, grill.lineWidth, false, grillColor.resolve(grill.color)),
                        SkyblockData.activePests(), spacing);
            }
            if (showPests) drawPests(context, garden.pestEsp, false);
            consumers.draw();

            if (showPests && (garden.pestEsp.boxThroughWalls || (garden.pestEsp.tracers && garden.pestEsp.tracerThroughWalls))) {
                // The vanilla always-depth phase leaves the current depth-test state alone.
                RenderSystem.disableDepthTest();
                drawPests(context, garden.pestEsp, true);
                consumers.draw();
            }
        } finally {
            if (depthWasEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }

    private void drawPests(WorldRenderContext context, Garden.PestEspSettings settings, boolean throughWalls) {
        float delta = context.tickCounter().getTickDelta(false);
        if (settings.boxThroughWalls == throughWalls) {
            Lines lines = lines(context, settings.boxLineWidth, throughWalls, boxColor.resolve(settings.boxColor));
            for (Entity pest : pests) {
                if (!pest.isAlive()) continue;
                Vec3d position = pest.getLerpedPos(delta).subtract(lines.camera());
                WorldRenderer.drawBox(context.matrixStack(), lines.vertices(),
                        position.x - 0.5, position.y - 1.0, position.z - 0.5,
                        position.x + 0.5, position.y, position.z + 0.5,
                        channel(lines.color(), 16), channel(lines.color(), 8), channel(lines.color(), 0), channel(lines.color(), 24));
            }
        }

        if (settings.tracers && settings.tracerThroughWalls == throughWalls) {
            Lines lines = lines(context, settings.tracerLineWidth, throughWalls, tracerColor.resolve(settings.tracerColor));
            Vec3d eye = context.camera().getFocusedEntity().getCameraPosVec(delta);
            for (Entity pest : pests) {
                if (!pest.isAlive()) continue;
                Vec3d target = pest.getLerpedPos(delta).add(0, -0.5, 0);
                Vec3d direction = target.subtract(eye);
                if (direction.lengthSquared() <= 0.01) continue;
                // Keep the start just off the eye so the line shader can project it in first person.
                Vec3d start = eye.add(direction.normalize().multiply(0.1));
                lines.draw(start.x, start.y, start.z, target.x, target.y, target.z);
            }
        }
    }

    private Lines lines(WorldRenderContext context, float width, boolean throughWalls, int color) {
        var style = new LineStyle(Math.clamp(Math.round(width), 1, 8), throughWalls);
        RenderLayer layer = LINE_LAYERS.computeIfAbsent(style, GardenWorldRenderer::createLineLayer);
        return new Lines(context.matrixStack().peek(), consumers.getBuffer(layer), context.camera().getPos(), color);
    }

    private static RenderLayer createLineLayer(LineStyle style) {
        return RenderLayer.of("nef_garden_lines", VertexFormats.LINES, VertexFormat.DrawMode.LINES, 4096, false, false,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.LINES_PROGRAM)
                        .lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(style.width())))
                        .cull(RenderPhase.DISABLE_CULLING)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .depthTest(style.throughWalls() ? RenderPhase.ALWAYS_DEPTH_TEST : RenderPhase.LEQUAL_DEPTH_TEST)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .build(false));
    }

    private static float channel(int argb, int shift) {
        return ((argb >>> shift) & 0xFF) / 255.0F;
    }

    private record LineStyle(int width, boolean throughWalls) {
    }

    record Lines(MatrixStack.Entry matrix, VertexConsumer vertices, Vec3d camera, int color) {
        void draw(double x1, double y1, double z1, double x2, double y2, double z2) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length == 0) return;

            float nx = (float) (dx / length);
            float ny = (float) (dy / length);
            float nz = (float) (dz / length);
            vertices.vertex(matrix, (float) (x1 - camera.x), (float) (y1 - camera.y), (float) (z1 - camera.z))
                    .color(color).normal(matrix, nx, ny, nz);
            vertices.vertex(matrix, (float) (x2 - camera.x), (float) (y2 - camera.y), (float) (z2 - camera.z))
                    .color(color).normal(matrix, nx, ny, nz);
        }
    }
}
