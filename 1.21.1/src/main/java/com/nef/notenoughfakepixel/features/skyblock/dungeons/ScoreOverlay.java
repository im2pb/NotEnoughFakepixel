package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.OverlayLayout;
import com.nef.notenoughfakepixel.config.position.OverlayPositionEditor;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.variables.Mayor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

public final class ScoreOverlay implements ClientModInitializer {
    private static final int PADDING = 2;
    private static final DungeonScore PREVIEW = new DungeonScore(100, 96, 100, 5, 91.5, 87.5, 100);

    @RegisterInstance
    private static final ScoreOverlay INSTANCE = new ScoreOverlay();

    private final ConfigColor background = new ConfigColor();
    private int failedPuzzles;
    private boolean spiritPet;
    private boolean bloodOpen;
    private boolean finished;
    private DungeonScore score = DungeonScore.EMPTY;
    private List<Text> simpleLines = lines(DungeonScore.EMPTY, true);
    private List<Text> detailedLines = lines(DungeonScore.EMPTY, false);

    private ScoreOverlay() {
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> reset());
        ClientReceiveMessageEvents.GAME.register(this::onMessage);
        HudRenderCallback.EVENT.register((context, ticks) -> render(context));
    }

    private void update(MinecraftClient client) {
        if (client.player == null || client.world == null || !DungeonContext.active()) {
            reset();
            return;
        }
        boolean paul = ClientConfig.configuration().dungeons.general.dungeonsIsPaul || SkyblockData.isMayor(Mayor.PAUL);
        var estimate = DungeonScore.estimate(SkyblockData.currentFloor(), SkyblockData.clearedPercentage(),
                SkyblockData.dungeonRunStats(), failedPuzzles, spiritPet, bloodOpen, paul);
        if (!finished || score.total() < 0 || estimate.total() >= 0) updateScore(estimate);
    }

    private void onMessage(Text message, boolean overlay) {
        if (overlay || !DungeonContext.active()) return;

        for (String raw : Formatting.strip(message.getString()).lines().toList()) {
            String line = raw.strip();
            if (line.equals("[NPC] Mort: Good luck.")) reset();
            else if (line.startsWith("PUZZLE FAIL!")) failedPuzzles++;
            else if (line.equals("Your Spirit Pet reduced your death score penalty to 1!")) spiritPet = true;
            else if (line.endsWith("The BLOOD DOOR has been opened!") && !line.contains(" > ")) bloodOpen = true;
            else if (line.contains("> EXTRA STATS <")) finished = true;
        }
    }

    private void reset() {
        failedPuzzles = 0;
        spiritPet = bloodOpen = finished = false;
        updateScore(DungeonScore.EMPTY);
    }

    private void updateScore(DungeonScore value) {
        if (score.equals(value)) return;

        score = value;
        simpleLines = lines(value, true);
        detailedLines = lines(value, false);
    }

    private void render(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !DungeonContext.active() || client.options.hudHidden
                || client.currentScreen instanceof OverlayPositionEditor) return;

        var settings = ClientConfig.configuration().dungeons.scoreSecrets;
        if (!settings.dungeonsScoreOverlay) return;
        var lines = settings.dungeonsScoreSimple ? simpleLines : detailedLines;
        var layout = layout(client.textRenderer, lines, settings.scoreOverlayScale);
        var bounds = layout.bounds(settings.scoreOverlayPosition, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        draw(context, client.textRenderer, lines, layout, bounds, background.resolve(settings.scoreOverlayBackgroundColor));
    }

    public static void editPosition(Position position, float scale, String background, boolean simple,
                                    Consumer<Position> updatePosition) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        var preview = INSTANCE.score.total() >= 0
                ? (simple ? INSTANCE.simpleLines : INSTANCE.detailedLines) : lines(PREVIEW, simple);
        var layout = layout(font, preview, scale);
        OverlayPositionEditor.open("Dungeon Score Overlay", position, layout,
                (context, bounds) -> draw(context, font, preview, layout, bounds, INSTANCE.background.resolve(background)),
                updatePosition);
    }

    static List<Text> lines(DungeonScore score, boolean simple) {
        Text total = totalLine(score.total());
        Text secrets = secretLine(score);
        if (simple) return List.of(total, Text.empty(), secrets);

        return List.of(total, Text.empty(), points("Skill", score.skill(), 100, 94),
                points("Exploration", score.exploration(), 100, 90), points("Speed", score.speed(), 100, 90),
                points("Bonus", score.bonus(), score.bonus() >= 10 ? 15 : 5, score.bonus() >= 10 ? 10 : 1),
                Text.empty(), secrets);
    }

    private static Text totalLine(int total) {
        if (total < 0) return points("Estimated score", total, 300, 270);

        String rank;
        Formatting color;
        if (total < 100) {
            rank = "D";
            color = Formatting.RED;
        } else if (total < 160) {
            rank = "C";
            color = Formatting.BLUE;
        } else if (total < 230) {
            rank = "B";
            color = Formatting.GREEN;
        } else if (total < 270) {
            rank = "A";
            color = Formatting.LIGHT_PURPLE;
        } else {
            rank = total < 300 ? "S" : "S+";
            color = Formatting.GOLD;
        }
        return Text.literal("Estimated score ").formatted(Formatting.GRAY)
                .append(Text.literal(Integer.toString(total)).formatted(total >= 300 ? Formatting.GREEN
                        : total >= 230 ? Formatting.YELLOW : Formatting.RED))
                .append(Text.literal(" (" + rank + ")").formatted(color));
    }

    private static Text points(String label, int points, int good, int warning) {
        Formatting color = points < 0 ? Formatting.DARK_GRAY : points >= good ? Formatting.GREEN
                : points >= warning ? Formatting.YELLOW : Formatting.RED;
        return Text.literal(label + " ").formatted(Formatting.GRAY)
                .append(Text.literal(points < 0 ? "—" : Integer.toString(points)).formatted(color));
    }

    private static Text secretLine(DungeonScore score) {
        double secrets = score.secretsPercent();
        double target = score.sPlusSecretTarget();
        Formatting progress = secrets < 0 ? Formatting.DARK_GRAY : target >= 0 && secrets >= target
                ? Formatting.GREEN : Formatting.YELLOW;
        var line = Text.literal("Secrets ").formatted(Formatting.GRAY)
                .append(Text.literal(percent(secrets)).formatted(progress));
        if (target == DungeonScore.INSUFFICIENT_POINTS) {
            return line.append(Text.literal("  S+ needs more points").formatted(Formatting.RED));
        }
        return line.append(Text.literal(" / ").formatted(Formatting.GRAY))
                .append(Text.literal(percent(target)).formatted(progress))
                .append(Text.literal(" for S+ / " + percent(score.floorSecretPercent()) + " cap").formatted(Formatting.GRAY));
    }

    private static String percent(double value) {
        if (value < 0) return "—";
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString() + "%";
    }

    private static OverlayLayout layout(TextRenderer font, List<Text> lines, float scale) {
        int width = lines.stream().mapToInt(font::getWidth).max().orElse(0) + 2 * PADDING;
        int height = 2 * PADDING + lines.size() * (font.fontHeight + 2) - 2;
        return new OverlayLayout(width, height, scale, false);
    }

    private static void draw(DrawContext context, TextRenderer font, List<Text> lines, OverlayLayout layout,
                             OverlayLayout.Bounds bounds, int color) {
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(bounds.x(), bounds.y(), 0);
            matrices.scale(layout.scale(), layout.scale(), 1);
            if ((color >>> 24) != 0) context.fill(0, 0, layout.width(), layout.height(), color);
            int y = PADDING;
            for (Text line : lines) {
                context.drawTextWithShadow(font, line, PADDING, y, 0xFFFFFFFF);
                y += font.fontHeight + 2;
            }
        } finally {
            matrices.pop();
        }
    }
}
