package com.nef.notenoughfakepixel.features.skyblock.garden;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Garden;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

@RegisterEvents
public final class GardenOverlay implements ClientModInitializer {
    private static final int PADDING = 2;

    public enum Panel {
        PESTS("Pests Overlay", List.of("\u00a7cPests: \u00a7f4")),
        VISITORS("Visitors Overlay", List.of("\u00a7bVisitors \u00a7f(1)", " \u00a7aFarmhand")),
        CROP_MILESTONE("Crop Milestone Overlay", List.of("\u00a76Crop Milestone", " \u00a7eWheat \u00a7f5,000 / 10,000"));

        final String title;
        final List<String> preview;
        final ConfigColor background = new ConfigColor();

        Panel(String title, List<String> preview) {
            this.title = title;
            this.preview = preview;
        }
    }

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !inGarden() || client.options.hudHidden
                || client.options.playerListKey.isPressed() || client.getDebugHud().shouldShowDebugHud()
                || client.currentScreen instanceof ChatScreen) return;

        Garden settings = ClientConfig.configuration().garden;
        if (!settings.enable) return;

        var pests = settings.pestsOverlay;
        if (pests.enabled) {
            drawHudPanel(context, client.textRenderer, Panel.PESTS, pests.pos, pests.scale, pests.backgroundColor,
                    List.of(pestLine(SkyblockData.activePests().values())));
        }
        var visitors = settings.visitorsOverlay;
        if (visitors.enabled) {
            drawHudPanel(context, client.textRenderer, Panel.VISITORS, visitors.pos, visitors.scale,
                    visitors.backgroundColor, visitorLines(SkyblockData.activeVisitors()));
        }
        var crop = settings.cropMilestoneOverlay;
        if (crop.enabled && !SkyblockData.cropMilestone().isEmpty()) {
            drawHudPanel(context, client.textRenderer, Panel.CROP_MILESTONE, crop.pos, crop.scale,
                    crop.backgroundColor, SkyblockData.cropMilestone());
        }
    }

    static boolean inGarden() {
        return SkyblockData.isSkyblock() && SkyblockData.currentLocation() == Location.GARDEN;
    }

    static String pestLine(Collection<Integer> counts) {
        long total = 0;
        for (int count : counts) {
            if (count > 0) total += count;
        }
        return "\u00a7cPests: \u00a7f" + total;
    }

    static List<String> visitorLines(List<String> visitors) {
        List<String> lines = new ArrayList<>(visitors.size() + 1);
        lines.add("\u00a7bVisitors \u00a7f(" + visitors.size() + ")");
        for (String visitor : visitors) lines.add(" " + visitor);
        return lines;
    }

    public static void editPosition(Panel panel, Position position, float scale, String background,
                                    Consumer<Position> updatePosition) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new GardenPositionEditor(client.currentScreen, panel, position, scale, background, updatePosition));
    }

    private static void drawHudPanel(DrawContext context, TextRenderer font, Panel panel, Position position, float scale,
                                     String background, List<String> lines) {
        Layout layout = layout(font, lines, position, scale, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        drawPanel(context, font, lines, layout, panel.background.resolve(background));
    }

    static Layout layout(TextRenderer font, List<String> lines, Position position, float scale,
                         int viewportWidth, int viewportHeight) {
        int width = PADDING * 2;
        for (String line : lines) width = Math.max(width, font.getWidth(line) + PADDING * 2);
        int height = PADDING * 2 + lines.size() * (font.fontHeight + 1) - 1;
        float safeScale = Float.isFinite(scale) ? Math.clamp(scale, 0.5F, 5.0F) : 1.0F;
        int scaledWidth = (int) Math.ceil(width * safeScale);
        int scaledHeight = (int) Math.ceil(height * safeScale);
        int x = axisStart(position.x(), position.centerX(), viewportWidth, scaledWidth);
        int y = axisStart(position.y(), position.centerY(), viewportHeight, scaledHeight);
        return new Layout(x, y, width, height, scaledWidth, scaledHeight, safeScale);
    }

    static int axisStart(int offset, boolean centered, int viewport, int panelSize) {
        int end = viewport - panelSize;
        long start = centered ? viewport / 2L + offset : offset < 0 ? (long) end + offset : offset;
        return (int) Math.clamp(start, 0, Math.max(0, end));
    }

    static void drawPanel(DrawContext context, TextRenderer font, List<String> lines, Layout layout, int color) {
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(layout.x(), layout.y(), 0);
            matrices.scale(layout.scale(), layout.scale(), 1);
            if ((color >>> 24) != 0) context.fill(0, 0, layout.width(), layout.height(), color);
            int y = PADDING;
            for (String line : lines) {
                context.drawTextWithShadow(font, line, PADDING, y, 0xFFFFFFFF);
                y += font.fontHeight + 1;
            }
        } finally {
            matrices.pop();
        }
    }

    record Layout(int x, int y, int width, int height, int scaledWidth, int scaledHeight, float scale) {
    }
}
