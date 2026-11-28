package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.overlays.Timer;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

@RegisterEvents
public final class DarkAhTimer extends Timer {
    private static final Identifier ICON = Identifier.of("notenoughfakepixel", "skyblock/dark_ah.png");
    private static final long HOUR_MILLIS = 3_600_000;
    private static final long AUCTION_MINUTE_MILLIS = 55 * 60_000;
    private static final long REMINDER_WINDOW_MILLIS = 60_000;
    private long remindedGoal;

    public DarkAhTimer() {
        super(ICON);
    }

    @Override
    protected void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null || !SkyblockData.isSkyblock()
                    || !ClientConfig.configuration().qol.darkAuctionSettings.qolDarkAhNotifier) return;

            if (consumeReminder(System.currentTimeMillis())) {
                notifyPlayer(client, "Dark Auction is about to start!", Formatting.GOLD, false);
            }
        });
    }

    private boolean consumeReminder(long now) {
        long goal = goalEpochMillis(now);
        if (goal <= remindedGoal || goal - now > REMINDER_WINDOW_MILLIS) return false;
        remindedGoal = goal;
        return true;
    }

    @Override
    protected boolean shouldShow(Configuration configuration) {
        return configuration.qol.darkAuctionSettings.qolDAtimer;
    }

    @Override
    protected Position position(Configuration configuration) {
        return configuration.qol.darkAuctionTimerSettings.darkAhTimerPos;
    }

    @Override
    protected float scale(Configuration configuration) {
        return configuration.qol.darkAuctionTimerSettings.darkAHTimerScale;
    }

    @Override
    protected long goalEpochMillis(long now) {
        long goal = Math.floorDiv(now, HOUR_MILLIS) * HOUR_MILLIS + AUCTION_MINUTE_MILLIS;
        return now < goal ? goal : goal + HOUR_MILLIS;
    }

    @Override
    protected int textColor(long remainingMillis) {
        if (remainingMillis <= 0) return 0xFF55FF55;
        return remainingMillis / 1_000 <= 180 ? 0xFFFF5555 : 0xFFFFAA00;
    }

    public static void editPosition(Position position, float scale, Consumer<Position> updatePosition) {
        editPosition("Dark Auction Timer", ICON, position, scale, 0xFFFFAA00, updatePosition);
    }
}
