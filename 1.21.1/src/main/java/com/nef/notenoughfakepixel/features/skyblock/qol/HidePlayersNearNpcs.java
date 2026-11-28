package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class HidePlayersNearNpcs implements ClientModInitializer {
    private static final double HIDE_DISTANCE_SQUARED = 3 * 3;
    private static final double SHOW_DISTANCE_SQUARED = 4 * 4;
    private static final int STABILITY_TICKS = 5;

    @RegisterInstance
    public static final HidePlayersNearNpcs INSTANCE = new HidePlayersNearNpcs();

    private final List<OtherClientPlayerEntity> npcs = new ArrayList<>();
    private final Map<OtherClientPlayerEntity, Visibility> visibility = new IdentityHashMap<>();
    private ClientWorld world;
    private long ticks;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public boolean hidden(Entity entity) {
        if (!(entity instanceof OtherClientPlayerEntity player) || player.getWorld() != world) return false;
        Visibility state = visibility.get(player);
        return state != null && state.hidden() && enabled();
    }

    private void tick(MinecraftClient client) {
        ClientWorld activeWorld = client.player != null && enabled() ? client.world : null;
        if (world != activeWorld) {
            world = activeWorld;
            visibility.clear();
            npcs.clear();
        }
        if (world == null) return;

        ticks++;
        var players = world.getPlayers();
        npcs.clear();
        for (var player : players) {
            if (player instanceof OtherClientPlayerEntity other && isNpc(other)) npcs.add(other);
        }
        if (npcs.isEmpty()) {
            visibility.clear();
            return;
        }

        visibility.keySet().removeIf(Entity::isRemoved);
        for (var player : players) {
            if (player instanceof OtherClientPlayerEntity other && !isNpc(other)) updateVisibility(other);
        }
    }

    private void updateVisibility(OtherClientPlayerEntity player) {
        Visibility state = visibility.get(player);
        if (state != null && state.hidden()) {
            if (ticks - state.since() >= STABILITY_TICKS && !nearNpc(player, SHOW_DISTANCE_SQUARED)) {
                visibility.remove(player);
            }
        } else if (!nearNpc(player, HIDE_DISTANCE_SQUARED)) {
            if (state != null) visibility.remove(player);
        } else if (state == null) {
            visibility.put(player, new Visibility(false, ticks));
        } else if (ticks - state.since() >= STABILITY_TICKS) {
            visibility.put(player, new Visibility(true, ticks));
        }
    }

    private boolean nearNpc(OtherClientPlayerEntity player, double distanceSquared) {
        for (var npc : npcs) {
            if (player.squaredDistanceTo(npc) <= distanceSquared) return true;
        }
        return false;
    }

    private static boolean isNpc(OtherClientPlayerEntity player) {
        // Fakepixel spawns NPCs with random UUIDs and a yellow, bold GameProfile name.
        return player.getUuid().version() == 4 && player.getGameProfile().getName().startsWith("§e§l");
    }

    private static boolean enabled() {
        Location location = SkyblockData.currentLocation();
        return SkyblockData.isSkyblock() && location != Location.PRIVATE_ISLAND && !location.isDungeon()
                && ClientConfig.configuration().qol.visualTweaks.qolHidePlayerNearNpcs;
    }

    private record Visibility(boolean hidden, long since) {}
}
