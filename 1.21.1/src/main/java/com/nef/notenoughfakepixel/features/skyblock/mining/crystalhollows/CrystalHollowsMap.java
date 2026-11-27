package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.OverlayLayout;
import com.nef.notenoughfakepixel.config.position.OverlayLayout.Bounds;
import com.nef.notenoughfakepixel.config.position.OverlayPositionEditor;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;

@RegisterEvents
public final class CrystalHollowsMap implements ClientModInitializer {
    private static final Identifier ZONES = Identifier.of("notenoughfakepixel", "crystalhollows/map.png");
    private static final Identifier GEMSTONES = Identifier.of("notenoughfakepixel", "crystalhollows/map_gems.png");
    private static final Identifier ARROW = Identifier.of("notenoughfakepixel", "crystalhollows/map_arrow.png");
    private static final Identifier EDGE_MARKER = Identifier.of("notenoughfakepixel", "crystalhollows/map_point.png");
    private static final int MAP_TEXTURE_SIZE = 256;
    private static final int MARKER_TEXTURE_SIZE = 16;
    private static final int MARKER_SIZE = 12;
    private static final int MARGIN = MARKER_SIZE / 2;
    private static final double WORLD_MIN = 202;
    private static final double WORLD_MAX = 823;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext context, RenderTickCounter tickCounter) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !SkyblockData.isSkyblock()
                || SkyblockData.currentLocation() != Location.CRYSTAL_HOLLOWS || client.options.hudHidden
                || client.options.playerListKey.isPressed() || client.getDebugHud().shouldShowDebugHud()
                || client.currentScreen instanceof OverlayPositionEditor) return;

        var mining = ClientConfig.configuration().mining;
        if (!mining.crystalHollows.miningCrystalMap
                || (mining.miningOverlayHideOnChat && client.currentScreen instanceof ChatScreen)) return;

        var settings = mining.crystalHollowsMap;
        OverlayLayout layout = layout(settings.miningCrystalMapWidth);
        Bounds bounds = layout.bounds(settings.crystalMapPos, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        float delta = tickCounter.getTickDelta(false);
        var playerPos = client.player.getLerpedPos(delta);
        draw(context, bounds, texture(settings.miningCrystalMapType), playerPos.x, playerPos.z, client.player.getYaw(delta));
    }

    public static void editPosition(Position position, int width, String type, Consumer<Position> updatePosition) {
        double center = (WORLD_MIN + WORLD_MAX) / 2;
        OverlayPositionEditor.open("Crystal Hollows Map", position, layout(width),
                (context, bounds) -> draw(context, bounds, texture(type), center, center, 180), updatePosition);
    }

    private static OverlayLayout layout(int width) {
        int size = Math.clamp(width, 32, 160) + 2 * MARGIN;
        return new OverlayLayout(size, size, 1, false);
    }

    private static Identifier texture(String type) {
        return "Zones".equals(type) ? ZONES : GEMSTONES;
    }

    private static void draw(DrawContext context, Bounds bounds, Identifier texture, double worldX, double worldZ, float yaw) {
        int x = bounds.x() + MARGIN;
        int y = bounds.y() + MARGIN;
        int width = bounds.width() - 2 * MARGIN;
        float markerX = x + mapOffset(worldX, width);
        float markerY = y + mapOffset(worldZ, width);
        Identifier marker = worldX < WORLD_MIN || worldX > WORLD_MAX || worldZ < WORLD_MIN || worldZ > WORLD_MAX
                ? EDGE_MARKER : ARROW;

        var matrices = context.getMatrices();
        boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        matrices.push();
        try {
            context.drawTexture(texture, x, y, width, width, 0, 0, MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE,
                    MAP_TEXTURE_SIZE, MAP_TEXTURE_SIZE);
            matrices.translate(markerX, markerY, 0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw - 180));
            context.drawTexture(marker, -MARGIN, -MARGIN, MARKER_SIZE, MARKER_SIZE, 0, 0, MARKER_TEXTURE_SIZE,
                    MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE, MARKER_TEXTURE_SIZE);
        } finally {
            matrices.pop();
            if (!blendWasEnabled) RenderSystem.disableBlend();
        }
    }

    static float mapOffset(double coordinate, int width) {
        return (float) (Math.clamp((coordinate - WORLD_MIN) / (WORLD_MAX - WORLD_MIN), 0, 1) * width);
    }
}
