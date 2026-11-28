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
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.List;

import static com.nef.notenoughfakepixel.features.skyblock.dungeons.BoulderSolver.PushDirection.FORWARD;
import static com.nef.notenoughfakepixel.features.skyblock.dungeons.BoulderSolver.PushDirection.LEFT;
import static com.nef.notenoughfakepixel.features.skyblock.dungeons.BoulderSolver.PushDirection.RIGHT;

@RegisterEvents
public final class BoulderSolver implements ClientModInitializer {
    private static final int DETECTION_RADIUS = 25;
    private static final int MARKER_Y = 68;
    private static final int COLUMNS = 7;
    private static final int ROWS = 6;
    private static final int PUSH_SETTLE_TICKS = 4;
    private static final int HIGHLIGHT_COLOR = 0x99FF0000;

    // Row-major signatures and pushes from the 1.8.9 solver. A short signature checks only its marked rows.
    private static final List<Variant> VARIANTS = List.of(
            new Variant("0100000", List.of(new Push(2, 4, RIGHT), new Push(2, 3, FORWARD),
                    new Push(3, 3, RIGHT), new Push(4, 3, RIGHT), new Push(4, 1, FORWARD), new Push(5, 1, RIGHT))),
            new Variant("1000001", List.of(new Push(3, 4, FORWARD), new Push(2, 4, LEFT),
                    new Push(3, 3, RIGHT), new Push(3, 2, FORWARD), new Push(2, 2, LEFT), new Push(4, 2, RIGHT),
                    new Push(2, 1, FORWARD), new Push(4, 1, FORWARD), new Push(3, 1, RIGHT))),
            new Variant("1001001", List.of(new Push(1, 1, RIGHT))),
            new Variant("1100011", List.of(new Push(4, 3, FORWARD), new Push(3, 3, LEFT),
                    new Push(3, 1, FORWARD), new Push(2, 1, LEFT))),
            new Variant("000000001", List.of(new Push(3, 4, FORWARD), new Push(3, 3, FORWARD),
                    new Push(2, 1, FORWARD), new Push(1, 1, LEFT))),
            new Variant("1001010", List.of(new Push(1, 4, FORWARD), new Push(1, 1, RIGHT))),
            new Variant("000000011111101", List.of(new Push(6, 4, FORWARD), new Push(6, 3, FORWARD),
                    new Push(4, 1, FORWARD), new Push(5, 1, RIGHT))),
            new Variant("00000001", List.of(new Push(0, 1, FORWARD))),
            new Variant("0100101" + "1010000" + "1001010" + "1001111" + "1010101" + "0101010",
                    List.of(new Push(1, 5, RIGHT), new Push(2, 1, RIGHT))));

    private final WorldBoxRenderer renderer = new WorldBoxRenderer();
    private Room room;
    private Variant variant;
    private int nextStep;
    private int emptySourceTicks;
    private int untilRoomScan;
    private List<WorldBoxRenderer.Marker> markers = List.of();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> reset());
        WorldRenderEvents.LAST.register(context -> {
            if (enabled()) renderer.draw(context, markers, true, true);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private void update(MinecraftClient client) {
        if (!enabled() || client.world == null || client.player == null) {
            reset();
            return;
        }

        ClientWorld world = client.world;
        BlockPos player = client.player.getBlockPos();
        if (room == null || !room.center().isWithinDistance(player, DETECTION_RADIUS)) {
            markers = List.of();
            if (untilRoomScan-- > 0) return;
            untilRoomScan = 19;

            var detected = findRoom(world, player);
            if (detected == null) return;
            room = detected;
            variant = null;
            nextStep = 0;
            emptySourceTicks = 0;
        }

        if (variant == null) {
            boolean[] filled = readBoard(world, room);
            if (filled == null) return;
            variant = identifyVariant(filled);
            if (variant == null) return;
        }

        var pushes = variant.pushes();
        markers = List.of();
        if (nextStep == pushes.size()) return;

        Push push = pushes.get(nextStep);
        BlockPos source = room.cell(push.column(), push.row());
        if (!world.isChunkLoaded(source.getX() >> 4, source.getZ() >> 4)) return;

        if (!world.getBlockState(source).isAir()) {
            emptySourceTicks = 0;
            markers = List.of(new WorldBoxRenderer.Marker(new Box(room.button(push)), HIGHLIGHT_COLOR));
        } else if (++emptySourceTicks >= PUSH_SETTLE_TICKS) {
            // A push can briefly leave the next boulder's source empty as its blocks move.
            emptySourceTicks = 0;
            nextStep++;
        }
    }

    private static Room findRoom(ClientWorld world, BlockPos player) {
        BlockPos min = null;
        BlockPos max = null;
        int quartz = 0;
        int barriers = 0;
        var from = new BlockPos(player.getX() - DETECTION_RADIUS, MARKER_Y, player.getZ() - DETECTION_RADIUS);
        var to = new BlockPos(player.getX() + DETECTION_RADIUS, MARKER_Y, player.getZ() + DETECTION_RADIUS);
        for (BlockPos pos : BlockPos.iterate(from, to)) {
            BlockState state = world.getBlockState(pos);
            if (quartzMarker(state)) {
                quartz++;
                if (min == null || (pos.getX() <= min.getX() && pos.getZ() <= min.getZ())) min = pos.toImmutable();
                if (max == null || (pos.getX() >= max.getX() && pos.getZ() >= max.getZ())) max = pos.toImmutable();
            } else if (state.isOf(Blocks.BARRIER)) {
                barriers++;
            }
        }
        if (quartz < 8 || barriers < 10) return null;

        Room best = null;
        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockPos log = switch (facing) {
                case NORTH -> min.add(11, 1, 0);
                case EAST -> max.add(0, 1, -11);
                case SOUTH -> max.add(-11, 1, 0);
                case WEST -> min.add(0, 1, 11);
                default -> throw new IllegalArgumentException("Expected a horizontal direction");
            };
            BlockState state = world.getBlockState(log);
            if (!(state.isIn(BlockTags.LOGS) || state.isOf(Blocks.OAK_LOG) || state.isOf(Blocks.SPRUCE_LOG)
                    || state.isOf(Blocks.BIRCH_LOG) || state.isOf(Blocks.JUNGLE_LOG))) continue;

            var candidate = new Room(log.offset(facing, 2).down(3), facing);
            if (!candidate.center().isWithinDistance(player, DETECTION_RADIUS)) continue;
            var chest = world.getBlockState(candidate.chest());
            if (chest.isOf(Blocks.CHEST) || chest.isOf(Blocks.TRAPPED_CHEST)) return candidate;
            if (best == null) best = candidate;
        }
        return best;
    }

    private static boolean quartzMarker(BlockState state) {
        return state.isOf(Blocks.QUARTZ_BLOCK) || state.isOf(Blocks.QUARTZ_PILLAR) || state.isOf(Blocks.CHISELED_QUARTZ_BLOCK);
    }

    private static boolean[] readBoard(ClientWorld world, Room room) {
        boolean[] filled = new boolean[COLUMNS * ROWS];
        for (int index = 0; index < filled.length; index++) {
            BlockPos cell = room.cell(index % COLUMNS, index / COLUMNS);
            if (!world.isChunkLoaded(cell.getX() >> 4, cell.getZ() >> 4)) return null;
            filled[index] = !world.getBlockState(cell).isAir();
        }
        return filled;
    }

    static Variant identifyVariant(boolean[] filled) {
        for (Variant variant : VARIANTS) {
            String signature = variant.signature();
            if (filled.length < signature.length()) continue;
            int index = 0;
            while (index < signature.length() && filled[index] == (signature.charAt(index) == '1')) index++;
            if (index == signature.length()) return variant;
        }
        return null;
    }

    private boolean enabled() {
        return DungeonContext.active() && ClientConfig.configuration().dungeons.puzzles.dungeonsBoulderSolver;
    }

    private void reset() {
        room = null;
        variant = null;
        nextStep = 0;
        emptySourceTicks = 0;
        untilRoomScan = 0;
        markers = List.of();
    }

    enum PushDirection {
        FORWARD, LEFT, RIGHT
    }

    record Push(int column, int row, PushDirection direction) {
    }

    record Variant(String signature, List<Push> pushes) {
    }

    record Room(BlockPos chest, Direction facing) {
        BlockPos center() {
            return chest.offset(facing.getOpposite(), 12);
        }

        BlockPos cell(int column, int row) {
            return chest.offset(facing.getOpposite(), 5 + 3 * row)
                    .offset(facing.rotateYClockwise(), 3 * column - 9);
        }

        BlockPos button(Push push) {
            Direction direction = switch (push.direction()) {
                case FORWARD -> facing;
                case LEFT -> facing.rotateYCounterclockwise();
                case RIGHT -> facing.rotateYClockwise();
            };
            return cell(push.column(), push.row()).offset(direction.getOpposite(), 2).down();
        }
    }
}
