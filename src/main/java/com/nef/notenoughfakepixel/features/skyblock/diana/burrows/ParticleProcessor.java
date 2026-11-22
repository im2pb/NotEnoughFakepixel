/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.burrows;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.utils.SoundUtils;
import com.nef.notenoughfakepixel.utils.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bounded, time-aware Diana particle classifier. Packet ingress may run on Netty, while
 * classification and waypoint mutation are performed on the client thread.
 */
public class ParticleProcessor {
    private static final String WAYPOINT_SOUND = "random.pop";
    private static final float WAYPOINT_SOUND_VOLUME = 4.0f;
    private static final float TREASURE_SOUND_VOLUME = 3.0f;
    private static final double BURROW_RADIUS_SQUARED = 4.0D;
    private static final int PARTICLE_THRESHOLD = 6;

    /** A short burst window prevents stale particles from unrelated effects joining a burrow. */
    private static final long CLUSTER_WINDOW_NANOS = 750_000_000L;
    private static final long MAX_SAMPLE_AGE_NANOS = 1_500_000_000L;
    private static final int MAX_PENDING_PARTICLES = 512;
    private static final long WAYPOINT_PRUNE_INTERVAL_NANOS = 250_000_000L;
    private static final int WAYPOINT_PRUNE_DISTANCE = 64;

    private final Object particleLock = new Object();
    private final Deque<ParticleSample> pendingParticles = new ArrayDeque<>();
    private long particleVersion;
    private long lastProcessedVersion = -1L;

    private final List<Waypoint> waypoints = new CopyOnWriteArrayList<>();

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    private long lastWaypointPruneNanos;
    private int lastPrunePlayerX = Integer.MIN_VALUE;
    private int lastPrunePlayerY = Integer.MIN_VALUE;
    private int lastPrunePlayerZ = Integer.MIN_VALUE;

    /** Called from packet ingress. Only immutable particle data is retained. */
    public void addParticle(S2APacketParticles packet) {
        if (packet == null || packet.getParticleType() == null) return;
        String type = packet.getParticleType().getParticleName();
        if (!isSupportedType(type)) return;

        ParticleSample sample = new ParticleSample(type,
                packet.getXCoordinate(), packet.getYCoordinate(), packet.getZCoordinate(), System.nanoTime());
        synchronized (particleLock) {
            while (pendingParticles.size() >= MAX_PENDING_PARTICLES) pendingParticles.pollFirst();
            pendingParticles.addLast(sample);
            particleVersion++;
        }
    }

    /** Called on the client tick. Expensive waypoint pruning is rate-limited independently. */
    public void processParticles() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null) return;

        int playerX = floor(mc.thePlayer.posX);
        int playerY = floor(mc.thePlayer.posY);
        int playerZ = floor(mc.thePlayer.posZ);
        long now = System.nanoTime();
        pruneWaypointsIfDue(playerX, playerY, playerZ, now);

        List<ParticleSample> snapshot;
        long processedVersion;
        synchronized (particleLock) {
            if (particleVersion == lastProcessedVersion) return;
            long oldestAllowed = now - MAX_SAMPLE_AGE_NANOS;
            Iterator<ParticleSample> expiry = pendingParticles.iterator();
            while (expiry.hasNext()) {
                if (expiry.next().receivedNanos < oldestAllowed) expiry.remove();
            }
            snapshot = new ArrayList<>(pendingParticles);
            processedVersion = particleVersion;
            lastProcessedVersion = processedVersion;
        }

        // Normally this loop handles one burst. The cap prevents a particle storm from monopolizing a tick.
        int detectionsThisTick = 0;
        while (snapshot.size() >= PARTICLE_THRESHOLD + 1 && detectionsThisTick < 4) {
            DetectionCandidate candidate = findBestCandidate(snapshot, now);
            if (candidate == null) break;
            consumeCluster(candidate);
            detectionsThisTick++;

            int[] coordinates = candidate.coordinates;
            BlockPos block = new BlockPos(coordinates[0], coordinates[1] - 1, coordinates[2]);
            if (!mc.theWorld.isAirBlock(block) && !isDevPetClose(coordinates)) {
                Waypoint result = new Waypoint(candidate.waypointType, coordinates);
                if (!isDuplicateResult(result)) {
                    waypoints.add(result);
                    if (Config.feature.diana.dianaWaypointSounds) {
                        if ("EMPTY".equals(result.getType()) || "MOB".equals(result.getType())) {
                            SoundUtils.playSound(coordinates, WAYPOINT_SOUND, WAYPOINT_SOUND_VOLUME, 2.0f);
                        } else if ("TREASURE".equals(result.getType())) {
                            SoundUtils.playSound(coordinates, WAYPOINT_SOUND, TREASURE_SOUND_VOLUME, 0.5f);
                        }
                    }
                }
            }

            synchronized (particleLock) {
                snapshot = new ArrayList<>(pendingParticles);
                processedVersion = particleVersion;
                lastProcessedVersion = processedVersion;
            }
            now = System.nanoTime();
        }

        if (detectionsThisTick >= 4) {
            synchronized (particleLock) {
                // Continue remaining clusters on the next tick without rescanning unchanged noise forever.
                if (particleVersion == lastProcessedVersion && pendingParticles.size() >= PARTICLE_THRESHOLD + 1) {
                    lastProcessedVersion = particleVersion - 1L;
                }
            }
        }
    }

    /**
     * Uses a coarse spatial hash to avoid comparing every packet with every other packet.
     * Only an enchantment-table anchor and six nearby, same-burst particles can form a result.
     */
    private DetectionCandidate findBestCandidate(List<ParticleSample> samples, long now) {
        Map<Long, List<ParticleSample>> buckets = new HashMap<>();
        for (ParticleSample sample : samples) {
            int cellX = Math.floorDiv(sample.blockX, 4);
            int cellY = Math.floorDiv(sample.blockY, 4);
            int cellZ = Math.floorDiv(sample.blockZ, 4);
            long key = packCell(cellX, cellY, cellZ);
            List<ParticleSample> bucket = buckets.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                buckets.put(key, bucket);
            }
            bucket.add(sample);
        }

        DetectionCandidate best = null;
        for (ParticleSample anchor : samples) {
            if (!"enchantmenttable".equals(anchor.type)) continue;
            if (now - anchor.receivedNanos > MAX_SAMPLE_AGE_NANOS) continue;

            int[] counts = new int[3]; // crit, magicCrit, dripLava
            double[] sumX = new double[3];
            double[] sumY = new double[3];
            double[] sumZ = new double[3];
            double[] sumDistanceSquared = new double[3];

            int centerX = Math.floorDiv(anchor.blockX, 4);
            int centerY = Math.floorDiv(anchor.blockY, 4);
            int centerZ = Math.floorDiv(anchor.blockZ, 4);
            for (int x = centerX - 1; x <= centerX + 1; x++) {
                for (int y = centerY - 1; y <= centerY + 1; y++) {
                    for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                        List<ParticleSample> bucket = buckets.get(packCell(x, y, z));
                        if (bucket == null) continue;
                        for (ParticleSample sample : bucket) {
                            int typeIndex = typeIndex(sample.type);
                            if (typeIndex < 0 || sample == anchor) continue;
                            if (absoluteDifference(sample.receivedNanos, anchor.receivedNanos) > CLUSTER_WINDOW_NANOS) continue;

                            double dx = sample.x - anchor.x;
                            double dy = sample.y - anchor.y;
                            double dz = sample.z - anchor.z;
                            double distanceSquared = dx * dx + dy * dy + dz * dz;
                            if (distanceSquared > BURROW_RADIUS_SQUARED) continue;

                            counts[typeIndex]++;
                            sumX[typeIndex] += sample.x;
                            sumY[typeIndex] += sample.y;
                            sumZ[typeIndex] += sample.z;
                            sumDistanceSquared[typeIndex] += distanceSquared;
                        }
                    }
                }
            }

            int winner = -1;
            int winnerCount = 0;
            int runnerUpCount = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] > winnerCount) {
                    runnerUpCount = winnerCount;
                    winnerCount = counts[i];
                    winner = i;
                } else if (counts[i] > runnerUpCount) {
                    runnerUpCount = counts[i];
                }
            }
            if (winner < 0 || winnerCount < PARTICLE_THRESHOLD || winnerCount == runnerUpCount) continue;

            int[] coordinates = {
                    floor((anchor.x + sumX[winner]) / (winnerCount + 1)),
                    floor((anchor.y + sumY[winner]) / (winnerCount + 1)),
                    floor((anchor.z + sumZ[winner]) / (winnerCount + 1))
            };
            double averageDistance = sumDistanceSquared[winner] / winnerCount;
            DetectionCandidate current = new DetectionCandidate(anchor, coordinates,
                    winner == 0 ? "MOB" : winner == 1 ? "EMPTY" : "TREASURE",
                    winnerCount, averageDistance);
            if (best == null || current.isBetterThan(best)) best = current;
        }
        return best;
    }

    private void consumeCluster(DetectionCandidate candidate) {
        synchronized (particleLock) {
            Iterator<ParticleSample> iterator = pendingParticles.iterator();
            while (iterator.hasNext()) {
                ParticleSample sample = iterator.next();
                if (absoluteDifference(sample.receivedNanos, candidate.anchor.receivedNanos) > CLUSTER_WINDOW_NANOS) continue;
                double dx = sample.x - candidate.anchor.x;
                double dy = sample.y - candidate.anchor.y;
                double dz = sample.z - candidate.anchor.z;
                if (dx * dx + dy * dy + dz * dz <= BURROW_RADIUS_SQUARED) iterator.remove();
            }
        }
    }

    private void pruneWaypointsIfDue(int playerX, int playerY, int playerZ, long now) {
        boolean playerMoved = Math.abs(playerX - lastPrunePlayerX) >= 2
                || Math.abs(playerY - lastPrunePlayerY) >= 2
                || Math.abs(playerZ - lastPrunePlayerZ) >= 2;
        if (!playerMoved && now - lastWaypointPruneNanos < WAYPOINT_PRUNE_INTERVAL_NANOS) return;

        final int px = playerX;
        final int py = playerY;
        final int pz = playerZ;
        final double maxDistanceSquared = WAYPOINT_PRUNE_DISTANCE * (double) WAYPOINT_PRUNE_DISTANCE;
        waypoints.removeIf(waypoint -> {
            if (waypoint == null || "MINOS".equals(waypoint.getType())) return false;
            int[] coords = waypoint.getCoordinates();
            if (coords == null || coords.length < 3) return true;
            double dx = px - coords[0];
            double dy = py - coords[1];
            double dz = pz - coords[2];
            return dx * dx + dy * dy + dz * dz > maxDistanceSquared;
        });

        lastPrunePlayerX = playerX;
        lastPrunePlayerY = playerY;
        lastPrunePlayerZ = playerZ;
        lastWaypointPruneNanos = now;
    }

    private boolean isDuplicateResult(Waypoint result) {
        if (result == null) return false;
        int[] resultCoords = result.getCoordinates();
        for (Waypoint waypoint : waypoints) {
            if (waypoint == null) continue;
            int[] coords = waypoint.getCoordinates();
            if (coords != null && coords.length >= 3 && squaredDistance(resultCoords, coords) < BURROW_RADIUS_SQUARED) {
                return true;
            }
        }
        return false;
    }

    public boolean areCoordinatesClose(int[] coords1, int[] coords2, float threshold) {
        if (!validCoordinates(coords1) || !validCoordinates(coords2)) return false;
        double limit = threshold * (double) threshold;
        return squaredDistance(coords1, coords2) < limit;
    }

    public static float getDistance(int[] coords1, int[] coords2) {
        if (!validCoordinates(coords1) || !validCoordinates(coords2)) return Float.MAX_VALUE;
        return (float) Math.sqrt(squaredDistance(coords1, coords2));
    }

    public void addWaypoint(Waypoint waypoint) {
        if (waypoint != null) waypoints.add(waypoint);
    }

    public void deleteWaypoint(Waypoint waypoint) {
        if (waypoint != null) waypoints.remove(waypoint);
    }

    /** Removes only waypoints of the requested type, leaving other Diana markers untouched. */
    public void removeWaypointsOfType(String type) {
        if (type == null || type.isEmpty()) return;
        waypoints.removeIf(waypoint -> waypoint != null && type.equals(waypoint.getType()));
    }

    public void clearWaypoints() {
        waypoints.clear();
    }

    public void clearParticles() {
        synchronized (particleLock) {
            pendingParticles.clear();
            particleVersion++;
            lastProcessedVersion = particleVersion;
        }
    }

    public Waypoint getClosestWaypoint(int[] coords) {
        if (!validCoordinates(coords)) return null;
        Waypoint closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Waypoint waypoint : waypoints) {
            if (waypoint == null || !validCoordinates(waypoint.getCoordinates())) continue;
            double distance = squaredDistance(coords, waypoint.getCoordinates());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = waypoint;
            }
        }
        return closest;
    }

    /** Finds a waypoint without allowing a burrow-completion sound to consume a Minos marker. */
    public Waypoint getClosestWaypointExcludingType(int[] coords, String excludedType) {
        if (!validCoordinates(coords)) return null;
        Waypoint closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Waypoint waypoint : waypoints) {
            if (waypoint == null || (excludedType != null && excludedType.equals(waypoint.getType()))
                    || !validCoordinates(waypoint.getCoordinates())) continue;
            double distance = squaredDistance(coords, waypoint.getCoordinates());
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = waypoint;
            }
        }
        return closest;
    }

    private boolean isDevPetClose(int[] coords) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return false;
        for (Object entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityArmorStand)) continue;
            EntityArmorStand armorStand = (EntityArmorStand) entity;
            String name = armorStand.getName();
            if (name == null || !name.contains("Developer's Pet")) continue;
            int dx = coords[0] - armorStand.getPosition().getX();
            int dy = coords[1] - armorStand.getPosition().getY();
            int dz = coords[2] - armorStand.getPosition().getZ();
            if (dx * dx + dy * dy + dz * dz <= 25) return true;
        }
        return false;
    }

    private static boolean isSupportedType(String type) {
        return "crit".equals(type) || "magicCrit".equals(type)
                || "dripLava".equals(type) || "enchantmenttable".equals(type);
    }

    private static int typeIndex(String type) {
        if ("crit".equals(type)) return 0;
        if ("magicCrit".equals(type)) return 1;
        if ("dripLava".equals(type)) return 2;
        return -1;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static long absoluteDifference(long first, long second) {
        return first >= second ? first - second : second - first;
    }

    private static boolean validCoordinates(int[] coords) {
        return coords != null && coords.length >= 3;
    }

    private static double squaredDistance(int[] first, int[] second) {
        double dx = first[0] - second[0];
        double dy = first[1] - second[1];
        double dz = first[2] - second[2];
        return dx * dx + dy * dy + dz * dz;
    }

    /** Packs 4-block spatial cells using the same signed coordinate widths as 1.8 BlockPos. */
    private static long packCell(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38
                | ((long) z & 0x3FFFFFFL) << 12
                | ((long) y & 0xFFFL);
    }

    private static final class ParticleSample {
        final String type;
        final double x;
        final double y;
        final double z;
        final int blockX;
        final int blockY;
        final int blockZ;
        final long receivedNanos;

        ParticleSample(String type, double x, double y, double z, long receivedNanos) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockX = floor(x);
            this.blockY = floor(y);
            this.blockZ = floor(z);
            this.receivedNanos = receivedNanos;
        }
    }

    private static final class DetectionCandidate {
        final ParticleSample anchor;
        final int[] coordinates;
        final String waypointType;
        final int count;
        final double averageDistanceSquared;

        DetectionCandidate(ParticleSample anchor, int[] coordinates, String waypointType,
                           int count, double averageDistanceSquared) {
            this.anchor = anchor;
            this.coordinates = coordinates;
            this.waypointType = waypointType;
            this.count = count;
            this.averageDistanceSquared = averageDistanceSquared;
        }

        boolean isBetterThan(DetectionCandidate other) {
            if (count != other.count) return count > other.count;
            return averageDistanceSquared < other.averageDistanceSquared;
        }
    }
}
