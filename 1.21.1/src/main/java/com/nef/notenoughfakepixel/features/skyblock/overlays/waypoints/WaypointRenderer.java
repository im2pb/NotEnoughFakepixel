package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Waypoints;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.IslandWaypoints.Waypoint;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class WaypointRenderer implements AutoCloseable {
    private static final double THROUGH_WALL_DISTANCE_SQUARED = 10 * 10;
    private static final double LABEL_RENDER_DISTANCE = 12;
    private static final int BEAM_HEIGHT = 300;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int YELLOW = 0xFFFFFF55;

    private final SequencedMap<RenderLayer, BufferAllocator> beamBuffers = new LinkedHashMap<>();
    private BufferAllocator textBuffer;
    private VertexConsumerProvider.Immediate consumers;
    private VertexConsumerProvider throughWallConsumers;

    static Waypoints visibleSettings(WorldRenderContext context) {
        if (!SkyblockData.isSkyblock() || context.matrixStack() == null || context.world() == null) return null;

        Waypoints settings = ClientConfig.configuration().waypoints;
        return settings.generalWaypointToggle ? settings : null;
    }

    void render(WorldRenderContext context, List<Waypoint> waypoints, boolean beacons, int islandColor,
                double labelHeight, boolean showDistance) {
        render(context, waypoints, beacons, islandColor, labelHeight, showDistance, false);
    }

    public void render(WorldRenderContext context, List<Waypoint> waypoints, boolean beacons, int islandColor,
                       double labelHeight, boolean showDistance, boolean beamsAlwaysThroughWalls) {
        if (waypoints.isEmpty() || context.matrixStack() == null) return;

        prepareBuffers();
        Camera camera = context.camera();
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = camera.getPos();
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try {
            if (beacons && !beamsAlwaysThroughWalls) renderBeams(context, matrices, cameraPos, waypoints, islandColor, BeamVisibility.NEAR);

            // Vanilla's see-through layers expect depth testing to be off before they draw.
            RenderSystem.disableDepthTest();
            if (beacons) renderBeams(context, matrices, cameraPos, waypoints, islandColor,
                    beamsAlwaysThroughWalls ? BeamVisibility.THROUGH_WALLS : BeamVisibility.FAR);

            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            // See-through layers sort quads by distance. Flush shadows before submitting the lettering.
            renderLabels(matrices, camera, cameraPos, font, waypoints, labelHeight, showDistance, true);
            renderLabels(matrices, camera, cameraPos, font, waypoints, labelHeight, showDistance, false);
        } finally {
            if (depthTestWasEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }

    void renderBlockMarkers(WorldRenderContext context, Collection<Waypoint> waypoints, int color,
                            boolean beamsAlwaysThroughWalls) {
        if (waypoints.isEmpty()) return;

        prepareBuffers();
        var matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        boolean depthTestWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try {
            if (!beamsAlwaysThroughWalls) renderBeams(context, matrices, cameraPos, waypoints, color, BeamVisibility.NEAR);
            RenderSystem.disableDepthTest();
            if ((color >>> 24) != 0) renderBlockHighlights(matrices, cameraPos, waypoints, color);
            renderBeams(context, matrices, cameraPos, waypoints, color,
                    beamsAlwaysThroughWalls ? BeamVisibility.THROUGH_WALLS : BeamVisibility.FAR);
        } finally {
            if (depthTestWasEnabled) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    }

    private void renderBlockHighlights(MatrixStack matrices, Vec3d cameraPos, Collection<Waypoint> waypoints, int color) {
        var vertices = consumers.getBuffer(BlockHighlight.LAYER);
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = (color >>> 24) / 255.0F;
        for (Waypoint waypoint : waypoints) {
            double x = waypoint.x() - cameraPos.x - 0.5;
            double y = waypoint.y() - cameraPos.y;
            double z = waypoint.z() - cameraPos.z - 0.5;
            WorldRenderer.renderFilledBox(matrices, vertices, x, y, z, x + 1, y + 1, z + 1, red, green, blue, alpha);
        }
        consumers.draw();
    }

    private void renderBeams(WorldRenderContext context, MatrixStack matrices, Vec3d cameraPos,
                             Collection<Waypoint> waypoints, int islandColor, BeamVisibility visibility) {
        VertexConsumerProvider vertices = visibility == BeamVisibility.NEAR ? consumers : throughWallConsumers;
        float tickDelta = context.tickCounter().getTickDelta(false);
        long worldTime = context.world().getTime();
        for (Waypoint waypoint : waypoints) {
            if (visibility != BeamVisibility.THROUGH_WALLS) {
                double distanceSquared = cameraPos.squaredDistanceTo(waypoint.x(), waypoint.y(), waypoint.z());
                if ((distanceSquared > THROUGH_WALL_DISTANCE_SQUARED) != (visibility == BeamVisibility.FAR)) continue;
            }

            int rgb = waypoint.beamColorOverride() == null ? islandColor : waypoint.beamColorOverride();
            matrices.push();
            try {
                // The vanilla renderer adds a half block to its X and Z origin.
                matrices.translate(waypoint.x() - cameraPos.x - 0.5, waypoint.y() - cameraPos.y,
                        waypoint.z() - cameraPos.z - 0.5);
                BeaconBlockEntityRenderer.renderBeam(matrices, vertices, BeaconBlockEntityRenderer.BEAM_TEXTURE,
                        tickDelta, 1.0F, worldTime, 0, BEAM_HEIGHT, rgb | 0xFF000000, 0.2F, 0.3F);
            } finally {
                matrices.pop();
            }
        }
        consumers.draw();
    }

    private void renderLabels(MatrixStack matrices, Camera camera, Vec3d cameraPos, TextRenderer font,
                              List<Waypoint> waypoints, double labelHeight, boolean showDistance, boolean shadow) {
        for (Waypoint waypoint : waypoints) {
            renderLabel(matrices, camera, cameraPos, font, waypoint, labelHeight, showDistance, shadow);
        }
        consumers.draw();
    }

    private void renderLabel(MatrixStack matrices, Camera camera, Vec3d cameraPos, TextRenderer font,
                             Waypoint waypoint, double labelHeight, boolean showDistance, boolean shadow) {
        double x = waypoint.x() - cameraPos.x;
        double y = waypoint.y() - cameraPos.y;
        double z = waypoint.z() - cameraPos.z;
        double labelY = y + labelHeight;
        double labelDistance = Math.sqrt(x * x + labelY * labelY + z * z);
        double clamped = labelDistance > LABEL_RENDER_DISTANCE ? LABEL_RENDER_DISTANCE / labelDistance : 1.0;

        matrices.push();
        try {
            matrices.translate(x * clamped, labelY * clamped, z * clamped);
            matrices.multiply(camera.getRotation());
            matrices.scale(0.05F, -0.05F, 0.05F);
            drawText(matrices, font, waypoint.name(), 0, WHITE, shadow);
            if (showDistance) {
                String distance = Math.round(Math.sqrt(x * x + y * y + z * z)) + "m";
                drawText(matrices, font, distance, font.fontHeight + 1, YELLOW, shadow);
            }
        } finally {
            matrices.pop();
        }
    }

    private void drawText(MatrixStack matrices, TextRenderer font, String text, float y, int color, boolean shadow) {
        float offset = shadow ? 1.0F : 0.0F;
        int tint = shadow ? (color & 0xFF000000) | ((color & 0x00FCFCFC) >>> 2) : color;
        font.draw(text, -font.getWidth(text) / 2.0F + offset, y + offset, tint, false, matrices.peek().getPositionMatrix(),
                consumers, TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    private void prepareBuffers() {
        if (consumers != null) return;

        for (RenderLayer layer : List.of(BeamLayers.CORE, BeamLayers.GLOW,
                BeamLayers.THROUGH_WALL_CORE, BeamLayers.THROUGH_WALL_GLOW)) {
            beamBuffers.put(layer, new BufferAllocator(layer.getExpectedBufferSize()));
        }
        textBuffer = new BufferAllocator(4096);
        consumers = VertexConsumerProvider.immediate(beamBuffers, textBuffer);
        throughWallConsumers = layer -> consumers.getBuffer(
                layer == BeamLayers.CORE ? BeamLayers.THROUGH_WALL_CORE : BeamLayers.THROUGH_WALL_GLOW);
    }

    @Override
    public void close() {
        if (consumers == null) return;

        beamBuffers.values().forEach(BufferAllocator::close);
        beamBuffers.clear();
        textBuffer.close();
        textBuffer = null;
        consumers = null;
        throughWallConsumers = null;
    }

    private static final class BeamLayers {
        private static final RenderLayer CORE = RenderLayer.getBeaconBeam(BeaconBlockEntityRenderer.BEAM_TEXTURE, false);
        private static final RenderLayer GLOW = RenderLayer.getBeaconBeam(BeaconBlockEntityRenderer.BEAM_TEXTURE, true);
        private static final RenderLayer THROUGH_WALL_CORE = throughWall("nef_waypoint_beam_core", false);
        private static final RenderLayer THROUGH_WALL_GLOW = throughWall("nef_waypoint_beam_glow", true);

        private static RenderLayer throughWall(String name, boolean glow) {
            return RenderLayer.of(name, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL,
                    VertexFormat.DrawMode.QUADS, 1536, false, true,
                    RenderLayer.MultiPhaseParameters.builder()
                            .program(RenderPhase.BEACON_BEAM_PROGRAM)
                            .texture(new RenderPhase.Texture(BeaconBlockEntityRenderer.BEAM_TEXTURE, false, false))
                            .transparency(glow ? RenderPhase.TRANSLUCENT_TRANSPARENCY : RenderPhase.NO_TRANSPARENCY)
                            .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                            .writeMaskState(RenderPhase.COLOR_MASK)
                            .build(false));
        }
    }

    private enum BeamVisibility {
        NEAR, FAR, THROUGH_WALLS
    }

    private static final class BlockHighlight {
        private static final RenderLayer LAYER = RenderLayer.of("nef_waypoint_highlight",
                VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP, 1536, false, false,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.COLOR_PROGRAM)
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .build(false));
    }
}
