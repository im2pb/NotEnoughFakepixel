package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.List;

@RegisterEvents
public final class TerminalWaypoints implements ClientModInitializer {
    private static final int CYAN = 0x9600BFFF;
    private final DungeonBoxes renderer = new DungeonBoxes();
    private List<DungeonBoxes.Marker> markers = List.of();

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> markers = List.of());
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
        WorldRenderEvents.LAST.register(context -> {
            if (!markers.isEmpty() && enabled()) renderer.draw(context, markers, true, true);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private void update(MinecraftClient client) {
        if (client.world == null || client.player == null || !enabled()) {
            markers = List.of();
            return;
        }
        var positions = new HashSet<BlockPos>();
        for (var entity : client.world.getEntities()) {
            if (!entity.isRemoved() && inactiveLabel(Formatting.strip(entity.getDisplayName().getString()))) {
                positions.add(entity.getBlockPos());
            }
        }
        markers = positions.stream().map(pos -> new DungeonBoxes.Marker(new Box(pos), CYAN)).toList();
    }

    static boolean inactiveLabel(String name) {
        return name.contains("Inactive Device") || name.contains("Not Activated") || name.contains("Inactive Terminal");
    }

    static boolean terminalFloor(DungeonFloor floor) {
        return floor == DungeonFloor.F7 || floor == DungeonFloor.M7;
    }

    private static boolean enabled() {
        if (!DungeonContext.active() || !terminalFloor(SkyblockData.currentFloor())) return false;
        var settings = ClientConfig.configuration();
        return settings.waypoints.generalWaypointToggle && settings.dungeons.terminals.dungeonsTerminalWaypoints;
    }
}
