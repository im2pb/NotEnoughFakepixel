package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Mining;
import com.nef.notenoughfakepixel.config.position.OverlayLayout;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@RegisterEvents
public final class MiningOverlay implements ClientModInitializer {
    private static final int PADDING = 2;
    private static final long COOLDOWN_MILLIS = 120_000;
    private static final Pattern ABILITY_USED = Pattern.compile("You used your .+ Pickaxe Ability!");
    private static final Pattern COMMISSION_PERCENT = Pattern.compile("(.+?):\\s*(\\d+(?:\\.\\d+)?)\\s*%");

    private final ConfigColor background = new ConfigColor();
    private long readyAtMillis = Long.MIN_VALUE;
    private boolean reminderPending;
    private DrillFuel drillFuel;

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register(this::render);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            readyAtMillis = Long.MIN_VALUE;
            reminderPending = false;
            drillFuel = null;
        }));
    }

    private void onGameMessage(Text message, boolean overlay) {
        if (inMiningIsland() && ABILITY_USED.matcher(Formatting.strip(message.getString())).find()) {
            readyAtMillis = Util.getMeasuringTimeMs() + COOLDOWN_MILLIS;
            reminderPending = true;
        }
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || !inMiningIsland()) {
            drillFuel = null;
            return;
        }

        Mining mining = ClientConfig.configuration().mining;
        drillFuel = mining.miningOverlay && mining.miningOverlaySettings.miningDrillFuel
                ? DrillFuel.read(DrillFix.INSTANCE.latestHeldStack(client)) : null;

        if (reminderPending && remainingMillis() == 0) {
            reminderPending = false;
            if (mining.miningAbilityNotifier) {
                client.inGameHud.setTitleTicks(2, 70, 2);
                client.inGameHud.setSubtitle(Text.empty());
                client.inGameHud.setTitle(Text.literal("Mining Ability Ready").formatted(Formatting.GOLD));
            }
        }
    }

    private void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || !inMiningIsland() || client.options.hudHidden
                || client.options.playerListKey.isPressed() || client.getDebugHud().shouldShowDebugHud()) return;

        Mining mining = ClientConfig.configuration().mining;
        if (!mining.miningOverlay || (mining.miningOverlayHideOnChat && client.currentScreen instanceof ChatScreen)) return;

        var settings = mining.miningOverlaySettings;
        List<String> lines = lines(settings);
        if (lines.isEmpty()) return;

        var font = client.textRenderer;
        int lineHeight = font.fontHeight + 2;
        int width = 20;
        for (String line : lines) width = Math.max(width, font.getWidth(line) + PADDING * 2);
        OverlayLayout layout = new OverlayLayout(width, lines.size() * lineHeight + PADDING, settings.miningOverlayScale, false);
        Position position = new Position(Math.round(settings.miningOverlayOffsetX), Math.round(settings.miningOverlayOffsetY));
        var bounds = layout.bounds(position, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        int color = background.resolve(settings.miningOverlayBackgroundColor);

        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(bounds.x(), bounds.y(), 0);
            matrices.scale(layout.scale(), layout.scale(), 1);
            if ((color >>> 24) != 0) context.fill(0, 0, layout.width(), layout.height(), color);
            int y = PADDING;
            for (String line : lines) {
                context.drawTextWithShadow(font, line, PADDING, y, 0xFFFFFFFF);
                y += lineHeight;
            }
        } finally {
            matrices.pop();
        }
    }

    private List<String> lines(Mining.MiningOverlaySettings settings) {
        List<String> lines = new ArrayList<>();
        Location location = SkyblockData.currentLocation();
        if (settings.miningAbilityCooldown) lines.add(cooldownLine(remainingMillis()));
        if (settings.miningMithrilPowder && location == Location.DWARVEN) {
            lines.add(Formatting.GRAY + "Mithril Powder: " + Formatting.DARK_GREEN + SkyblockData.mithrilPowder());
        }
        if (settings.miningGemstonePowder && location == Location.CRYSTAL_HOLLOWS) {
            lines.add(Formatting.GRAY + "Gemstone Powder: " + Formatting.LIGHT_PURPLE + SkyblockData.gemstonePowder());
        }
        if (settings.miningDrillFuel && drillFuel != null) lines.add(drillFuel.line());
        if (settings.miningCommissions) {
            for (String commission : SkyblockData.commissions()) lines.add(commissionLine(commission));
        }
        return lines;
    }

    private static boolean inMiningIsland() {
        Location location = SkyblockData.currentLocation();
        return SkyblockData.isSkyblock() && (location == Location.DWARVEN || location == Location.CRYSTAL_HOLLOWS);
    }

    private long remainingMillis() {
        long now = Util.getMeasuringTimeMs();
        return readyAtMillis > now ? readyAtMillis - now : 0;
    }

    static String cooldownLine(long remainingMillis) {
        String value = remainingMillis > 0
                ? Formatting.RED.toString() + Math.ceilDiv(remainingMillis, 1000) + "s" : Formatting.GREEN + "Ready";
        return Formatting.GRAY + "Ability Cooldown: " + value;
    }

    static String commissionLine(String commission) {
        var match = COMMISSION_PERCENT.matcher(commission);
        if (!match.matches()) return commission;

        String percentText = match.group(2);
        double percent = Double.parseDouble(percentText);
        Formatting color = percent <= 33 ? Formatting.RED : percent <= 79 ? Formatting.YELLOW : Formatting.GREEN;
        return Formatting.GRAY + match.group(1).strip() + ": " + color + percentText + "%";
    }
}
