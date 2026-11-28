package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.IslandWaypoints.Waypoint;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.WaypointRenderer;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.WorldBoxRenderer;
import com.nef.notenoughfakepixel.variables.Slayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VoidgloomBeacons implements ClientModInitializer {
    private static final long TIMEOUT_MILLIS = 20_000;
    private static final double MAX_DISTANCE_SQUARED = 32 * 32;

    @RegisterInstance
    public static final VoidgloomBeacons INSTANCE = new VoidgloomBeacons();

    private final WorldBoxRenderer lines = new WorldBoxRenderer();
    private final WaypointRenderer waypoints = new WaypointRenderer();
    private final ConfigColor color = new ConfigColor();
    private final Map<BlockPos, Long> beacons = new HashMap<>();

    private VoidgloomBeacons() {}

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> beacons.clear());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.LAST.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            lines.close();
            waypoints.close();
        });
    }

    public void observeBlock(MinecraftClient client, BlockPos pos, BlockState state) {
        if (!state.isOf(Blocks.BEACON)) {
            beacons.remove(pos);
            return;
        }
        if (client.player == null || client.world == null || !active()
                || client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > MAX_DISTANCE_SQUARED
                || beacons.containsKey(pos)) return;

        var settings = ClientConfig.configuration().slayer.beaconSettings;
        if (!settings.slayerShowBeaconPath && !settings.showTracerToBeacon && !settings.notifyBeaconInScreen) return;
        beacons.put(pos.toImmutable(), Util.getMeasuringTimeMs());
        if (settings.notifyBeaconInScreen) {
            SlayerAlerts.showTitle(client, "Beacon");
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1f));
        }
    }

    public void observeChunkDelta(MinecraftClient client, ChunkDeltaUpdateS2CPacket packet) {
        if (active()) packet.visitUpdates((pos, state) -> observeBlock(client, pos, state));
    }

    private void tick(MinecraftClient client) {
        if (beacons.isEmpty()) return;
        if (client.player == null || client.world == null || !active()) {
            beacons.clear();
            return;
        }
        long now = Util.getMeasuringTimeMs();
        beacons.entrySet().removeIf(entry -> now - entry.getValue() >= TIMEOUT_MILLIS
                || !client.world.getBlockState(entry.getKey()).isOf(Blocks.BEACON));
    }

    private void render(WorldRenderContext context) {
        if (beacons.isEmpty() || !active() || context.matrixStack() == null) return;
        var config = ClientConfig.configuration();
        var settings = config.slayer.beaconSettings;
        int argb = color.resolve(settings.slayerBeaconColor);

        if (settings.slayerShowBeaconPath && config.waypoints.generalWaypointToggle) {
            var markers = beacons.keySet().stream()
                    .map(pos -> Waypoint.atBlock("BEACON", pos.getX(), pos.getY(), pos.getZ())).toList();
            waypoints.render(context, markers, true, argb, 1.5, true, true);
        }
        if (!settings.showTracerToBeacon) return;

        var player = MinecraftClient.getInstance().player;
        if (player == null) return;
        Vec3d eye = player.getCameraPosVec(context.tickCounter().getTickDelta(false));
        for (BlockPos pos : beacons.keySet()) {
            Vec3d target = Vec3d.ofCenter(pos);
            Vec3d direction = target.subtract(eye);
            if (direction.lengthSquared() > 0.01) {
                Vec3d start = eye.add(direction.normalize().multiply(0.1));
                lines.drawPath(context, List.of(start, target), argb, true);
            }
        }
    }

    private static boolean active() {
        return SkyblockData.isSkyblock() && SkyblockData.currentLocation().isEnd()
                && SkyblockData.currentSlayer() == Slayer.VOIDGLOOM && SkyblockData.bossActive();
    }
}
