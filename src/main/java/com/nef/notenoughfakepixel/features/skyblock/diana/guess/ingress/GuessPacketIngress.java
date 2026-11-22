/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.guess.ingress;

import com.nef.notenoughfakepixel.features.skyblock.diana.services.DianaSchedulerService;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Hands relevant Guess packets from Netty to the client thread in arrival order.
 * The receive timestamp is preserved so Harp/particle correlation is independent of client-tick batching.
 */
public final class GuessPacketIngress {
    private static final int MAX_PENDING_PACKETS = 256;
    private static final int MAX_PACKETS_PER_CLIENT_DISPATCH = 128;

    private static final class QueuedPacket {
        final Packet packet;
        final long receivedNanos;

        QueuedPacket(Packet packet, long receivedNanos) {
            this.packet = packet;
            this.receivedNanos = receivedNanos;
        }
    }

    private final DianaSchedulerService scheduler;
    private final BiConsumer<Packet, Long> packetConsumer;
    private final Queue<QueuedPacket> pendingPackets = new ArrayDeque<>();
    private final AtomicBoolean drainScheduled = new AtomicBoolean(false);

    public GuessPacketIngress(DianaSchedulerService scheduler, BiConsumer<Packet, Long> packetConsumer) {
        if (scheduler == null) throw new IllegalArgumentException("Diana scheduler cannot be null");
        if (packetConsumer == null) throw new IllegalArgumentException("Packet consumer cannot be null");
        this.scheduler = scheduler;
        this.packetConsumer = packetConsumer;
    }

    /** May be called by Netty. Packet contents are immutable; no Minecraft world state is accessed here. */
    public void accept(Packet packet) {
        accept(packet, System.nanoTime());
    }

    public void accept(Packet packet, long receivedNanos) {
        if (!isRelevant(packet)) return;

        synchronized (pendingPackets) {
            if (pendingPackets.size() >= MAX_PENDING_PACKETS) {
                pendingPackets.poll();
            }
            pendingPackets.offer(new QueuedPacket(packet, receivedNanos));
        }
        scheduleDrain();
    }

    private boolean isRelevant(Packet packet) {
        if (packet instanceof S29PacketSoundEffect) {
            return "note.harp".equals(((S29PacketSoundEffect) packet).getSoundName());
        }
        if (packet instanceof S2APacketParticles) {
            S2APacketParticles particles = (S2APacketParticles) packet;
            return particles.getParticleType() != null
                    && "dripLava".equals(particles.getParticleType().getParticleName());
        }
        return false;
    }

    private void scheduleDrain() {
        if (drainScheduled.compareAndSet(false, true)) {
            scheduler.executeOnClientThread(this::drainOnClientThread);
        }
    }

    private void drainOnClientThread() {
        try {
            drainBatch();
        } finally {
            // Any remaining bounded batch is drained by the next client tick; avoid recursive
            // rescheduling when Minecraft executes addScheduledTask inline on the client thread.
            drainScheduled.set(false);
        }
    }

    /** Also called from the listener's client tick to make progress if the scheduled drain was coalesced. */
    public void drainPendingOnClientThread() {
        drainBatch();
    }

    private void drainBatch() {
        for (int processed = 0; processed < MAX_PACKETS_PER_CLIENT_DISPATCH; processed++) {
            QueuedPacket queued;
            synchronized (pendingPackets) {
                queued = pendingPackets.poll();
            }
            if (queued == null) break;
            packetConsumer.accept(queued.packet, queued.receivedNanos);
        }
    }

    /** Clears queued packets during a world transition, listener deactivation, or shutdown. */
    public void clear() {
        synchronized (pendingPackets) {
            pendingPackets.clear();
        }
        drainScheduled.set(false);
    }

    public int getPendingPacketCount() {
        synchronized (pendingPackets) {
            return pendingPackets.size();
        }
    }
}
