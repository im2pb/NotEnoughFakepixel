package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.overlays.Timer;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

@RegisterEvents
public final class WormSpawnTimer extends Timer {
    private static final Identifier ICON = Identifier.of("notenoughfakepixel", "crystalhollows/pets_scatha.png");
    private static final String WORM_SPAWN = "You hear the sound of something approaching...";
    private ClientWorld spawnWorld;
    private long goal;

    public WormSpawnTimer() {
        super(ICON);
    }

    @Override
    protected void registerEvents() {
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (spawnWorld != client.world) {
                spawnWorld = null;
                goal = 0;
            }
        });
    }

    private void onGameMessage(Text message, boolean overlay) {
        if (overlay || !SkyblockData.isSkyblock() || !Formatting.strip(message.getString()).contains(WORM_SPAWN)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        spawnWorld = client.world;
        goal = System.currentTimeMillis() + 30_000;
        if (ClientConfig.configuration().mining.crystalHollows.crystalWormNotifier) {
            notifyPlayer(client, "Worm", Formatting.GREEN, true);
        }
    }

    @Override
    protected boolean shouldShow(Configuration configuration) {
        return configuration.mining.crystalHollows.wormTimerCooldown
                && SkyblockData.currentLocation() == Location.CRYSTAL_HOLLOWS
                && spawnWorld == MinecraftClient.getInstance().world;
    }

    @Override
    protected Position position(Configuration configuration) {
        return configuration.mining.wormTimerSettings.wormTimerPos;
    }

    @Override
    protected float scale(Configuration configuration) {
        return configuration.mining.wormTimerSettings.wormTimerScale;
    }

    @Override
    protected long goalEpochMillis(long now) {
        return goal;
    }

    public static void editPosition(Position position, float scale, Consumer<Position> updatePosition) {
        editPosition("Worm Timer", ICON, position, scale, 0xFFFFFFFF, updatePosition);
    }
}
