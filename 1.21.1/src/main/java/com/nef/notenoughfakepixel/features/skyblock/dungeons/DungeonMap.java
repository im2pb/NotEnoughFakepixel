package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.OverlayLayout;
import com.nef.notenoughfakepixel.config.position.OverlayPositionEditor;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class DungeonMap implements ClientModInitializer {
    private static final int MAP_SIZE = 128;
    private static final int CENTER = MAP_SIZE / 2;
    private static final int MAP_SLOT = 8;
    private static final int[] PARTY_COLORS = {0xFF4141FF, 0xFFFFFF00, 0xFFFF8000, 0xFFFF3030};
    @RegisterInstance
    private static final DungeonMap INSTANCE = new DungeonMap();

    private final ConfigColor borderColor = new ConfigColor();
    private NativeImageBackedTexture texture;
    private Identifier textureId;
    private byte[] pixels;
    private List<MapDecoration> decorations = List.of();
    private MapDecoration localMarker;
    private boolean extraStats;

    private DungeonMap() {
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> reset(client));
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::reset);
        ClientReceiveMessageEvents.GAME.register(this::onMessage);
        HudRenderCallback.EVENT.register(this::render);
    }

    private void update(MinecraftClient client) {
        if (!DungeonContext.active() || client.world == null || client.player == null) {
            reset(client);
            return;
        }
        if (!ClientConfig.configuration().dungeons.map.dungeonsMap) {
            releaseMap(client);
            return;
        }

        var stack = client.player.getInventory().getStack(MAP_SLOT);
        var id = stack.get(DataComponentTypes.MAP_ID);
        var state = stack.isOf(Items.FILLED_MAP) && id != null ? FilledMapItem.getMapState(id, client.world) : null;
        if (state == null || state.colors.length != MAP_SIZE * MAP_SIZE) {
            releaseMap(client);
            return;
        }

        if (!Arrays.equals(pixels, state.colors)) upload(client, state.colors);
        var markers = new ArrayList<MapDecoration>();
        state.getDecorations().forEach(markers::add);
        decorations = markers;
        localMarker = chooseLocalMarker(decorations, client.player.getGameProfile().getName());
    }

    private void upload(MinecraftClient client, byte[] colors) {
        if (texture == null) {
            texture = new NativeImageBackedTexture(MAP_SIZE, MAP_SIZE, false);
            textureId = client.getTextureManager().registerDynamicTexture("nef_dungeon_map", texture);
        }
        var image = texture.getImage();
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                image.setColor(x, y, MapColor.getRenderColor(colors[x + y * MAP_SIZE]));
            }
        }
        texture.upload();
        pixels = colors.clone();
    }

    private void onMessage(Text message, boolean overlay) {
        if (overlay || !DungeonContext.active()) return;
        for (String line : Formatting.strip(message.getString()).lines().toList()) {
            if (line.contains("> EXTRA STATS <")) extraStats = true;
            else if (line.strip().equals("[NPC] Mort: Good luck.")) extraStats = false;
        }
    }

    static MapDecoration chooseLocalMarker(List<MapDecoration> decorations, String playerName) {
        MapDecoration named = null;
        MapDecoration frame = null;
        boolean multipleFrames = false;
        for (MapDecoration marker : decorations) {
            boolean frameMarker = marker.type().equals(MapDecorationTypes.FRAME);
            if (frameMarker) {
                multipleFrames |= frame != null;
                frame = marker;
            }
            if ((frameMarker || partyMarker(marker)) && marker.name().filter(name ->
                    Formatting.strip(name.getString()).strip().equalsIgnoreCase(playerName)).isPresent()) {
                if (named != null) return null;
                named = marker;
            }
        }
        if (named != null) return named;
        return multipleFrames ? null : frame;
    }

    static float mapCoordinate(byte coordinate) {
        return CENTER + coordinate / 2.0F;
    }

    static float mapRotation(float yaw, boolean rotate, boolean extraStats, MapDecoration local) {
        return rotate && !extraStats && local != null ? MathHelper.wrapDegrees(180.0F - yaw) : 0.0F;
    }

    private void render(DrawContext context, RenderTickCounter tickCounter) {
        var client = MinecraftClient.getInstance();
        if (textureId == null || client.world == null || client.player == null || client.options.hudHidden
                || !DungeonContext.active() || client.currentScreen instanceof OverlayPositionEditor) return;
        var settings = ClientConfig.configuration().dungeons.map;
        if (!settings.dungeonsMap) return;
        OverlayLayout layout = layout(settings.dungeonsMapScale);
        var bounds = layout.bounds(settings.dungeonsMapPos, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        drawPanel(context, layout, bounds, settings.dungeonsMapBorderColor,
                client.player.getYaw(tickCounter.getTickDelta(false)), settings.dungeonsRotateMap);
    }

    public static void editPosition(Position position, float scale, String border, Consumer<Position> updatePosition) {
        OverlayLayout layout = layout(scale);
        OverlayPositionEditor.open("Dungeon Map", position, layout,
                (context, bounds) -> INSTANCE.drawPanel(context, layout, bounds, border, 0, false), updatePosition);
    }

    private static OverlayLayout layout(float scale) {
        return new OverlayLayout(MAP_SIZE, MAP_SIZE, scale, true, 0.1F, 10.0F);
    }

    private void drawPanel(DrawContext context, OverlayLayout layout, OverlayLayout.Bounds bounds, String border,
                           float yaw, boolean rotate) {
        int x = bounds.x();
        int y = bounds.y();
        int maxX = Math.min(x + bounds.width(), context.getScaledWindowWidth());
        int maxY = Math.min(y + bounds.height(), context.getScaledWindowHeight());
        context.enableScissor(x, y, maxX, maxY);
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(x, y, 0);
            matrices.scale(layout.scale(), layout.scale(), 1);
            if (textureId == null) drawPreview(context);
            else drawMap(context, yaw, rotate);
        } finally {
            matrices.pop();
            context.disableScissor();
        }
        context.drawBorder(x, y, bounds.width(), bounds.height(), borderColor.resolve(border));
    }

    private void drawMap(DrawContext context, float yaw, boolean rotate) {
        boolean rotating = rotate && !extraStats && localMarker != null;
        var matrices = context.getMatrices();
        matrices.push();
        try {
            if (rotating) {
                matrices.translate(CENTER, CENTER, 0);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(mapRotation(yaw, true, false, localMarker)));
                matrices.translate(-mapCoordinate(localMarker.x()), -mapCoordinate(localMarker.z()), 0);
            }
            context.drawTexturedQuad(textureId, 0, MAP_SIZE, 0, MAP_SIZE, 0, 0, 1, 0, 1, 1, 1, 1, 1);

            int colorIndex = 0;
            for (MapDecoration marker : decorations) {
                if (marker == localMarker) continue;
                boolean teammate = partyMarker(marker);
                int tint = teammate ? PARTY_COLORS[colorIndex++ % PARTY_COLORS.length] : 0xFFFFFFFF;
                drawMarker(context, marker, teammate, tint);
            }
        } finally {
            matrices.pop();
        }
        if (localMarker != null) {
            float markerX = rotating ? CENTER : mapCoordinate(localMarker.x());
            float markerY = rotating ? CENTER : mapCoordinate(localMarker.z());
            drawPlayerArrow(context, markerX, markerY, rotating ? 0 : MathHelper.wrapDegrees(yaw + 180.0F));
        }
    }

    private static boolean partyMarker(MapDecoration marker) {
        return marker.type().equals(MapDecorationTypes.PLAYER) || marker.type().equals(MapDecorationTypes.BLUE_MARKER);
    }

    private static void drawMarker(DrawContext context, MapDecoration marker, boolean teammate, int tint) {
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(mapCoordinate(marker.x()), mapCoordinate(marker.z()), 1);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((marker.rotation() & 15) * 22.5F));
            var spriteMarker = teammate && marker.type().equals(MapDecorationTypes.BLUE_MARKER)
                    ? new MapDecoration(MapDecorationTypes.PLAYER, (byte) 0, (byte) 0, (byte) 0, Optional.empty()) : marker;
            var sprite = MinecraftClient.getInstance().getMapDecorationsAtlasManager().getSprite(spriteMarker);
            int radius = teammate ? 5 : 4;
            context.drawSprite(-radius, -radius, 0, radius * 2, radius * 2, sprite,
                    ((tint >>> 16) & 255) / 255.0F, ((tint >>> 8) & 255) / 255.0F, (tint & 255) / 255.0F, 1);
        } finally {
            matrices.pop();
        }
    }

    private static void drawPlayerArrow(DrawContext context, float x, float y, float angle) {
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(x, y, 2);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            context.fill(-1, -5, 1, -3, 0xFF72F56B);
            context.fill(-2, -3, 2, -1, 0xFF72F56B);
            context.fill(-3, -1, 3, 1, 0xFF72F56B);
            context.fill(-1, 1, 1, 5, 0xFF72F56B);
        } finally {
            matrices.pop();
        }
    }

    private static void drawPreview(DrawContext context) {
        context.fill(0, 0, MAP_SIZE, MAP_SIZE, 0xFF262A2D);
        context.fill(14, 17, 43, 48, 0xFF548A54);
        context.fill(54, 17, 83, 48, 0xFF9D774A);
        context.fill(43, 28, 54, 37, 0xFF9D774A);
        context.fill(54, 48, 83, 103, 0xFF9D774A);
        context.fill(94, 65, 116, 94, 0xFF785E9A);
        context.fill(83, 74, 94, 84, 0xFF785E9A);
        drawPlayerArrow(context, CENTER, CENTER, 0);
    }

    private void reset(MinecraftClient client) {
        extraStats = false;
        releaseMap(client);
    }

    private void releaseMap(MinecraftClient client) {
        if (textureId != null) client.getTextureManager().destroyTexture(textureId);
        textureId = null;
        texture = null;
        pixels = null;
        decorations = List.of();
        localMarker = null;
    }
}
