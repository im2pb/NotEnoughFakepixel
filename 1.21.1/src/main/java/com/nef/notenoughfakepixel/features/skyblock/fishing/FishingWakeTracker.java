package com.nef.notenoughfakepixel.features.skyblock.fishing;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class FishingWakeTracker {
    private static final long WAKE_LIFETIME = 200;
    private static final int MAX_TRAILS = 32;
    private static final double MAX_STEP_SQUARED = 0.45 * 0.45;
    private static final double MAX_TURN = Math.toRadians(16);
    private static final double MIN_PROGRESS = 0.001;
    private static final int MIN_APPROACH_STEPS = 3;

    private final List<Trail> trails = new ArrayList<>();

    boolean observe(Vec3d position, double axis, double verticalTolerance, List<Bobber> bobbers, int localId, long now) {
        expire(now);
        Map<Integer, Double> distances = alignedBobbers(position, axis, verticalTolerance, bobbers);
        if (!distances.containsKey(localId)) return false;

        Trail best = null;
        Map<Integer, Progress> bestProgress = null;
        double closestStep = Double.POSITIVE_INFINITY;
        for (Trail trail : trails) {
            double step = trail.position.squaredDistanceTo(position);
            if (step > MAX_STEP_SQUARED || axisDifference(trail.axis, axis) > MAX_TURN) continue;
            // Vanilla sends two opposite wakes at the same position. They represent one sample.
            if (step == 0 && trail.candidates.containsKey(localId)) return false;
            Map<Integer, Progress> progress = trail.advance(distances);
            if (step < closestStep && progress.containsKey(localId)) {
                best = trail;
                bestProgress = progress;
                closestStep = step;
            }
        }

        if (best != null) {
            best.position = position;
            best.axis = axis;
            best.lastSeen = now;
            best.candidates = bestProgress;
        } else {
            if (trails.size() == MAX_TRAILS) trails.removeFirst();
            Map<Integer, Progress> candidates = new HashMap<>();
            distances.forEach((id, distance) -> candidates.put(id, new Progress(distance, distance, now, 0)));
            trails.add(new Trail(position, axis, now, candidates));
        }
        return true;
    }

    Approach approach(int localId, long now) {
        Approach closest = null;
        for (Trail trail : trails) {
            if (now - trail.lastSeen >= WAKE_LIFETIME || trail.candidates.size() != 1) continue;
            Progress progress = trail.candidates.get(localId);
            if (progress == null || progress.steps() < MIN_APPROACH_STEPS) continue;
            if (closest != null && progress.distance() >= closest.distance()) continue;
            long elapsed = trail.lastSeen - progress.firstSeen();
            double travelled = progress.firstDistance() - progress.distance();
            double eta = elapsed > 0 && travelled > MIN_PROGRESS && progress.distance() > 0
                    ? (progress.distance() / travelled) * elapsed : 0;
            long arrival = eta > 0 && eta < 10_000 ? trail.lastSeen + Math.round(eta) : 0;
            closest = new Approach(progress.distance(), arrival);
        }
        return closest;
    }

    void expire(long now) {
        trails.removeIf(trail -> now - trail.lastSeen >= WAKE_LIFETIME);
    }

    void clear() {
        trails.clear();
    }

    private static Map<Integer, Double> alignedBobbers(Vec3d position, double axis, double tolerance, List<Bobber> bobbers) {
        Map<Integer, Double> distances = new HashMap<>();
        for (Bobber bobber : bobbers) {
            Vec3d hook = bobber.position();
            if (Math.abs(hook.y - position.y) > tolerance) continue;
            double dx = hook.x - position.x;
            double dz = hook.z - position.z;
            double distance = Math.hypot(dx, dz);
            double margin = Math.atan2(0.03125, distance) * 1.5;
            if (distance < 0.2 || axisDifference(axis, Math.atan2(dx, dz)) <= margin) {
                distances.put(bobber.id(), distance);
            }
        }
        return distances;
    }

    // Wake packets can point in either direction along the same axis.
    private static double axisDifference(double first, double second) {
        return Math.abs(Math.IEEEremainder(first - second, Math.PI));
    }

    record Bobber(int id, Vec3d position) {
    }

    record Approach(double distance, long arrival) {
    }

    private record Progress(double distance, double firstDistance, long firstSeen, int steps) {
    }

    private static final class Trail {
        private Vec3d position;
        private double axis;
        private long lastSeen;
        private Map<Integer, Progress> candidates;

        private Trail(Vec3d position, double axis, long now, Map<Integer, Progress> candidates) {
            this.position = position;
            this.axis = axis;
            lastSeen = now;
            this.candidates = candidates;
        }

        private Map<Integer, Progress> advance(Map<Integer, Double> distances) {
            Map<Integer, Progress> progress = new HashMap<>();
            for (var entry : candidates.entrySet()) {
                Double distance = distances.get(entry.getKey());
                if (distance == null) continue;
                Progress previous = entry.getValue();
                double delta = previous.distance() - distance;
                if (distance >= 0.2 && (delta <= -0.1 || delta >= 0.3)) continue;
                int steps = previous.steps() + (delta > MIN_PROGRESS ? 1 : 0);
                progress.put(entry.getKey(), new Progress(distance, previous.firstDistance(), previous.firstSeen(), steps));
            }
            return progress;
        }
    }
}
