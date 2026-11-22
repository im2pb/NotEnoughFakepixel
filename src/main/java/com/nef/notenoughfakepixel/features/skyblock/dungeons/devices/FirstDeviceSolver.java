/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.devices;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketWriteEvent;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonManager;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockButtonStone;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RegisterEvents
public class FirstDeviceSolver {

    private static final EnumSet<EnumFacing> HORIZONTAL_FACINGS = EnumSet.of(
            EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST
    );

    // All solver/world state below is owned by the Minecraft client thread.
    private BlockPos[] positionsToSolve;
    private int[] positionsIndexSolved = new int[]{-1, -1, -1, -1, -1};
    private boolean startMemorising = false;
    private boolean resolving = false;
    private int positionInRound = 0;
    private int round = 1;
    private BlockPos startButtonPos;
    private BlockPos startButtonTarget;
    private EnumFacing deviceFacing;
    private long stateGeneration;

    // Netty only reads the immutable button/sequence snapshot and enqueues accepted clicks.
    private final Object packetGateLock = new Object();
    private volatile PacketGate packetGate = PacketGate.disabled();
    private final ConcurrentLinkedQueue<PacketClick> pendingPacketClicks = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<BlockPos, Long> recentlyHandledClientClicks = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!isFeatureActive(mc)) {
            clearAllState();
            return;
        }

        boolean stateChanged = processPendingPacketClicks();
        stateChanged |= captureNextMemorisedBlock(mc.theWorld);
        if (stateChanged) publishPacketGate(mc.theWorld);
        else refreshPacketGateBlockStates(mc.theWorld);

        long cutoff = System.nanoTime() - 300000000L;
        recentlyHandledClientClicks.entrySet().removeIf(entry -> entry.getValue() < cutoff);
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Config.feature.dungeons.dungeonsFirstDeviceSolver || mc.theWorld == null || mc.thePlayer == null) return;
        if (!resolving || positionsToSolve == null) return;

        int visibleRound = Math.min(round, positionsIndexSolved.length);
        for (int i = 0; i < visibleRound; i++) {
            int solvedIndex = positionsIndexSolved[i];
            if (solvedIndex < 0 || solvedIndex >= positionsToSolve.length) return;

            BlockPos target = positionsToSolve[solvedIndex];
            Color baseColor = ColorUtils.getColor(Config.feature.dungeons.dungeonsAlternativeColor);
            if (i == positionInRound) {
                RenderUtils.highlightBlock(target, ColorUtils.getColor(Config.feature.dungeons.dungeonsCorrectColor), false, true, event.partialTicks);
            } else if (i == positionInRound + 1) {
                RenderUtils.highlightBlock(target, ColorUtils.getColor(Config.feature.dungeons.dungeonsAlternativeColor), false, true, event.partialTicks);
            } else if (i == positionInRound + 2) {
                RenderUtils.highlightBlock(target, new Color(
                        baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 150
                ), false, true, event.partialTicks);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!Config.feature.dungeons.dungeonsFirstDeviceSolver || !DungeonManager.checkEssentialsF7()) return;
        if (mc.theWorld == null || mc.thePlayer == null || mc.thePlayer != event.entityPlayer) return;
        boolean rightClick = event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK;
        boolean leftClick = event.action == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK;
        if (!rightClick && !leftClick) return;

        Block buttonBlock = mc.theWorld.getBlockState(event.pos).getBlock();
        if (!(buttonBlock instanceof BlockButtonStone)) return;

        EnumFacing facing = mc.theWorld.getBlockState(event.pos).getValue(BlockButton.FACING);
        BlockPos target = getBlockUnderButton(event.pos, facing);
        if (target == null) return;

        Block targetBlock = mc.theWorld.getBlockState(target).getBlock();
        if (targetBlock == Blocks.emerald_block && (rightClick || !startMemorising)) {
            // Preserve the legacy behavior: a right-click on the emerald button always
            // restarts memorization. The left-click packet path only starts while idle,
            // matching the old C07 handler's !startMemorising guard.
            startSequence(target, event.pos, facing);
            recentlyHandledClientClicks.put(target, System.nanoTime());
            publishPacketGate(mc.theWorld);
            return;
        }

        // Only right-clicks are advanced directly by PlayerInteractEvent. Left-click
        // sequence packets are validated synchronously by onDiggingPacket instead.
        if (!rightClick) return;

        if (targetBlock == Blocks.obsidian && resolving) {
            if (!isExpectedTarget(target)) {
                event.setCanceled(true);
                return;
            }
            // Invalidate any packet-side reservation for this same interaction before
            // applying the client event, so the click is never counted twice.
            pendingPacketClicks.clear();
            advanceCorrectTarget();
            stateGeneration++;
            recentlyHandledClientClicks.put(target, System.nanoTime());
            publishPacketGate(mc.theWorld);
        }
    }

    /**
     * PacketWriteEvent is raised from Netty's channel thread. This method deliberately does
     * not access Minecraft, World, DungeonManager, block states, entities, or solver fields.
     * It only validates against a client-thread-published snapshot and queues accepted clicks.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDiggingPacket(PacketWriteEvent event) {
        if (!(event.packet instanceof C07PacketPlayerDigging)) return;
        C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.packet;
        if (packet.getStatus() != C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) return;

        BlockPos buttonPos = packet.getPosition();
        synchronized (packetGateLock) {
            PacketGate gate = packetGate;
            if (!gate.enabled) return;

            BlockPos target = gate.buttonTargets.get(buttonPos);
            if (target == null) return;

            // A right-click PlayerInteractEvent may already have advanced the same click.
            Long handledAt = recentlyHandledClientClicks.get(target);
            if (handledAt != null && System.nanoTime() - handledAt < 300000000L) return;

            if (gate.startAllowed && gate.startButtonIsEmerald && target.equals(gate.startTarget)) {
                if (!gate.startReserved) {
                    gate.startReserved = true;
                    pendingPacketClicks.add(new PacketClick(gate.generation, target, true));
                }
                return;
            }

            if (!gate.obsidianTargets.contains(target)) return;
            if (!gate.resolving || gate.reservedClicks >= gate.expectedTargets.length) return;

            BlockPos expected = gate.expectedTargets[gate.reservedClicks];
            if (!target.equals(expected)) {
                event.setCanceled(true);
                return;
            }

            gate.reservedClicks++;
            pendingPacketClicks.add(new PacketClick(gate.generation, target, false));
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // Forge also posts WorldEvent.Unload for the integrated server world.
        // This listener owns client-thread state, so ignore non-client worlds.
        if (event.world == null || !event.world.isRemote) return;
        clearAllState();
    }

    private boolean isFeatureActive(Minecraft mc) {
        return Config.feature.dungeons.dungeonsFirstDeviceSolver
                && mc.theWorld != null
                && mc.thePlayer != null
                && DungeonManager.checkEssentialsF7();
    }

    private void startSequence(BlockPos target, BlockPos buttonPos, EnumFacing facing) {
        pendingPacketClicks.clear();
        positionsToSolve = getSurroundingBlocks(target, facing);
        startButtonPos = buttonPos;
        startButtonTarget = target;
        deviceFacing = facing;
        resetProgress();
        startMemorising = true;
        stateGeneration++;
    }

    private boolean captureNextMemorisedBlock(World world) {
        if (!startMemorising || resolving || positionsToSolve == null || world == null) return false;
        if (round < 1 || round > positionsIndexSolved.length) {
            resetAllProgress();
            return true;
        }

        for (int i = 0; i < positionsToSolve.length; i++) {
            if (positionsIndexSolved[round - 1] != -1) break;
            if (checkIfAdded(positionsIndexSolved, i)) continue;
            if (world.getBlockState(positionsToSolve[i]).getBlock() == Blocks.sea_lantern) {
                positionsIndexSolved[round - 1] = i;
                resolving = true;
                startMemorising = false;
                return true;
            }
        }
        return false;
    }

    private boolean processPendingPacketClicks() {
        PacketClick click;
        boolean consumed = false;
        while ((click = pendingPacketClicks.poll()) != null) {
            consumed = true;
            if (click.generation != stateGeneration) continue;

            if (click.start) {
                if (startButtonTarget != null && click.target.equals(startButtonTarget) && !startMemorising) {
                    startSequence(click.target, startButtonPos, deviceFacing);
                }
                continue;
            }

            // The Netty snapshot already verified that this target was obsidian-backed
            // when the packet was sent. Do not re-read mutable world state here.
            if (resolving && isExpectedTarget(click.target)) advanceCorrectTarget();
        }
        return consumed;
    }

    private boolean isExpectedTarget(BlockPos target) {
        if (!resolving || positionsToSolve == null || positionInRound < 0 || positionInRound >= round) return false;
        if (positionInRound >= positionsIndexSolved.length) return false;
        int solvedIndex = positionsIndexSolved[positionInRound];
        return solvedIndex >= 0 && solvedIndex < positionsToSolve.length && positionsToSolve[solvedIndex].equals(target);
    }

    private void advanceCorrectTarget() {
        positionInRound++;
        if (positionInRound == round) {
            positionInRound = 0;
            round++;
            startMemorising = true;
            resolving = false;
            if (round == 6) resetAllProgress();
        }
    }

    private void publishPacketGate(World world) {
        if (world == null || positionsToSolve == null || startButtonPos == null || startButtonTarget == null) {
            synchronized (packetGateLock) {
                packetGate = PacketGate.disabled();
            }
            return;
        }

        Map<BlockPos, BlockPos> buttonTargets = new HashMap<>();
        buttonTargets.put(startButtonPos, startButtonTarget);
        for (BlockPos target : positionsToSolve) {
            for (EnumFacing facing : HORIZONTAL_FACINGS) {
                BlockPos buttonPos = getButtonPositionForBlock(target, facing);
                if (buttonPos == null) continue;
                if (!(world.getBlockState(buttonPos).getBlock() instanceof BlockButtonStone)) continue;
                if (world.getBlockState(buttonPos).getValue(BlockButton.FACING) == facing) {
                    buttonTargets.put(buttonPos, target);
                }
            }
        }

        Set<BlockPos> obsidianTargets = new HashSet<>();
        for (BlockPos target : positionsToSolve) {
            if (world.getBlockState(target).getBlock() == Blocks.obsidian) obsidianTargets.add(target);
        }
        boolean startButtonIsEmerald = world.getBlockState(startButtonTarget).getBlock() == Blocks.emerald_block;

        BlockPos[] expectedTargets = new BlockPos[0];
        if (resolving && positionsIndexSolved != null && positionInRound >= 0 && positionInRound < round) {
            int count = Math.max(0, Math.min(round - positionInRound, positionsIndexSolved.length - positionInRound));
            expectedTargets = new BlockPos[count];
            for (int i = 0; i < count; i++) {
                int solvedIndex = positionsIndexSolved[positionInRound + i];
                if (solvedIndex >= 0 && solvedIndex < positionsToSolve.length) {
                    expectedTargets[i] = positionsToSolve[solvedIndex];
                }
            }
        }

        synchronized (packetGateLock) {
            packetGate = new PacketGate(
                    true,
                    startButtonTarget,
                    !startMemorising,
                    resolving,
                    buttonTargets,
                    expectedTargets,
                    obsidianTargets,
                    startButtonIsEmerald,
                    stateGeneration
            );
        }
    }


    private void refreshPacketGateBlockStates(World world) {
        if (world == null || positionsToSolve == null || startButtonTarget == null) return;
        Set<BlockPos> obsidianTargets = new HashSet<>();
        for (BlockPos target : positionsToSolve) {
            if (world.getBlockState(target).getBlock() == Blocks.obsidian) obsidianTargets.add(target);
        }
        boolean startButtonIsEmerald = world.getBlockState(startButtonTarget).getBlock() == Blocks.emerald_block;
        synchronized (packetGateLock) {
            if (packetGate.enabled) packetGate.updateBlockStates(obsidianTargets, startButtonIsEmerald);
        }
    }

    private void clearAllState() {
        if (positionsToSolve == null && startButtonPos == null && startButtonTarget == null
                && !startMemorising && !resolving && pendingPacketClicks.isEmpty()) {
            recentlyHandledClientClicks.clear();
            return;
        }
        resetAllProgress();
        recentlyHandledClientClicks.clear();
    }

    private void resetAllProgress() {
        resetProgress();
        positionsToSolve = null;
        startButtonPos = null;
        startButtonTarget = null;
        deviceFacing = null;
        stateGeneration++;
        pendingPacketClicks.clear();
        synchronized (packetGateLock) {
            packetGate = PacketGate.disabled();
        }
    }

    private void resetProgress() {
        positionsIndexSolved = new int[]{-1, -1, -1, -1, -1};
        startMemorising = false;
        resolving = false;
        positionInRound = 0;
        round = 1;
    }

    private static BlockPos getBlockUnderButton(BlockPos pos, EnumFacing facing) {
        switch (facing) {
            case NORTH: return new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
            case EAST: return new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());
            case SOUTH: return new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
            case WEST: return new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
            default: return null;
        }
    }

    private static BlockPos getButtonPositionForBlock(BlockPos target, EnumFacing facing) {
        switch (facing) {
            case NORTH: return new BlockPos(target.getX(), target.getY(), target.getZ() - 1);
            case EAST: return new BlockPos(target.getX() + 1, target.getY(), target.getZ());
            case SOUTH: return new BlockPos(target.getX(), target.getY(), target.getZ() + 1);
            case WEST: return new BlockPos(target.getX() - 1, target.getY(), target.getZ());
            default: return null;
        }
    }

    public static BlockPos[] getSurroundingBlocks(BlockPos startPos, EnumFacing enumFacing) {
        BlockPos[] surroundingBlocks = new BlockPos[16];
        int index = 0;
        for (int yOffset = -1; yOffset <= 2; yOffset++) {
            for (int hOffset = 1; hOffset <= 4; hOffset++) {
                int dx = 0, dz = 0;
                switch (enumFacing) {
                    case NORTH: dx = -hOffset; break;
                    case SOUTH: dx = hOffset; break;
                    case WEST: dz = hOffset; break;
                    case EAST: dz = -hOffset; break;
                    default: break;
                }
                surroundingBlocks[index++] = new BlockPos(startPos.getX() + dx, startPos.getY() + yOffset, startPos.getZ() + dz);
            }
        }
        return surroundingBlocks;
    }

    private boolean checkIfAdded(int[] array, int index) {
        for (int value : array) if (value == index) return true;
        return false;
    }

    private static final class PacketClick {
        private final long generation;
        private final BlockPos target;
        private final boolean start;

        private PacketClick(long generation, BlockPos target, boolean start) {
            this.generation = generation;
            this.target = target;
            this.start = start;
        }
    }

    private static final class PacketGate {
        private final boolean enabled;
        private final BlockPos startTarget;
        private final boolean startAllowed;
        private final boolean resolving;
        private final Map<BlockPos, BlockPos> buttonTargets;
        private final BlockPos[] expectedTargets;
        private volatile Set<BlockPos> obsidianTargets;
        private volatile boolean startButtonIsEmerald;
        private final long generation;
        // Accessed only while holding packetGateLock.
        private int reservedClicks;
        private boolean startReserved;

        private PacketGate(boolean enabled, BlockPos startTarget, boolean startAllowed, boolean resolving,
                           Map<BlockPos, BlockPos> buttonTargets, BlockPos[] expectedTargets,
                           Set<BlockPos> obsidianTargets, boolean startButtonIsEmerald, long generation) {
            this.enabled = enabled;
            this.startTarget = startTarget;
            this.startAllowed = startAllowed;
            this.resolving = resolving;
            this.buttonTargets = Collections.unmodifiableMap(new HashMap<>(buttonTargets));
            this.expectedTargets = expectedTargets.clone();
            this.obsidianTargets = Collections.unmodifiableSet(new HashSet<>(obsidianTargets));
            this.startButtonIsEmerald = startButtonIsEmerald;
            this.generation = generation;
        }

        private void updateBlockStates(Set<BlockPos> obsidianTargets, boolean startButtonIsEmerald) {
            if (!this.obsidianTargets.equals(obsidianTargets)) {
                this.obsidianTargets = Collections.unmodifiableSet(new HashSet<>(obsidianTargets));
            }
            this.startButtonIsEmerald = startButtonIsEmerald;
        }

        private static PacketGate disabled() {
            return new PacketGate(false, null, false, false,
                    Collections.<BlockPos, BlockPos>emptyMap(), new BlockPos[0],
                    Collections.<BlockPos>emptySet(), false, -1L);
        }
    }
}
