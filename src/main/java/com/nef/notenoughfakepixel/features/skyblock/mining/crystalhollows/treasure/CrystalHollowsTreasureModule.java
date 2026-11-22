/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows.treasure;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.Logger;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.Area;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Crystal Hollows Metal Detector. Packet callbacks only copy immutable packet data;
 * all Minecraft world/player access is performed on the client thread.
 */
@RegisterEvents
public class CrystalHollowsTreasureModule implements GatedListener {

    private static final double LERP = 0.15;
    private static final int MAX_PENDING_BLOCK_UPDATES = 256;
    private static final int MAX_PACKET_UPDATES_PER_TICK = 256;
    private static final double CONFIRM_RADIUS_SQ = 25.0;
    private static final double CONFIRMED_DISTANCE_TOLERANCE = 3.0;
    private static final double MIN_FALLBACK_CONFIDENCE = 0.02;

    private static final TreasureTriangulator TRI = TreasureTriangulator.getInstance();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final GradientSolver gradientSolver = new GradientSolver();
    private final BlockingQueue<PacketBlockUpdate> pendingBlockUpdates =
            new ArrayBlockingQueue<>(MAX_PENDING_BLOCK_UPDATES);
    private final LinkedHashMap<Long, FloorCacheEntry> floorCache =
            new LinkedHashMap<Long, FloorCacheEntry>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, FloorCacheEntry> eldest) {
                    return size() > 64;
                }
            };

    private BlockPos confirmedChest;
    /** Published to Netty so packet callbacks can filter covered-chest updates without reading the world. */
    private volatile BlockPos confirmedChestForPacket;

    private double lastSeenDistance = Double.NaN;
    private int postResetSkip;
    private boolean wasInDivan;

    private double smoothX0 = Double.NaN;
    private double smoothZ0 = Double.NaN;
    private double smoothX1 = Double.NaN;
    private double smoothZ1 = Double.NaN;
    // Reused render outputs: this code runs once per frame, so avoid temporary arrays.
    private final double[] smoothPointA = new double[2];
    private final double[] smoothPointB = new double[2];

    @Override
    public boolean isListenerActive() {
        return Config.feature != null && Config.feature.mining.crystalMetalDetector;
    }

    @Override
    public void onListenerDeactivated() {
        pendingBlockUpdates.clear();
        wasInDivan = false;
        clearAllState();
    }

    /**
     * PacketReadEvent is dispatched from Netty. Never touch Minecraft world/player state here.
     * Only enqueue chest placements and changes to the currently confirmed chest.
     */
    @SubscribeEvent
    public void onPacketRead(PacketReadEvent event) {
        if (event == null || event.packet == null) return;

        if (event.packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange packet = (S23PacketBlockChange) event.packet;
            if (packet.blockState == null) return;
            BlockPos pos = packet.getBlockPosition();
            boolean isChest = packet.blockState.getBlock() instanceof BlockChest;
            if (isChest || isConfirmedChestPacketPosition(pos)) {
                enqueueBlockUpdate(pos, isChest);
            }
            return;
        }

        if (event.packet instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange packet = (S22PacketMultiBlockChange) event.packet;
            S22PacketMultiBlockChange.BlockUpdateData[] updates = packet.getChangedBlocks();
            if (updates == null) return;
            for (S22PacketMultiBlockChange.BlockUpdateData update : updates) {
                if (update == null || update.getPos() == null || update.getBlockState() == null) continue;
                BlockPos pos = update.getPos();
                boolean isChest = update.getBlockState().getBlock() instanceof BlockChest;
                if (isChest || isConfirmedChestPacketPosition(pos)) {
                    enqueueBlockUpdate(pos, isChest);
                }
            }
        }
    }

    private boolean isConfirmedChestPacketPosition(BlockPos pos) {
        BlockPos confirmed = confirmedChestForPacket;
        return confirmed != null && confirmed.equals(pos);
    }

    private void enqueueBlockUpdate(BlockPos pos, boolean isChest) {
        if (pos == null) return;
        PacketBlockUpdate update = new PacketBlockUpdate(
                new BlockPos(pos.getX(), pos.getY(), pos.getZ()), isChest);
        if (!pendingBlockUpdates.offer(update)) {
            // Keep the newest bounded window instead of allowing packet bursts to grow memory.
            pendingBlockUpdates.poll();
            pendingBlockUpdates.offer(update);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (mc.theWorld == null || mc.thePlayer == null || !isInDivan()) {
            boolean hadPendingUpdates = !pendingBlockUpdates.isEmpty();
            pendingBlockUpdates.clear();
            if (wasInDivan || confirmedChest != null || gradientSolver.getReadingCount() > 0
                    || postResetSkip > 0 || hadPendingUpdates) {
                clearAllState();
            }
            wasInDivan = false;
            return;
        }

        wasInDivan = true;
        processPendingBlockUpdates();
    }

    private boolean isInDivan() {
        return SkyblockData.getCurrentLocation() == Location.CRYSTAL_HOLLOWS
                && SkyblockData.getCurrentArea() == Area.CH_MINES_OF_DIVAN;
    }

    /** Main-thread packet processing: only now is it safe to inspect the loaded world. */
    private void processPendingBlockUpdates() {
        int processed = 0;
        PacketBlockUpdate update;
        while (processed++ < MAX_PACKET_UPDATES_PER_TICK
                && (update = pendingBlockUpdates.poll()) != null) {
            BlockPos pos = update.pos;

            if (!update.isChest) {
                if (confirmedChest != null && confirmedChest.equals(pos)) {
                    Logger.log("§c[MD] Confirmed treasure chest changed — resetting");
                    clearConfirmedChestAndSolver();
                }
                continue;
            }

            if (confirmedChest != null || mc.theWorld == null || mc.thePlayer == null) continue;
            if (!mc.theWorld.isBlockLoaded(pos)) continue;
            if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockChest)) continue;

            double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
            double dy = pos.getY() + 0.5 - mc.thePlayer.posY;
            double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > CONFIRM_RADIUS_SQ) continue;
            double measuredDistance = lastSeenDistance;
            if (!Double.isNaN(measuredDistance)
                    && Math.abs(Math.sqrt(distanceSq) - measuredDistance) > CONFIRMED_DISTANCE_TOLERANCE) continue;

            confirmedChest = pos;
            confirmedChestForPacket = pos;
            resetSolversAndSmoothing();
            Logger.log("§a[MD] Treasure chest confirmed at "
                    + pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
    }

    @SubscribeEvent
    public void onChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
        if (!Config.feature.mining.crystalMetalDetector || !isInDivan() || event == null || event.message == null) return;
        String message = event.message.getFormattedText();

        if (message.contains("with your Metal Detector!")) {
            clearAllState();
            return;
        }

        int marker = message.indexOf("TREASURE:");
        if (marker < 0) return;

        if (postResetSkip > 0) {
            postResetSkip--;
            return;
        }

        int valueStart = marker + "TREASURE:".length();
        int unitIndex = message.indexOf('m', valueStart);
        if (unitIndex < 0) return;
        double distance = parseDistance(message, valueStart, unitIndex);
        if (Double.isNaN(distance)) {
            Logger.log("§c[MD] Could not parse Metal Detector distance");
            return;
        }

        EntityPlayerSP player = mc.thePlayer;
        if (player == null || Double.isNaN(distance) || Double.isInfinite(distance)
                || distance <= 0.0 || distance > 500.0) return;

        if (!Double.isNaN(lastSeenDistance) && Math.abs(distance - lastSeenDistance) < 0.05) return;
        lastSeenDistance = distance;

        if (confirmedChest != null) {
            double dx = confirmedChest.getX() + 0.5 - player.posX;
            double dy = confirmedChest.getY() + 0.5 - player.posY;
            double dz = confirmedChest.getZ() + 0.5 - player.posZ;
            double actualDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (Math.abs(actualDistance - distance) > CONFIRMED_DISTANCE_TOLERANCE) {
                Logger.log("§c[MD] Confirmed chest does not match Metal Detector distance; recalibrating");
                clearConfirmedChestAndSolver();
                lastSeenDistance = distance;
            }
        }

        gradientSolver.addReading(player.posX, player.posY, player.posZ, distance);
        if (gradientSolver.consumeTreasureSwitch()) {
            // Do not let the fallback estimator display a result from the previous treasure.
            TRI.reset();
        }
        TRI.handleData(player.posX, player.posY, player.posZ, distance);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        pendingBlockUpdates.clear();
        wasInDivan = false;
        clearAllState();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (SkyblockData.getCurrentLocation() == Location.CRYSTAL_HOLLOWS) {
            pendingBlockUpdates.clear();
            wasInDivan = false;
            clearAllState();
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent event) {
        if (!Config.feature.mining.crystalMetalDetector || event == null || event.world == null
                || !event.world.isRemote || event.pos == null) return;
        // The reset debounce is only for treasure chests in Mines of Divan.
        // Clicking a chest elsewhere in Crystal Hollows must not discard valid readings later.
        if (!isInDivan()) return;
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                && event.world.isBlockLoaded(event.pos)
                && event.world.getBlockState(event.pos).getBlock() instanceof BlockChest) {
            pendingBlockUpdates.clear();
            clearAllState();
            postResetSkip = 7;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!Config.feature.mining.crystalMetalDetector || !isInDivan()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.theWorld == null) return;

        Color waypointColor = ColorUtils.getColor(Config.feature.mining.crystalDivanWaypointColor);
        int playerY = (int) minecraft.thePlayer.posY;

        if (confirmedChest != null) {
            int beamY = findFloorY(confirmedChest.getX(), confirmedChest.getZ(), confirmedChest.getY());
            RenderUtils.renderBeaconBeam(new BlockPos(confirmedChest.getX(), beamY, confirmedChest.getZ()),
                    waypointColor.getRGB(), 1, event.partialTicks);
            RenderUtils.renderWaypointText("TREASURE!",
                    new BlockPos(confirmedChest.getX(), playerY + 3, confirmedChest.getZ()), event.partialTicks);
            RenderUtils.draw3DLine(minecraft.thePlayer.getPositionEyes(event.partialTicks),
                    new Vec3(confirmedChest.getX() + 0.5, confirmedChest.getY() + 0.5, confirmedChest.getZ() + 0.5),
                    waypointColor, 2, true, event.partialTicks);
            return;
        }

        List<BlockPos> candidates = gradientSolver.getCandidatePositions();
        if (!candidates.isEmpty()) {
            BlockPos first = candidates.get(0);
            if (candidates.size() >= 2) {
                BlockPos second = candidates.get(1);
                if (shouldSwapCandidates(first, second)) {
                    BlockPos swap = first;
                    first = second;
                    second = swap;
                }
                smoothCandidate(0, first.getX() + 0.5, first.getZ() + 0.5, smoothPointA);
                smoothCandidate(1, second.getX() + 0.5, second.getZ() + 0.5, smoothPointB);
                drawCandidate(smoothPointA[0], smoothPointA[1], playerY, "Guess A", waypointColor, event.partialTicks);
                drawCandidate(smoothPointB[0], smoothPointB[1], playerY, "Guess B", waypointColor, event.partialTicks);
            } else {
                smoothCandidate(0, first.getX() + 0.5, first.getZ() + 0.5, smoothPointA);
                smoothX1 = smoothZ1 = Double.NaN;
                drawCandidate(smoothPointA[0], smoothPointA[1], playerY, "Treasure?", waypointColor, event.partialTicks);
            }
            return;
        }

        // Keep the existing grid-based estimator as a fallback while GradientSolver gathers enough geometry.
        GridTrilateration.Int3 guess = TRI.getBestGuess();
        if (guess != null && TRI.getConfidence() >= MIN_FALLBACK_CONFIDENCE) {
            smoothCandidate(0, guess.x + 0.5, guess.z + 0.5, smoothPointA);
            smoothX1 = smoothZ1 = Double.NaN;
            drawCandidate(smoothPointA[0], smoothPointA[1], playerY, "Guess", waypointColor, event.partialTicks);
        } else {
            resetSmoothingIfNeeded();
        }
    }

    private boolean shouldSwapCandidates(BlockPos first, BlockPos second) {
        if (Double.isNaN(smoothX0) || Double.isNaN(smoothX1)) return false;
        double direct = distanceSq(first.getX() + 0.5, first.getZ() + 0.5, smoothX0, smoothZ0)
                + distanceSq(second.getX() + 0.5, second.getZ() + 0.5, smoothX1, smoothZ1);
        double swapped = distanceSq(second.getX() + 0.5, second.getZ() + 0.5, smoothX0, smoothZ0)
                + distanceSq(first.getX() + 0.5, first.getZ() + 0.5, smoothX1, smoothZ1);
        return swapped < direct;
    }

    private static double distanceSq(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return dx * dx + dz * dz;
    }

    private void smoothCandidate(int index, double targetX, double targetZ, double[] output) {
        if (index == 0) {
            if (Double.isNaN(smoothX0)) {
                smoothX0 = targetX;
                smoothZ0 = targetZ;
            } else {
                smoothX0 += (targetX - smoothX0) * LERP;
                smoothZ0 += (targetZ - smoothZ0) * LERP;
            }
            output[0] = smoothX0;
            output[1] = smoothZ0;
            return;
        }
        if (Double.isNaN(smoothX1)) {
            smoothX1 = targetX;
            smoothZ1 = targetZ;
        } else {
            smoothX1 += (targetX - smoothX1) * LERP;
            smoothZ1 += (targetZ - smoothZ1) * LERP;
        }
        output[0] = smoothX1;
        output[1] = smoothZ1;
    }

    /** Parses the distance without regex compilation, substring, or replaceAll allocations. */
    private static double parseDistance(String message, int start, int end) {
        double value = 0.0;
        double decimalScale = 0.1;
        boolean decimalSeen = false;
        boolean digitSeen = false;
        for (int i = start; i < end; i++) {
            char c = message.charAt(i);
            if (c == '\u00A7' && i + 1 < end) {
                i++; // Ignore the following Minecraft formatting character.
                continue;
            }
            if (c >= '0' && c <= '9') {
                digitSeen = true;
                int digit = c - '0';
                if (decimalSeen) {
                    value += digit * decimalScale;
                    decimalScale *= 0.1;
                } else {
                    value = value * 10.0 + digit;
                }
            } else if (c == '.' && !decimalSeen) {
                decimalSeen = true;
            }
        }
        return digitSeen ? value : Double.NaN;
    }

    private void drawCandidate(double x, double z, int playerY, String label, Color color, float partialTicks) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int beamY = findFloorY(blockX, blockZ, playerY);
        RenderUtils.renderBeaconBeam(new BlockPos(blockX, beamY, blockZ), color.getRGB(), 1, partialTicks);
        RenderUtils.renderWaypointText(label, new BlockPos(blockX, playerY + 3, blockZ), partialTicks);
    }

    private int findFloorY(int x, int z, int fromY) {
        if (mc.theWorld == null) return Math.max(1, fromY - 20);
        long now = System.currentTimeMillis();
        long key = (((long) x) << 32) ^ (z & 0xffffffffL);
        FloorCacheEntry cached = floorCache.get(key);
        if (cached != null && now - cached.checkedAtMs < 750L
                && Math.abs(cached.fromY - fromY) <= 3) return cached.floorY;

        int limit = Math.max(1, fromY - 50);
        int result = limit;
        for (int y = fromY; y >= limit; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            try {
                if (!mc.theWorld.isBlockLoaded(pos)) {
                    result = Math.max(1, fromY - 20);
                    break;
                }
                if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.air) {
                    result = y + 1;
                    break;
                }
            } catch (RuntimeException ignored) {
                result = Math.max(1, fromY - 20);
                break;
            }
        }
        floorCache.put(key, new FloorCacheEntry(result, fromY, now));
        return result;
    }

    private void clearConfirmedChestAndSolver() {
        // Drop packet updates captured for the chest that has just been invalidated.
        // Otherwise an older queued chest-placement update can re-confirm stale data.
        pendingBlockUpdates.clear();
        confirmedChest = null;
        confirmedChestForPacket = null;
        lastSeenDistance = Double.NaN;
        resetSolversAndSmoothing();
    }

    private void clearAllState() {
        // A reset invalidates any block updates captured before the reset boundary.
        pendingBlockUpdates.clear();
        confirmedChest = null;
        confirmedChestForPacket = null;
        lastSeenDistance = Double.NaN;
        postResetSkip = 0;
        resetSolversAndSmoothing();
    }

    private void resetSolversAndSmoothing() {
        gradientSolver.reset();
        TRI.reset();
        smoothX0 = smoothZ0 = smoothX1 = smoothZ1 = Double.NaN;
        floorCache.clear();
    }

    private void resetSmoothingIfNeeded() {
        // No candidates are available; avoid carrying stale interpolation into the next search.
        smoothX0 = smoothZ0 = smoothX1 = smoothZ1 = Double.NaN;
    }

    private static final class FloorCacheEntry {
        final int floorY;
        final int fromY;
        final long checkedAtMs;

        FloorCacheEntry(int floorY, int fromY, long checkedAtMs) {
            this.floorY = floorY;
            this.fromY = fromY;
            this.checkedAtMs = checkedAtMs;
        }
    }

    private static final class PacketBlockUpdate {
        final BlockPos pos;
        final boolean isChest;

        PacketBlockUpdate(BlockPos pos, boolean isChest) {
            this.pos = pos;
            this.isChest = isChest;
        }
    }
}
