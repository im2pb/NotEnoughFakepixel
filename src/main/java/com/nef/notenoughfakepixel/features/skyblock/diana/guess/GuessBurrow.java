/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.guess;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.features.skyblock.diana.core.DianaRuntimeContext;
import com.nef.notenoughfakepixel.features.skyblock.diana.guess.ingress.GuessPacketIngress;
import com.nef.notenoughfakepixel.features.skyblock.diana.guess.ingress.GuessPacketCorrelator;
import com.nef.notenoughfakepixel.features.skyblock.diana.warp.WarpHelperController;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Area;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;

public class GuessBurrow implements GatedListener {
    private final GuessPacketIngress packetIngress;
    private final WarpHelperController warpHelper = new WarpHelperController();

    public GuessBurrow(DianaRuntimeContext runtimeContext) {
        if (runtimeContext == null) throw new IllegalArgumentException("Diana runtime context cannot be null");
        this.packetIngress = new GuessPacketIngress(runtimeContext.getScheduler(), this::processPacket);
    }

    @Override
    public boolean isListenerActive() {
        boolean hub = SkyblockData.getCurrentLocation() != null
                && SkyblockData.getCurrentLocation().isHub();
        boolean guessEnabled = Config.feature.diana.dianaBurrowGuess;
        boolean warpEnabled = Config.feature.diana.dianaWarpHelper;

        // Keep this listener registered while a confirmed Guess marker is on screen.
        // SkyblockData can briefly report no Hub location during sub-area transitions;
        // deactivating here used to call resetForLifecycle() and erase the marker.
        boolean markerNeedsRendering = guessPoint != null && guessEnabled;
        return (hub && (guessEnabled || warpEnabled)) || markerNeedsRendering;
    }

    @Override
    public void onListenerDeactivated() {
        // Listener gating is not a world change. Stop the in-progress calculation but
        // retain the last confirmed Guess so it can be rendered again when the feature
        // becomes active. The marker is cleared only by a world unload or replaced by
        // a newly resolved Guess.
        resetTrackingState();
    }

    int dingIndex = 0;
    boolean hasDinged = false;
    Float lastDingPitch = 0f;
    Vec3 lastSoundPoint = null;
    Vec3 guessPoint = null;
    ArrayList<Float> dingSlope = new ArrayList<>();
    Float distance = null;
    /** A particle is kept with the exact Harp source and distance that matched it. */
    private static final class MatchedDripParticle {
        final S2APacketParticles particle;
        final Vec3 soundPoint;
        final float targetDistance;
        MatchedDripParticle(S2APacketParticles particle, Vec3 soundPoint, float targetDistance) {
            this.particle = particle;
            this.soundPoint = new Vec3(soundPoint.xCoord, soundPoint.yCoord, soundPoint.zCoord);
            this.targetDistance = targetDistance;
        }
    }

    private final GuessPacketCorrelator packetCorrelator = new GuessPacketCorrelator();
    private final Queue<MatchedDripParticle> particleDripLavaQueue = new LinkedList<>();
    // The legacy Guesser held baseline particles until the next valid Harp distance existed.
    private final Queue<S2APacketParticles> baselineDripParticles = new LinkedList<>();
    private final ArrayList<Vec3> particleLocations = new ArrayList<>();
    private Area lastObservedArea;
    private boolean wasHubActive;
    /** Identity of the world that owns the persistent Guess coordinate. */
    private World observedWorld;

    @SubscribeEvent
    public void onPacketRead(PacketReadEvent event) {
        if (event == null || event.packet == null) return;
        // PacketReadEvent may run on Netty. Preserve the actual packet receive time before
        // handing immutable packet data to the client thread.
        packetIngress.accept(event.packet, System.nanoTime());
    }

    /** Runs on the client thread; packets are correlated before touching the legacy Guess state. */
    private void processPacket(Packet packet, Long receivedNanos) {
        if (SkyblockData.getCurrentLocation() == null || !SkyblockData.getCurrentLocation().isHub()) return;
        if (packet == null) return;
        long packetTime = receivedNanos == null ? System.nanoTime() : receivedNanos;
        packetCorrelator.accept(packet, packetTime);
    }

    /**
     * Only Harp/Drip pairs from the same place and close in packet time enter the legacy
     * single-sequence Guesser. The pitch/distance calculation below remains the old algorithm.
     */
    private void processMatchedPair(GuessPacketCorrelator.Match match) {
        if (match == null || match.sound == null || match.particle == null) return;
        processHarpPacket(match.sound);

        // Preserve the legacy baseline behavior: its particle waits until the next valid Harp
        // distance is available, just as the old particle queue waited for lastSoundPoint.
        if (lastSoundPoint == null || distance == null) {
            baselineDripParticles.add(match.particle);
            return;
        }

        while (!baselineDripParticles.isEmpty()) {
            S2APacketParticles baseline = baselineDripParticles.poll();
            particleDripLavaQueue.add(new MatchedDripParticle(baseline, lastSoundPoint, distance));
        }
        MatchedDripParticle sample = new MatchedDripParticle(match.particle, lastSoundPoint, distance);
        particleDripLavaQueue.add(sample);
    }

    /** Original single-sequence Harp slope/distance algorithm, unchanged from the legacy implementation. */
    private void processHarpPacket(S29PacketSoundEffect soundPacket) {
        if (soundPacket == null || !"note.harp".equals(soundPacket.getSoundName())) return;
        float pitch = soundPacket.getPitch();
        Vec3 pos = new Vec3(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ());

        hasDinged = true;

        if (pitch < lastDingPitch) {
            resetGuessSession();
            hasDinged = true;
            lastDingPitch = pitch;
        }

        if (lastDingPitch == 0f) {
            lastDingPitch = pitch;
            distance = null;
            lastSoundPoint = null;
            particleLocations.clear();

            return;
        }

        dingIndex++;
        float pitchDelta = pitch - lastDingPitch;
        if (dingIndex > 1) dingSlope.add(pitchDelta);
        if (dingSlope.size() > 20) dingSlope.remove(0);

        Float slope = dingSlope.isEmpty() ? 0f
                : dingSlope.stream().reduce(0f, Float::sum) / dingSlope.size();

        lastSoundPoint = pos;
        lastDingPitch = pitch;
        float rawDistance = slope == 0f ? 0f : (float) (Math.E / slope / 2.8);
        distance = rawDistance;
        float pitchMultiplier = 1f;
        if (pitch > 0) {
            pitchMultiplier = Math.max(0.1f, 2.0f - pitch);
            distance *= pitchMultiplier;
        }

        if (distance < 0) {
            // An invalid intermediate Harp sample must not erase the last confirmed marker.
            // A later valid solver result can replace it; only a world/lifecycle reset clears it.
            distance = 0f;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.START) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        // Keep the last marker across Diana sub-area/Hub metadata transitions, but never
        // carry a world-space coordinate into a different Minecraft world.
        if (mc.theWorld == null) {
            if (observedWorld != null) resetForLifecycle();
            return;
        }
        if (observedWorld != null && observedWorld != mc.theWorld) resetForLifecycle();
        observedWorld = mc.theWorld;

        boolean hub = SkyblockData.getCurrentLocation() != null
                && SkyblockData.getCurrentLocation().isHub();

        if (!hub) {
            // Do not let packets from a non-Hub area leak into the next Diana session.
            // This is only a tracking reset: keep the last confirmed marker visible.
            if (wasHubActive) {
                packetIngress.clear();
                packetCorrelator.clear();
                resetGuessSession();
                warpHelper.clearPrediction();
                lastObservedArea = null;
                wasHubActive = false;
            }
            return;
        }
        wasHubActive = true;

        // Sub-areas share the same Hub server location, so listener activation alone cannot
        // detect transitions such as Village <-> Wilderness. Reset uniformly for every Area.
        resetWhenAreaChanges();

        try {
            packetIngress.drainPendingOnClientThread();

            List<GuessPacketCorrelator.Match> readyPairs = packetIngress.getPendingPacketCount() == 0
                    ? packetCorrelator.drainReady(System.nanoTime()) : java.util.Collections.emptyList();

            for (GuessPacketCorrelator.Match match : readyPairs) processMatchedPair(match);

            if (mc.theWorld == null || mc.thePlayer == null) return;
            while (!particleDripLavaQueue.isEmpty()) {
                MatchedDripParticle sample = particleDripLavaQueue.poll();
                if (sample == null || sample.particle == null || sample.soundPoint == null) continue;
                S2APacketParticles particle = sample.particle;
                Vec3 sampleSoundPoint = sample.soundPoint;
                float sampleDistance = sample.targetDistance;
                Vec3 currLoc = new Vec3(particle.getXCoordinate(), particle.getYCoordinate(), particle.getZCoordinate());

                double deltaX = currLoc.xCoord - sampleSoundPoint.xCoord;
                double deltaY = currLoc.yCoord - sampleSoundPoint.yCoord;
                double deltaZ = currLoc.zCoord - sampleSoundPoint.zCoord;
                boolean matchesSoundWindow = Math.abs(deltaX) < 2 && Math.abs(deltaY) < 0.5 && Math.abs(deltaZ) < 2;

                if (matchesSoundWindow) {
                    if (particleLocations.size() >= 100) continue;
                    if (!particleLocations.isEmpty()
                            && distance(particleLocations.get(particleLocations.size() - 1), currLoc) == 0.0) {

                        continue;
                    }

                    double distMultiplier = 1.0;
                    double predictedDist = 0.0;
                    double actualDist = 0.0;
                    if (particleLocations.size() > 2) {
                        predictedDist = 0.06507 * particleLocations.size() + 0.259;
                        Vec3 lastPos = particleLocations.get(particleLocations.size() - 1);
                        actualDist = distance(currLoc, lastPos);
                        distMultiplier = actualDist / predictedDist;
                    }
                    particleLocations.add(currLoc);

                    if (particleLocations.size() > 5) {

                        ArrayList<Double> slopeThing = new ArrayList<>();
                        for (int i = 0; i < particleLocations.size() - 1; i++) {
                            Vec3 a = particleLocations.get(i);
                            Vec3 b = particleLocations.get(i + 1);
                            slopeThing.add(Math.atan2(b.zCoord - a.zCoord, b.xCoord - a.xCoord));
                        }

                        if (slopeThing.size() < 5) return;

                        Vec3 equationX = new Vec3(slopeThing.size() - 5, slopeThing.size() - 3, slopeThing.size() - 1);
                        Vec3 equationY = new Vec3(
                                slopeThing.get(slopeThing.size() - 5),
                                slopeThing.get(slopeThing.size() - 3),
                                slopeThing.get(slopeThing.size() - 1));

                        Vec3 abc;
                        try {
                            abc = solveEquationThing(equationX, equationY);
                        } catch (ArithmeticException ex) {
                            return;
                        }
                        if (abc == null || !Double.isFinite(abc.xCoord) || !Double.isFinite(abc.yCoord) || !Double.isFinite(abc.zCoord)) return;
                        double a = abc.xCoord;
                        double b = abc.yCoord;
                        double c = abc.zCoord;

                        Vec3 lastParticle = particleLocations.get(particleLocations.size() - 1);
                        Vec3 secondLastParticle = particleLocations.get(particleLocations.size() - 2);
                        double dx = lastParticle.xCoord - secondLastParticle.xCoord;
                        double dz = lastParticle.zCoord - secondLastParticle.zCoord;
                        double magnitude = Math.hypot(dx, dz);

                        if (magnitude < 1.0E-6) return;
                        dx /= magnitude;
                        dz /= magnitude;

                        double[] lastPos = {lastParticle.xCoord, lastParticle.yCoord, lastParticle.zCoord};
                        double distCovered = 0.0;
                        int i = slopeThing.size() - 1;
                        while (distCovered < sampleDistance && i < 10000) {
                            double distStep = distMultiplier * (0.06507 * i + 0.259);
                            lastPos[0] += dx * distStep;
                            lastPos[1] = sampleSoundPoint.yCoord;
                            lastPos[2] += dz * distStep;
                            distCovered = Math.hypot(lastPos[0] - sampleSoundPoint.xCoord, lastPos[2] - sampleSoundPoint.zCoord);
                            i++;
                        }
                        Vec3 predictedPoint = new Vec3(lastPos[0], lastPos[1], lastPos[2]);

                        Vec3 resolvedGuess = findNearestDirtWithAirAbove(mc, predictedPoint);
                        // Keep the previous confirmed marker while this Echo sequence is being
                        // solved. Replace it only when a new world location was actually resolved.
                        if (resolvedGuess != null) {
                            guessPoint = new Vec3(resolvedGuess.xCoord, resolvedGuess.yCoord, resolvedGuess.zCoord);
                        }

                    } 
                }
            }
        } finally {
            // Always refresh/clear Warp Helper, including early exits from particle solver.

            warpHelper.update(mc, guessPoint);

        }
    }

    private void resetWhenAreaChanges() {
        Area currentArea = SkyblockData.getCurrentArea();
        if (lastObservedArea == null) {
            lastObservedArea = currentArea;
            return;
        }
        if (currentArea != lastObservedArea) {
            // Pending pairs from the previous sub-area must not enter the new area.
            packetIngress.clear();
            packetCorrelator.clear();
            // A new Echo/map scan starts fresh solver tracking, but the previous confirmed
            // location remains the visible Guess until another valid location replaces it.
            resetGuessSession();

            lastObservedArea = currentArea;
        }
    }

    /** Resets only in-progress Echo tracking; the last confirmed marker/warp target remain visible. */
    private void resetGuessSession() {
        // A pitch decrease starts a fresh calculation, not a new map. Keep the last confirmed
        // marker until a valid new result replaces it. Area transitions do not erase it.
        dingIndex = 0;
        hasDinged = false;
        lastDingPitch = 0f;
        dingSlope.clear();
        distance = null;
        lastSoundPoint = null;
        particleDripLavaQueue.clear();
        baselineDripParticles.clear();
        particleLocations.clear();
    }

    private Vec3 solveEquationThing(Vec3 x, Vec3 y) {
        double denominatorA = x.yCoord * y.xCoord - x.yCoord * y.zCoord +
                x.xCoord * y.zCoord - y.xCoord * x.zCoord +
                y.yCoord * x.zCoord - y.yCoord * x.xCoord;

        if (Math.abs(denominatorA) < 1.0E-9 || Math.abs(x.yCoord - x.xCoord) < 1.0E-9) {

            throw new ArithmeticException("Degenerate burrow guess equation");
        }

        double a = (-y.xCoord * x.yCoord * x.xCoord - y.yCoord * x.yCoord * x.zCoord +
                y.yCoord * x.yCoord * x.xCoord + x.yCoord * x.zCoord * y.zCoord +
                x.xCoord * x.zCoord * y.xCoord - x.xCoord * x.zCoord * y.zCoord) /
                denominatorA;
        double b = (y.xCoord - y.yCoord) * (x.xCoord + a) * (x.yCoord + a) / (x.yCoord - x.xCoord);
        if (Math.abs(x.xCoord + a) < 1.0E-9 || !Double.isFinite(b)) {
            throw new ArithmeticException("Degenerate burrow guess solution");
        }
        double c = y.xCoord - b / (x.xCoord + a);

        return new Vec3(a, b, c);
    }

    private Vec3 findNearestDirtWithAirAbove(Minecraft mc, Vec3 point) {
        int x = (int) Math.floor(point.xCoord);
        int z = (int) Math.floor(point.zCoord);
        int yStart = Math.max(1, Math.min(255, (int) Math.floor(point.yCoord)));

        for (int y = yStart; y > 0; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            Block blockAbove = mc.theWorld.getBlockState(pos.up()).getBlock();
            if ((block == Blocks.dirt || block == Blocks.grass) && blockAbove == Blocks.air) {
                return new Vec3(x + 0.5, y, z + 0.5);
            }
        }

        for (int y = yStart + 1; y < 255; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            Block blockAbove = mc.theWorld.getBlockState(pos.up()).getBlock();
            if ((block == Blocks.dirt || block == Blocks.grass) && blockAbove == Blocks.air) {
                return new Vec3(x + 0.5, y, z + 0.5);
            }
        }

        double playerY = mc.thePlayer.posY;
        int yFallback = Math.max(1, Math.min(255, (int) Math.floor(playerY)));
        BlockPos pos = new BlockPos(x, yFallback, z);
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        Block blockAbove = mc.theWorld.getBlockState(pos.up()).getBlock();
        return new Vec3(x + 0.5, yFallback, z + 0.5);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        // Capture the prediction once so the marker and tracer use the same snapshot.
        Vec3 guess = guessPoint;
        if (guess != null && Config.feature.diana.dianaBurrowGuess) {
            BlockPos loc = new BlockPos(guess.xCoord, guess.yCoord + 1, guess.zCoord);
            AxisAlignedBB aabb = new AxisAlignedBB(
                    loc.getX(), loc.getY(), loc.getZ(),
                    loc.getX() + 1, loc.getY() + 1, loc.getZ() + 1
            );
            RenderUtils.renderWaypointText("Burrow Guess", loc, event.partialTicks);
            RenderUtils.drawOutlinedBoundingBox(aabb, Color.GREEN, 2.0f, event.partialTicks);
        }
        if (guess != null && Config.feature.diana.dianaTracerBurrowGuess) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.thePlayer != null) {
                Color base = ColorUtils.getColor(Config.feature.diana.dianaGuessBurrowTracerColor);
                Color tracer = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);
                RenderUtils.draw3DLine(
                        new Vec3(guess.xCoord, guess.yCoord, guess.zCoord),
                        mc.thePlayer.getPositionEyes(event.partialTicks),
                        tracer, 4, true, event.partialTicks
                );
            }
        }
    }

    public static double distance(Vec3 d1, Vec3 d2) {
        return d1.distanceTo(d2);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        resetForLifecycle();
    }

    /** Clears transient packet/solver state without deleting the last confirmed Guess. */
    private void resetTrackingState() {
        packetIngress.clear();
        packetCorrelator.clear();
        warpHelper.clearPrediction();

        dingIndex = 0;
        hasDinged = false;
        lastDingPitch = 0f;
        lastSoundPoint = null;
        dingSlope.clear();
        distance = null;
        particleDripLavaQueue.clear();
        baselineDripParticles.clear();
        particleLocations.clear();
        lastObservedArea = null;
        wasHubActive = false;
    }

    /** A true world unload/shutdown invalidates the old world coordinate. */
    public void resetForLifecycle() {
        resetTrackingState();
        warpHelper.resetForLifecycle();
        guessPoint = null;
        observedWorld = null;
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
        warpHelper.renderOverlay(event);
    }

}
