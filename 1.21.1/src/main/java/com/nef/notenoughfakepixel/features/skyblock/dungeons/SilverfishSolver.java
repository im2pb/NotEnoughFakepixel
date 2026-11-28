package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.WorldBoxRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.block.HopperBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RegisterEvents
public final class SilverfishSolver implements ClientModInitializer {
    private static final int RANGE = 27;
    private static final int FLOOR_Y = 66;
    private static final int SIZE = 17;
    private static final Cell GOAL = new Cell(9, 0);
    private static final int PATH_COLOR = 0xFFFF0000;

    private final WorldBoxRenderer renderer = new WorldBoxRenderer();
    private Room room;
    private SilverfishEntity silverfish;
    private Cell lastCell;
    private int untilRoomScan;
    private List<Vec3d> path = List.of();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> reset());
        WorldRenderEvents.LAST.register(context -> {
            if (enabled()) renderer.drawPath(context, path, PATH_COLOR, false);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private void update(MinecraftClient client) {
        if (!enabled() || client.world == null || client.player == null) {
            reset();
            return;
        }

        if (silverfish == null || !silverfish.isAlive() || client.player.squaredDistanceTo(silverfish) > RANGE * RANGE) {
            room = null;
            silverfish = null;
            lastCell = null;
            path = List.of();
            if (untilRoomScan-- > 0) return;
            untilRoomScan = 19;
            if (!detect(client.world, client.player.getBlockPos())) return;
        }

        Cell cell = room.locate(silverfish.getBlockPos());
        if (cell == null) {
            path = List.of();
            lastCell = null;
        } else if (!cell.equals(lastCell)) {
            var walls = readBoard(client.world, room);
            if (walls == null) {
                path = List.of();
                lastCell = null;
                return;
            }
            lastCell = cell;
            path = solve(walls, cell, GOAL).stream().map(point -> Vec3d.ofCenter(room.cell(point))).toList();
        }
    }

    private boolean detect(ClientWorld world, BlockPos player) {
        var area = new Box(player.getX() - RANGE, FLOOR_Y + 1, player.getZ() - RANGE,
                player.getX() + RANGE + 1, FLOOR_Y + 3, player.getZ() + RANGE + 1);
        var fish = world.getEntitiesByClass(SilverfishEntity.class, area, SilverfishEntity::isAlive);
        if (fish.isEmpty()) return false;
        var tnt = world.getEntitiesByClass(ItemEntity.class, area, item -> item.getStack().isOf(Items.TNT));
        var candidates = fish.stream().filter(entity -> tnt.stream().anyMatch(item ->
                Math.abs(item.getX() - entity.getX()) < 1 && Math.abs(item.getZ() - entity.getZ()) < 1)).toList();
        if (candidates.isEmpty()) return false;

        var from = new BlockPos(player.getX() - RANGE, FLOOR_Y, player.getZ() - RANGE);
        var to = new BlockPos(player.getX() + RANGE, FLOOR_Y, player.getZ() + RANGE);
        for (BlockPos floor : BlockPos.iterate(from, to)) {
            if (!world.getBlockState(floor).isOf(Blocks.PACKED_ICE)) continue;
            var hopper = world.getBlockState(floor.up(3));
            if (!hopper.isOf(Blocks.HOPPER)) continue;
            Direction facing = hopper.get(HopperBlock.FACING);
            if (!facing.getAxis().isHorizontal()) continue;

            var candidate = new Room(floor.up(), facing);
            for (var entity : candidates) {
                if (candidate.locate(entity.getBlockPos()) == null) continue;
                room = candidate;
                silverfish = entity;
                return true;
            }
        }
        return false;
    }

    private static boolean[][] readBoard(ClientWorld world, Room room) {
        var walls = new boolean[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                BlockPos pos = room.cell(new Cell(column, row));
                if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) return null;
                walls[row][column] = !world.getBlockState(pos).isAir();
            }
        }
        return walls;
    }

    static List<Cell> solve(boolean[][] walls, Cell start, Cell goal) {
        if (!open(walls, start.column(), start.row()) || !open(walls, goal.column(), goal.row())
                || start.equals(goal)) return List.of();

        var pending = new ArrayDeque<Cell>();
        var previous = new Cell[walls.length][walls[0].length];
        pending.add(start);
        previous[start.row()][start.column()] = start;

        while (!pending.isEmpty()) {
            Cell cell = pending.removeFirst();
            for (Direction direction : Direction.Type.HORIZONTAL) {
                int dx = direction.getOffsetX();
                int dy = direction.getOffsetZ();
                int column = cell.column();
                int row = cell.row();
                while (open(walls, column + dx, row + dy)) {
                    column += dx;
                    row += dy;
                }
                if (previous[row][column] != null) continue;

                var next = new Cell(column, row);
                previous[row][column] = cell;
                if (next.equals(goal)) {
                    List<Cell> result = new ArrayList<>();
                    for (Cell step = next; !step.equals(start); step = previous[step.row()][step.column()]) {
                        result.add(step);
                    }
                    result.add(start);
                    Collections.reverse(result);
                    return result;
                }
                pending.addLast(next);
            }
        }
        return List.of();
    }

    private static boolean open(boolean[][] walls, int column, int row) {
        return row >= 0 && row < walls.length && column >= 0 && column < walls[row].length && !walls[row][column];
    }

    private static boolean enabled() {
        return DungeonContext.active() && ClientConfig.configuration().dungeons.puzzles.dungeonsSilverfishSolver;
    }

    private void reset() {
        room = null;
        silverfish = null;
        lastCell = null;
        untilRoomScan = 0;
        path = List.of();
    }

    record Cell(int column, int row) {
    }

    record Room(BlockPos chest, Direction facing) {
        private BlockPos origin() {
            return chest.offset(facing.getOpposite(), 4).offset(facing.rotateYCounterclockwise(), 8);
        }

        BlockPos cell(Cell cell) {
            return origin().offset(facing.rotateYClockwise(), cell.column()).offset(facing.getOpposite(), cell.row());
        }

        Cell locate(BlockPos pos) {
            BlockPos origin = origin();
            if (Math.abs(pos.getY() - origin.getY()) > 1) return null;

            int x = pos.getX() - origin.getX();
            int z = pos.getZ() - origin.getZ();
            Direction right = facing.rotateYClockwise();
            Direction down = facing.getOpposite();
            int column = x * right.getOffsetX() + z * right.getOffsetZ();
            int row = x * down.getOffsetX() + z * down.getOffsetZ();
            return column >= 0 && column < SIZE && row >= 0 && row < SIZE ? new Cell(column, row) : null;
        }
    }
}
