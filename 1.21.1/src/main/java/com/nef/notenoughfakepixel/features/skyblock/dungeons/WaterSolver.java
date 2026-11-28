package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.WorldBoxRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@RegisterEvents
public final class WaterSolver implements ClientModInitializer {
    private static final int PISTON_Y = 57;
    private static final int ROOM_RADIUS = 20;
    private static final int ACTIVE_BOX = 0x8000FF00;
    private static final int LATER_BOX = 0x80FFFF00;
    private static final int TRACER = 0xFF00FF00;
    private static final Control[] MATERIALS = {
            Control.QUARTZ, Control.DIAMOND, Control.GOLD, Control.EMERALD, Control.COAL, Control.TERRACOTTA
    };

    private final WorldBoxRenderer renderer = new WorldBoxRenderer();
    private Room room;
    private Hint hint = Hint.EMPTY;
    private int ticksUntilScan;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> reset());
        WorldRenderEvents.LAST.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private void update(MinecraftClient client) {
        if (!enabled() || client.world == null || client.player == null) {
            reset();
            return;
        }

        BlockPos player = client.player.getBlockPos().withY(PISTON_Y);
        if (room != null && (Math.abs(room.anchor().getX() - player.getX()) > ROOM_RADIUS
                || Math.abs(room.anchor().getZ() - player.getZ()) > ROOM_RADIUS)) reset();
        if ((room == null || room.controls().size() < Control.values().length) && ticksUntilScan-- <= 0) {
            ticksUntilScan = 19;
            Room detected = findRoom(client.world, player);
            if (detected != null) room = detected;
        }
        if (room == null) return;

        hint = hint(client.world, room);
        if (hint == null) {
            room = null;
            hint = Hint.EMPTY;
        }
    }

    private void render(WorldRenderContext context) {
        if (hint == Hint.EMPTY || !enabled()) return;

        var camera = context.camera();
        var lineStart = camera.getPos().add(Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).multiply(0.6))
                .add(0, -0.15, 0);
        for (Vec3d target : hint.tracers()) {
            renderer.drawPath(context, List.of(lineStart, target), TRACER, true);
        }
        renderer.draw(context, hint.boxes(), false, true);
    }

    static Room findRoom(BlockView world, BlockPos player) {
        boolean stickyPiston = false;
        BlockPos anchor = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (BlockPos pos : BlockPos.iterate(player.add(-ROOM_RADIUS, 0, -ROOM_RADIUS),
                player.add(ROOM_RADIUS, 0, ROOM_RADIUS))) {
            var state = world.getBlockState(pos);
            if (state.isOf(Blocks.STICKY_PISTON)) stickyPiston = true;
            if (!state.isOf(Blocks.PISTON_HEAD) || gateAt(world, pos, state) == null) continue;

            double distance = pos.getSquaredDistance(player);
            if (distance < nearest) {
                nearest = distance;
                anchor = pos.toImmutable();
            }
        }
        if (!stickyPiston || anchor == null) return null;

        Map<Control, BlockPos> controls = findControls(world, anchor);
        return controls.isEmpty() ? null : new Room(anchor, controls);
    }

    private static Map<Control, BlockPos> findControls(BlockView world, BlockPos anchor) {
        Map<Control, BlockPos> controls = new EnumMap<>(Control.class);
        for (BlockPos pos : BlockPos.iterate(anchor.add(-16, 3, -16), anchor.add(16, 4, 16))) {
            BlockState state = world.getBlockState(pos);
            if (!state.isOf(Blocks.LEVER)) continue;

            BlockPos support = leverSupport(pos, state);
            Control control = Control.forBlock(world.getBlockState(support).getBlock());
            if (control == null) continue;

            BlockPos previous = controls.get(control);
            if (previous == null || pos.getSquaredDistance(anchor) < previous.getSquaredDistance(anchor)) {
                controls.put(control, pos.toImmutable());
            }
        }
        return Map.copyOf(controls);
    }

    static Hint hint(BlockView world, Room room) {
        Map<Control, BlockState> states = new EnumMap<>(Control.class);
        for (var entry : room.controls().entrySet()) {
            BlockState state = world.getBlockState(entry.getValue());
            if (!state.isOf(Blocks.LEVER)) return null;
            states.put(entry.getKey(), state);
        }

        EnumSet<Gate> active = activeGates(world, room.anchor());
        if (active.isEmpty()) return Hint.EMPTY;

        List<WorldBoxRenderer.Marker> boxes = new ArrayList<>();
        List<Vec3d> tracers = new ArrayList<>();
        int row = 0;
        for (Gate gate : active) {
            boolean missingRequiredLever = false;
            for (int i = 0; i < MATERIALS.length; i++) {
                char wanted = gate.settings.charAt(i);
                if (wanted == '-') continue;

                Control control = MATERIALS[i];
                BlockState state = states.get(control);
                if (state == null) {
                    missingRequiredLever = true;
                    continue;
                }
                if (state.get(LeverBlock.POWERED) == (wanted == '1')) continue;

                Box box = leverBounds(world, room.controls().get(control), state);
                boxes.add(new WorldBoxRenderer.Marker(box.offset(0, row, 0).expand(0.02), row == 0 ? ACTIVE_BOX : LATER_BOX));
                if (row == 0) tracers.add(box.getCenter());
            }
            if (row == 0 && tracers.isEmpty() && !missingRequiredLever && states.containsKey(Control.WATER)) {
                tracers.add(leverBounds(world, room.controls().get(Control.WATER), states.get(Control.WATER)).getCenter());
            }
            row++;
        }
        return new Hint(List.copyOf(boxes), List.copyOf(tracers));
    }

    private static EnumSet<Gate> activeGates(BlockView world, BlockPos anchor) {
        EnumSet<Gate> active = EnumSet.noneOf(Gate.class);
        for (BlockPos pos : BlockPos.iterate(anchor.add(-5, 0, -5), anchor.add(5, 0, 5))) {
            var state = world.getBlockState(pos);
            if (!state.isOf(Blocks.PISTON_HEAD)) continue;

            Gate gate = gateAt(world, pos, state);
            if (gate != null) active.add(gate);
        }
        return active;
    }

    private static BlockPos leverSupport(BlockPos pos, BlockState state) {
        return switch (state.get(LeverBlock.FACE)) {
            case FLOOR -> pos.down();
            case CEILING -> pos.up();
            case WALL -> pos.offset(state.get(LeverBlock.FACING).getOpposite());
        };
    }

    private static Box leverBounds(BlockView world, BlockPos pos, BlockState state) {
        var shape = state.getOutlineShape(world, pos);
        return shape.isEmpty() ? new Box(pos) : shape.getBoundingBox().offset(pos);
    }

    private static Gate gateAt(BlockView world, BlockPos pos, BlockState state) {
        var facing = state.get(PistonHeadBlock.FACING);
        if (!facing.getAxis().isHorizontal()) return null;

        Block color = world.getBlockState(pos.offset(facing)).getBlock();
        for (Gate gate : Gate.values()) {
            if (gate.wool == color) return gate;
        }
        return null;
    }

    private static boolean enabled() {
        return DungeonContext.active() && ClientConfig.configuration().dungeons.puzzles.dungeonsWaterSolver;
    }

    private void reset() {
        room = null;
        hint = Hint.EMPTY;
        ticksUntilScan = 0;
    }

    enum Control {
        QUARTZ(Blocks.QUARTZ_BLOCK), DIAMOND(Blocks.DIAMOND_BLOCK), GOLD(Blocks.GOLD_BLOCK),
        EMERALD(Blocks.EMERALD_BLOCK), COAL(Blocks.COAL_BLOCK), TERRACOTTA(Blocks.TERRACOTTA), WATER(Blocks.STONE);

        private final Block support;

        Control(Block support) {
            this.support = support;
        }

        private static Control forBlock(Block block) {
            if (block == Blocks.CHISELED_QUARTZ_BLOCK || block == Blocks.QUARTZ_PILLAR) return QUARTZ;
            for (Control control : values()) {
                if (control.support == block) return control;
            }
            return null;
        }
    }

    enum Gate {
        LIME(Blocks.LIME_WOOL, "000000"),
        BLUE(Blocks.BLUE_WOOL, "0000-1"),
        RED(Blocks.RED_WOOL, "001-1-"),
        PURPLE(Blocks.PURPLE_WOOL, "1110--"),
        ORANGE(Blocks.ORANGE_WOOL, "1101--");

        private final Block wool;
        private final String settings;

        Gate(Block wool, String settings) {
            this.wool = wool;
            this.settings = settings;
        }
    }

    record Room(BlockPos anchor, Map<Control, BlockPos> controls) {
    }

    record Hint(List<WorldBoxRenderer.Marker> boxes, List<Vec3d> tracers) {
        private static final Hint EMPTY = new Hint(List.of(), List.of());
    }
}
