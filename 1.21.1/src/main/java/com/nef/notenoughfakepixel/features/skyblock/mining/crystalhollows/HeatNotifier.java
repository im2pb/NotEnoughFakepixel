package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@RegisterEvents
public final class HeatNotifier implements ClientModInitializer {
    private boolean notified;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> notified = false);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || !SkyblockData.isSkyblock()
                || SkyblockData.currentLocation() != Location.CRYSTAL_HOLLOWS) {
            notified = false;
            return;
        }

        var settings = ClientConfig.configuration().mining.crystalHollows;
        boolean hot = settings.crystalHeatNotifier && SkyblockData.heat() >= settings.crystalHeatLevel;
        if (hot && !notified) {
            client.inGameHud.setTitleTicks(2, 70, 2);
            client.inGameHud.setSubtitle(Text.empty());
            client.inGameHud.setTitle(Text.literal("HEAT").formatted(Formatting.RED));
        }
        notified = hot;
    }
}
