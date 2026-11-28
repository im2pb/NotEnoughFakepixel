package com.nef.notenoughfakepixel.features.skyblock.crimson;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.OverlayLayout;
import com.nef.notenoughfakepixel.config.position.OverlayPositionEditor;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

@RegisterEvents
public final class AshfangOverlay implements ClientModInitializer {
    private static final AshfangHelper.Stats PREVIEW = new AshfangHelper.Stats(
            new AshfangHelper.Health(31_000_000, 50_000_000), 4);

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((context, ticks) -> render(context));
    }

    private void render(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (!AshfangHelper.inArena(client) || client.options.hudHidden
                || client.currentScreen instanceof OverlayPositionEditor) return;
        var settings = ClientConfig.configuration().crimson.ashfangSettings;
        var stats = AshfangHelper.INSTANCE.stats();
        if (!settings.crimsonAshfangOverlay || stats == null) return;

        var lines = lines(stats);
        var font = client.textRenderer;
        var bounds = layout(font, lines).bounds(settings.ashfangOverlayPos, context.getScaledWindowWidth(),
                context.getScaledWindowHeight());
        draw(context, font, lines, bounds);
    }

    public static void editPosition(Position position, Consumer<Position> updatePosition) {
        var font = MinecraftClient.getInstance().textRenderer;
        var lines = lines(PREVIEW);
        OverlayPositionEditor.open("Ashfang Overlay", position, layout(font, lines),
                (context, bounds) -> draw(context, font, lines, bounds), updatePosition);
    }

    private static List<Text> lines(AshfangHelper.Stats stats) {
        var hp = stats.health();
        double fraction = hp.current() / hp.max();
        Formatting color = fraction > 0.5 ? Formatting.GREEN : fraction > 0.1 ? Formatting.YELLOW : Formatting.RED;
        return List.of(
                Text.literal("Ashfang HP: ").formatted(Formatting.GRAY)
                        .append(Text.literal(compact(hp.current())).formatted(color)),
                Text.literal("Blazing souls: ").formatted(Formatting.GRAY)
                        .append(Text.literal(stats.souls() + " / " + stats.hitsNeeded()).formatted(Formatting.WHITE)));
    }

    private static String compact(double hp) {
        double divisor = hp >= 1_000_000_000 ? 1_000_000_000 : hp >= 1_000_000 ? 1_000_000 : hp >= 1_000 ? 1_000 : 1;
        String suffix = divisor == 1_000_000_000 ? "B" : divisor == 1_000_000 ? "M" : divisor == 1_000 ? "k" : "";
        return BigDecimal.valueOf(Math.round(hp / divisor * 10), 1).stripTrailingZeros().toPlainString() + suffix;
    }

    private static OverlayLayout layout(TextRenderer font, List<Text> lines) {
        int width = lines.stream().mapToInt(font::getWidth).max().orElse(0);
        return new OverlayLayout(width + 1, (font.fontHeight + 1) * lines.size(), 1, false);
    }

    private static void draw(DrawContext context, TextRenderer font, List<Text> lines, OverlayLayout.Bounds bounds) {
        int y = bounds.y();
        for (Text line : lines) {
            context.drawTextWithShadow(font, line, bounds.x(), y, 0xFFFFFFFF);
            y += font.fontHeight + 1;
        }
    }
}
