package com.nef.notenoughfakepixel.features.skyblock.chocolate;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.overlays.Timer;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;
import java.util.regex.Pattern;

@RegisterEvents
public final class ChocolateEggTimer extends Timer {
    private static final Identifier ICON = Identifier.of("notenoughfakepixel", "skyblock/egg_hunt.png");
    private static final Pattern EGG_SPAWN = Pattern.compile(
            "HOPPITY'S HUNT\\s+A Chocolate (Breakfast|Lunch|Dinner) Egg has appeared!");
    private long goal;

    public ChocolateEggTimer() {
        super(ICON);
    }

    @Override
    protected void registerEvents() {
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (SkyblockData.isSkyblock() && SkyblockData.season() != SkyblockData.Season.SPRING) goal = 0;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> goal = 0));
    }

    private void onGameMessage(Text message, boolean overlay) {
        if (overlay || !SkyblockData.isSkyblock() || SkyblockData.season() != SkyblockData.Season.SPRING) return;

        var spawn = EGG_SPAWN.matcher(Formatting.strip(message.getString()));
        if (!spawn.find()) return;

        boolean dinner = spawn.group(1).equals("Dinner");
        goal = System.currentTimeMillis() + (dinner ? 500_000 : 350_000);
        if (dinner && ClientConfig.configuration().chocolateFactory.huntDayNotifier) {
            notifyPlayer(MinecraftClient.getInstance(), "New Egg Hunt Day!", Formatting.GREEN, true);
        }
    }

    @Override
    protected boolean shouldShow(Configuration configuration) {
        return configuration.chocolateFactory.chocolateEggTimer && SkyblockData.season() == SkyblockData.Season.SPRING;
    }

    @Override
    protected Position position(Configuration configuration) {
        return configuration.chocolateFactory.eggTimerPos;
    }

    @Override
    protected float scale(Configuration configuration) {
        return configuration.chocolateFactory.eggTimerScale;
    }

    @Override
    protected long goalEpochMillis(long now) {
        return goal;
    }

    @Override
    protected int textColor(long remainingMillis) {
        if (remainingMillis < 0) return 0xFFFFFF55;
        return remainingMillis < 60_000 ? 0xFFFF5555 : 0xFFFFAA00;
    }

    public static void editPosition(Position position, float scale, Consumer<Position> updatePosition) {
        editPosition("Chocolate Egg Timer", ICON, position, scale, 0xFFFFAA00, updatePosition);
    }
}
