package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.OverlayLayout;
import com.nef.notenoughfakepixel.config.position.OverlayPositionEditor;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Slayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.Consumer;

@RegisterEvents
public final class SlayerHealthDisplay implements ClientModInitializer {
    private static final double MAX_RANGE_SQUARED = 32 * 32;
    private static final int LINE_GAP = 2;
    private static final float SCALE = 2f;
    private static final List<Text> PREVIEW = List.of(Text.literal("☠ Voidgloom Seraph ").formatted(Formatting.AQUA)
            .append(Text.literal("12.5M").formatted(Formatting.GREEN))
            .append(Text.literal("❤").formatted(Formatting.RED)));

    private List<Text> lines = List.of();

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> lines = List.of());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || !enabled()) {
            lines = List.of();
            return;
        }

        Slayer family = SkyblockData.currentSlayer();
        if (family == Slayer.INFERNO) {
            var blaze = BlazeSlayerDisplay.INSTANCE.healthLines(client.player);
            if (!blaze.isEmpty()) {
                lines = blaze;
                return;
            }
        }

        Text closest = null;
        double nearest = MAX_RANGE_SQUARED;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity stand) || !stand.isAlive()) continue;
            var label = stand.getCustomName();
            if (label == null) continue;
            double distance = client.player.squaredDistanceTo(stand);
            if (distance >= nearest) continue;
            Slayer match = SlayerMobsDisplay.bossFamily(label.getString());
            if (match == Slayer.NONE || (family != Slayer.NONE && match != family)) continue;

            nearest = distance;
            closest = label;
        }
        lines = closest == null ? List.of() : List.of(closest);
    }

    private void render(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (lines.isEmpty() || !enabled() || client.options.hudHidden
                || client.currentScreen instanceof OverlayPositionEditor) return;

        var font = client.textRenderer;
        var layout = layout(font, lines);
        var position = ClientConfig.configuration().slayer.bossSettings.slayerBossHPPos;
        var bounds = layout.bounds(position, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        draw(context, font, lines, bounds);
    }

    public static void editPosition(Position position, Consumer<Position> updatePosition) {
        var font = MinecraftClient.getInstance().textRenderer;
        OverlayPositionEditor.open("Slayer Boss Health", position, layout(font, PREVIEW),
                (context, bounds) -> draw(context, font, PREVIEW, bounds), updatePosition);
    }

    private static OverlayLayout layout(TextRenderer font, List<Text> lines) {
        int width = lines.stream().mapToInt(font::getWidth).max().orElse(0);
        int height = (font.fontHeight + LINE_GAP) * lines.size() - LINE_GAP;
        return new OverlayLayout(width + 1, height + 1, SCALE, false);
    }

    private static void draw(DrawContext context, TextRenderer font, List<Text> lines, OverlayLayout.Bounds bounds) {
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(bounds.x(), bounds.y(), 0);
            matrices.scale(SCALE, SCALE, 1);
            int y = 0;
            for (Text line : lines) {
                context.drawTextWithShadow(font, line, 0, y, 0xFFFFFFFF);
                y += font.fontHeight + LINE_GAP;
            }
        } finally {
            matrices.pop();
        }
    }

    private static boolean enabled() {
        return SkyblockData.isSkyblock() && SkyblockData.bossActive()
                && ClientConfig.configuration().slayer.bossSettings.slayerBossHP;
    }
}
