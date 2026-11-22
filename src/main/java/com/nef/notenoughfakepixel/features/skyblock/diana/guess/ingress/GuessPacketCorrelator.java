/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Correlates the legacy Guesser's Harp and dripLava inputs without creating separate Guess sessions.
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.guess.ingress;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Pairs each Harp sound with the nearby dripLava particle emitted for the same Echo pulse.
 * The spatial thresholds intentionally match the legacy Guesser's existing acceptance rule.
 * This class only filters packet input; it does not calculate or alter a Guess.
 * All methods are called on the Minecraft client thread.
 */
public final class GuessPacketCorrelator {
    /** Maximum receive-time separation allowed when associating the two packet types. */
    public static final long MATCH_WINDOW_NANOS = 200_000_000L;
    /**
     * Small ordering guard after the Harp timestamp. The old implementation waited the full
     * 200 ms matching window even after both packets had already been paired, delaying every guess.
     */
    private static final long PAIR_ORDER_GRACE_NANOS = 25_000_000L;
    private static final int MAX_PENDING_PER_TYPE = 64;
    private static final int MAX_READY_PAIRS = 128;

    public static final class Match {
        public final S29PacketSoundEffect sound;
        public final S2APacketParticles particle;
        public final long soundReceivedNanos;
        public final long particleReceivedNanos;
        public final long deltaNanos;

        private Match(PendingSound sound, PendingParticle particle) {
            this.sound = sound.packet;
            this.particle = particle.packet;
            this.soundReceivedNanos = sound.receivedNanos;
            this.particleReceivedNanos = particle.receivedNanos;
            this.deltaNanos = Math.abs(sound.receivedNanos - particle.receivedNanos);
        }

        public long orderNanos() {
            return soundReceivedNanos;
        }
    }

    private static final class PendingSound {
        final S29PacketSoundEffect packet;
        final long receivedNanos;
        final double x;
        final double y;
        final double z;

        PendingSound(S29PacketSoundEffect packet, long receivedNanos) {
            this.packet = packet;
            this.receivedNanos = receivedNanos;
            this.x = packet.getX();
            this.y = packet.getY();
            this.z = packet.getZ();
        }
    }

    private static final class PendingParticle {
        final S2APacketParticles packet;
        final long receivedNanos;
        final double x;
        final double y;
        final double z;

        PendingParticle(S2APacketParticles packet, long receivedNanos) {
            this.packet = packet;
            this.receivedNanos = receivedNanos;
            this.x = packet.getXCoordinate();
            this.y = packet.getYCoordinate();
            this.z = packet.getZCoordinate();
        }
    }

    private final ArrayDeque<PendingSound> pendingSounds = new ArrayDeque<>();
    private final ArrayDeque<PendingParticle> pendingParticles = new ArrayDeque<>();
    private final ArrayList<Match> readyPairs = new ArrayList<>();
    private long matchedCount;
    private long expiredSounds;
    private long expiredParticles;
    private long overflowCount;

    /**
     * Accepts one already-filtered packet. receivedNanos must be captured at packet ingress,
     * before the packet is handed from Netty to the client thread.
     */
    public void accept(Packet packet, long receivedNanos) {
        if (packet == null) return;
        expirePending(receivedNanos);

        if (packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect sound = (S29PacketSoundEffect) packet;
            if (!"note.harp".equals(sound.getSoundName())) return;
            PendingSound incoming = new PendingSound(sound, receivedNanos);
            int candidate = findParticleCandidate(incoming);
            if (candidate >= 0) {
                PendingParticle particle = removeParticle(candidate);
                addMatch(incoming, particle);
            } else {
                offerSound(incoming);
            }
            return;
        }

        if (packet instanceof S2APacketParticles) {
            S2APacketParticles particle = (S2APacketParticles) packet;
            if (particle.getParticleType() == null
                    || !"dripLava".equals(particle.getParticleType().getParticleName())) return;
            PendingParticle incoming = new PendingParticle(particle, receivedNanos);
            int candidate = findSoundCandidate(incoming);
            if (candidate >= 0) {
                PendingSound sound = removeSound(candidate);
                addMatch(sound, incoming);
            } else {
                offerParticle(incoming);
            }
        }
    }

    /**
     * Returns matched pairs after a short ordering guard. The 200 ms match window is still used
     * to associate late counterparts, but a completed pair no longer waits out that entire window.
     */
    public List<Match> drainReady(long nowNanos) {
        expirePending(nowNanos);
        if (readyPairs.isEmpty()) return Collections.emptyList();

        readyPairs.sort(Comparator.comparingLong(Match::orderNanos));
        long cutoff = nowNanos - PAIR_ORDER_GRACE_NANOS;
        ArrayList<Match> result = new ArrayList<>();
        Iterator<Match> iterator = readyPairs.iterator();
        while (iterator.hasNext()) {
            Match match = iterator.next();
            if (match.orderNanos() <= cutoff) {
                result.add(match);
                iterator.remove();
            } else {
                break;
            }
        }
        return result;
    }

    private int findParticleCandidate(PendingSound sound) {
        int bestIndex = -1;
        double bestScore = Double.MAX_VALUE;
        int index = 0;
        for (PendingParticle particle : pendingParticles) {
            long delta = Math.abs(sound.receivedNanos - particle.receivedNanos);
            if (delta > MATCH_WINDOW_NANOS || !sameEchoSource(sound.x, sound.y, sound.z,
                    particle.x, particle.y, particle.z)) {
                index++;
                continue;
            }
            double spatial = squaredDistance(sound.x, sound.y, sound.z, particle.x, particle.y, particle.z);
            double score = delta + spatial * 1_000_000.0;
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
            index++;
        }
        return bestIndex;
    }

    private int findSoundCandidate(PendingParticle particle) {
        int bestIndex = -1;
        double bestScore = Double.MAX_VALUE;
        int index = 0;
        for (PendingSound sound : pendingSounds) {
            long delta = Math.abs(particle.receivedNanos - sound.receivedNanos);
            if (delta > MATCH_WINDOW_NANOS || !sameEchoSource(sound.x, sound.y, sound.z,
                    particle.x, particle.y, particle.z)) {
                index++;
                continue;
            }
            double spatial = squaredDistance(sound.x, sound.y, sound.z, particle.x, particle.y, particle.z);
            double score = delta + spatial * 1_000_000.0;
            if (score < bestScore) {
                bestScore = score;
                bestIndex = index;
            }
            index++;
        }
        return bestIndex;
    }

    private void addMatch(PendingSound sound, PendingParticle particle) {
        if (readyPairs.size() >= MAX_READY_PAIRS) {
            readyPairs.remove(0);
            overflowCount++;
        }
        readyPairs.add(new Match(sound, particle));
        matchedCount++;
    }

    private void offerSound(PendingSound sound) {
        if (pendingSounds.size() >= MAX_PENDING_PER_TYPE) {
            pendingSounds.pollFirst();
            overflowCount++;
        }
        pendingSounds.addLast(sound);
    }

    private void offerParticle(PendingParticle particle) {
        if (pendingParticles.size() >= MAX_PENDING_PER_TYPE) {
            pendingParticles.pollFirst();
            overflowCount++;
        }
        pendingParticles.addLast(particle);
    }

    private PendingSound removeSound(int index) {
        Iterator<PendingSound> iterator = pendingSounds.iterator();
        for (int current = 0; iterator.hasNext(); current++) {
            PendingSound value = iterator.next();
            if (current == index) {
                iterator.remove();
                return value;
            }
        }
        return null;
    }

    private PendingParticle removeParticle(int index) {
        Iterator<PendingParticle> iterator = pendingParticles.iterator();
        for (int current = 0; iterator.hasNext(); current++) {
            PendingParticle value = iterator.next();
            if (current == index) {
                iterator.remove();
                return value;
            }
        }
        return null;
    }

    private void expirePending(long nowNanos) {
        while (!pendingSounds.isEmpty()
                && nowNanos - pendingSounds.peekFirst().receivedNanos > MATCH_WINDOW_NANOS) {
            pendingSounds.pollFirst();
            expiredSounds++;
        }
        while (!pendingParticles.isEmpty()
                && nowNanos - pendingParticles.peekFirst().receivedNanos > MATCH_WINDOW_NANOS) {
            pendingParticles.pollFirst();
            expiredParticles++;
        }
    }

    private static boolean sameEchoSource(double sx, double sy, double sz,
                                          double px, double py, double pz) {
        return Math.abs(px - sx) < 2.0
                && Math.abs(py - sy) < 0.5
                && Math.abs(pz - sz) < 2.0;
    }

    private static double squaredDistance(double x1, double y1, double z1,
                                          double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    public void clear() {
        pendingSounds.clear();
        pendingParticles.clear();
        readyPairs.clear();
    }

    public int getPendingSoundCount() {
        return pendingSounds.size();
    }

    public int getPendingParticleCount() {
        return pendingParticles.size();
    }

    public int getReadyPairCount() {
        return readyPairs.size();
    }

    public long getMatchedCount() {
        return matchedCount;
    }

    public long getExpiredSoundCount() {
        return expiredSounds;
    }

    public long getExpiredParticleCount() {
        return expiredParticles;
    }

    public long getOverflowCount() {
        return overflowCount;
    }
}
