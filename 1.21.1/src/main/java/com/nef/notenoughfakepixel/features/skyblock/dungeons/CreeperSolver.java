package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.WorldBoxRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RegisterEvents
public final class CreeperSolver implements ClientModInitializer {
    private static final int[] COLORS = {
            0x8050EF39, 0x80C51111, 0x80132ED1, 0x80117F2D, 0x80ED54BA,
            0x80EF7D0D, 0x80F5F557, 0x80D6E0F0, 0x806B2FBB, 0x8039FEDC
    };
    private static final int MIN_LAMP_Y = 68;
    private static final int X_RANGE = 14;
    private static final int Z_RANGE = 13;
    private static final int Y_RANGE = 8;

    private final WorldBoxRenderer renderer = new WorldBoxRenderer();
    private CreeperEntity creeper;
    private int ticksUntilScan;
    private List<WorldBoxRenderer.Marker> markers = List.of();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> reset());
        WorldRenderEvents.LAST.register(context -> {
            if (enabled()) renderer.draw(context, markers, true, false);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private void update(MinecraftClient client) {
        if (!enabled() || client.world == null || client.player == null) {
            reset();
            return;
        }

        var player = client.player;
        var search = Box.of(player.getPos(), X_RANGE * 2, Y_RANGE * 2, Z_RANGE * 2);
        if (creeper != null && (!visible(creeper) || !search.intersects(creeper.getBoundingBox()))) reset();
        if (ticksUntilScan-- > 0) return;
        ticksUntilScan = 19;
        creeper = client.world.getEntitiesByClass(CreeperEntity.class, search, CreeperSolver::visible).stream()
                .min(Comparator.comparingDouble(player::squaredDistanceTo)).orElse(null);
        if (creeper == null) {
            markers = List.of();
            return;
        }

        List<Pair> pairs = findPairs(client.world, creeper.getPos().add(0, 1, 0));
        var nextMarkers = new ArrayList<WorldBoxRenderer.Marker>(pairs.size() * 2);
        for (int i = 0; i < pairs.size(); i++) {
            var pair = pairs.get(i);
            int color = COLORS[i % COLORS.length];
            nextMarkers.add(new WorldBoxRenderer.Marker(new Box(pair.first()).expand(0.01), color));
            nextMarkers.add(new WorldBoxRenderer.Marker(new Box(pair.second()).expand(0.01), color));
        }
        markers = nextMarkers;
    }

    static List<Pair> findPairs(BlockView world, Vec3d center) {
        BlockPos origin = BlockPos.ofFloored(center);
        int top = origin.getY() + 9;
        if (top <= MIN_LAMP_Y) return List.of();

        var from = new BlockPos(origin.getX() - X_RANGE, Math.max(MIN_LAMP_Y, origin.getY() - 8), origin.getZ() - Z_RANGE);
        var to = new BlockPos(origin.getX() + X_RANGE, top, origin.getZ() + Z_RANGE);
        List<BlockPos> lamps = new ArrayList<>();
        for (BlockPos pos : BlockPos.iterate(from, to)) {
            if (isLamp(world.getBlockState(pos))) lamps.add(pos.toImmutable());
        }
        if (lamps.size() < 2) return List.of();

        Set<BlockPos> lampPositions = new HashSet<>(lamps);
        List<Pair> candidates = new ArrayList<>();
        for (BlockPos source : lamps) {
            Vec3d throughCreeper = center.subtract(Vec3d.ofCenter(source));
            if (throughCreeper.lengthSquared() < 1) continue;

            Vec3d direction = throughCreeper.normalize();
            BlockPos wall = BlockView.raycast(center, center.add(direction.multiply(22)), world,
                    (view, pos) -> view.getBlockState(pos).isAir() ? null : pos.toImmutable(), view -> null);
            if (wall == null) continue;

            BlockPos nearest = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (BlockPos pos : BlockPos.iterate(wall.add(-2, -3, -2), wall.add(2, 3, 2))) {
                if (pos.getY() <= MIN_LAMP_Y || !lampPositions.contains(pos)
                        || Vec3d.ofCenter(pos).subtract(center).dotProduct(direction) < 1) continue;

                double distance = pos.getSquaredDistance(wall);
                if (distance < bestDistance) {
                    nearest = pos.toImmutable();
                    bestDistance = distance;
                }
            }
            if (nearest != null) candidates.add(new Pair(source, nearest, bestDistance));
        }

        candidates.sort(Comparator.comparingDouble(Pair::error).thenComparing(Pair::first).thenComparing(Pair::second));
        Set<BlockPos> used = new HashSet<>();
        List<Pair> pairs = new ArrayList<>();
        for (Pair pair : candidates) {
            if (used.contains(pair.first()) || used.contains(pair.second())) continue;
            used.add(pair.first());
            used.add(pair.second());
            pairs.add(pair);
        }
        return pairs;
    }

    private static boolean isLamp(BlockState state) {
        return state.isOf(Blocks.SEA_LANTERN) || state.isOf(Blocks.PRISMARINE)
                || state.isOf(Blocks.PRISMARINE_BRICKS) || state.isOf(Blocks.DARK_PRISMARINE);
    }

    private static boolean visible(CreeperEntity creeper) {
        return creeper.isAlive() && !creeper.isInvisible();
    }

    private static boolean enabled() {
        return DungeonContext.active() && ClientConfig.configuration().dungeons.puzzles.dungeonsCreeper;
    }

    private void reset() {
        creeper = null;
        ticksUntilScan = 0;
        markers = List.of();
    }

    record Pair(BlockPos first, BlockPos second, double error) {
    }
}
