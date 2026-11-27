package com.nef.notenoughfakepixel.features.skyblock.garden;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Garden;
import com.nef.notenoughfakepixel.config.position.OverlayLayout;
import com.nef.notenoughfakepixel.config.position.OverlayLayout.Bounds;
import com.nef.notenoughfakepixel.config.position.OverlayPositionEditor;
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
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        OverlayLayout layout = layout(font, panel.preview, scale);
        OverlayPositionEditor.open(panel.title, position, layout,
                (context, bounds) -> drawPanel(context, font, panel.preview, layout, bounds, panel.background.resolve(background)),
                updatePosition);
    }

    private static void drawHudPanel(DrawContext context, TextRenderer font, Panel panel, Position position, float scale,
                                     String background, List<String> lines) {
        OverlayLayout layout = layout(font, lines, scale);
        Bounds bounds = layout.bounds(position, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        drawPanel(context, font, lines, layout, bounds, panel.background.resolve(background));
    }

    private static OverlayLayout layout(TextRenderer font, List<String> lines, float scale) {
        int width = PADDING * 2;
        for (String line : lines) width = Math.max(width, font.getWidth(line) + PADDING * 2);
        int height = PADDING * 2 + lines.size() * (font.fontHeight + 1) - 1;
        return new OverlayLayout(width, height, scale, false);
    }

    private static void drawPanel(DrawContext context, TextRenderer font, List<String> lines, OverlayLayout layout,
                                  Bounds bounds, int color) {
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(bounds.x(), bounds.y(), 0);
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

}
