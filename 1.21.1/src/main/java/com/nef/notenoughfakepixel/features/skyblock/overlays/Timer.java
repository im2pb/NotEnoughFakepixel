package com.nef.notenoughfakepixel.features.skyblock.overlays;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.OverlayLayout;
import com.nef.notenoughfakepixel.config.position.OverlayLayout.Bounds;
import com.nef.notenoughfakepixel.config.position.OverlayPositionEditor;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/** Renders a countdown to an epoch timestamp using the subclass's visibility and layout settings. */
public abstract class Timer implements ClientModInitializer {
    private static final int ICON_GAP = 3;
    private final Identifier icon;
    private long cachedSeconds = -1;
    private String cachedText;

    protected Timer(Identifier icon) {
        this.icon = icon;
    }

    @Override
    public final void onInitializeClient() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
        registerEvents();
    }

    protected void registerEvents() {
    }

    protected abstract boolean shouldShow(Configuration configuration);

    protected abstract Position position(Configuration configuration);

    protected abstract float scale(Configuration configuration);

    protected abstract long goalEpochMillis(long now);

    protected int textColor(long remainingMillis) {
        if (remainingMillis < 0) return 0xFFFFFF55;
        if (remainingMillis < 5_000) return 0xFF55FF55;
        return 0xFFFFFFFF;
    }

    private void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden || !SkyblockData.isSkyblock()) return;

        Configuration configuration = ClientConfig.configuration();
        if (!shouldShow(configuration)) return;

        long now = System.currentTimeMillis();
        long goal = goalEpochMillis(now);
        if (goal <= 0) return;

        long remaining = goal - now;
        long seconds = Math.max(0, remaining) / 1_000;
        if (seconds != cachedSeconds) {
            cachedSeconds = seconds;
            cachedText = formatSeconds(seconds);
        }

        TextRenderer font = client.textRenderer;
        OverlayLayout layout = layout(font, icon, cachedText, scale(configuration));
        Bounds bounds = layout.bounds(position(configuration), context.getScaledWindowWidth(), context.getScaledWindowHeight());
        draw(context, font, icon, cachedText, textColor(remaining), layout, bounds);
    }

    static String formatSeconds(long seconds) {
        long minutes = seconds / 60;
        long remainder = seconds % 60;
        return (minutes < 10 ? "0" : "") + minutes + (remainder < 10 ? ":0" : ":") + remainder;
    }

    protected static void editPosition(String name, Identifier icon, Position position, float scale, int color,
                                       Consumer<Position> updatePosition) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        String preview = "05:30";
        OverlayLayout layout = layout(font, icon, preview, scale);
        OverlayPositionEditor.open(name, position, layout,
                (context, bounds) -> draw(context, font, icon, preview, color, layout, bounds), updatePosition);
    }

    protected static void notifyPlayer(MinecraftClient client, String message, Formatting color, boolean playSound) {
        if (client.player == null) return;
        if (playSound) client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
        client.inGameHud.setTitleTicks(2, 70, 2);
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitle(Text.literal(message).formatted(color));
    }

    private static OverlayLayout layout(TextRenderer font, Identifier icon, String text, float scale) {
        int iconWidth = icon == null ? 0 : font.fontHeight + ICON_GAP;
        return new OverlayLayout(iconWidth + font.getWidth(text) + 1, font.fontHeight + 1, scale, true);
    }

    private static void draw(DrawContext context, TextRenderer font, Identifier icon, String text, int color,
                             OverlayLayout layout, Bounds bounds) {
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(bounds.x(), bounds.y(), 0);
            matrices.scale(layout.scale(), layout.scale(), 1);
            int x = 0;
            if (icon != null) {
                int size = font.fontHeight;
                context.drawTexture(icon, 0, 0, 0.0F, 0.0F, size, size, size, size);
                x = size + ICON_GAP;
            }
            context.drawTextWithShadow(font, text, x, 0, color);
        } finally {
            matrices.pop();
        }
    }
}
