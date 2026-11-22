/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows.treasure;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public final class TreasureTriangulator {

    private final double epsilon = 4.0;
    private final int maxSamples = 24;
    private final int minSamples = 4;
    private final double minPlayerDelta = 0.9; 
    private final long estimateCooldownMs = 250; 
    private final double pruneK = 2.5;

    private final int maxVolume = 200_000;

    private final RingBuffer samples;
    private final ReentrantLock samplesLock = new ReentrantLock();

    private volatile GridTrilateration.Result lastResult;
    private final AtomicBoolean estimatePending = new AtomicBoolean(false);
    private final AtomicLong resetGeneration = new AtomicLong();

    private double lastX = Double.NaN, lastY = Double.NaN, lastZ = Double.NaN, lastD = Double.NaN;

    private final ScheduledThreadPoolExecutor executor;
    private final AtomicBoolean workerScheduled = new AtomicBoolean(false);

    private static final TreasureTriangulator INSTANCE = new TreasureTriangulator();

    public static TreasureTriangulator getInstance() {
        return INSTANCE;
    }

    private TreasureTriangulator() {
        this.samples = new RingBuffer(64);
        this.executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "NEF-TreasureWorker");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        this.executor.setKeepAliveTime(15L, TimeUnit.SECONDS);
        this.executor.allowCoreThreadTimeOut(true);
        this.executor.setRemoveOnCancelPolicy(true);
    }

    public void shutdown() {
        executor.shutdownNow();
        workerScheduled.set(false);
    }

    /** Schedule one delayed calculation only when new samples arrive; do not wake an idle worker forever. */
    private void scheduleEstimate() {
        if (!workerScheduled.compareAndSet(false, true)) return;
        try {
            executor.schedule(() -> {
                try {
                    tryEstimateOnWorker();
                } finally {
                    workerScheduled.set(false);
                    // If samples arrived while the previous estimate was running, process one fresh snapshot.
                    if (estimatePending.get() && !executor.isShutdown()) scheduleEstimate();
                }
            }, estimateCooldownMs, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            workerScheduled.set(false);
        }
    }

    public void handleData(double playerX, double playerY, double playerZ, double distance) {
        if (Double.isNaN(playerX) || Double.isNaN(playerY) || Double.isNaN(playerZ)
                || Double.isInfinite(playerX) || Double.isInfinite(playerY) || Double.isInfinite(playerZ)
                || Double.isNaN(distance) || Double.isInfinite(distance) || distance <= 0.0) return;
        if (!shouldAccept(playerX, playerY, playerZ, distance)) return;

        samplesLock.lock();
        try {
            samples.add(new GridTrilateration.Sample(playerX, playerY, playerZ, distance));
            while (samples.size() > maxSamples) samples.poll();
        } finally {
            samplesLock.unlock();
        }
        estimatePending.set(true);
        scheduleEstimate();
    }

    private void tryEstimateOnWorker() {
        // Consume one batch. New arrivals during this calculation set the flag again for one follow-up run.
        if (!estimatePending.compareAndSet(true, false)) return;
        final long generationAtStart = resetGeneration.get();
        GridTrilateration.Sample[] snap;
        samplesLock.lock();
        try {
            int n = samples.size();
            if (n < minSamples) return;
            snap = samples.toArray(new GridTrilateration.Sample[n]);
        } finally {
            samplesLock.unlock();
        }

        ArrayList<GridTrilateration.Sample> buf = new ArrayList<>(snap.length);
        Collections.addAll(buf, snap);

        GridTrilateration.Result r = safeEstimateWithCap(buf, epsilon, maxVolume);

        if (r != null && buf.size() > minSamples) {
            final GridTrilateration.Int3 p = r.best;
            final double thr = pruneK * epsilon;
            boolean removed = false;
            for (Iterator<GridTrilateration.Sample> it = buf.iterator(); it.hasNext(); ) {
                GridTrilateration.Sample s = it.next();
                double resid = residual(p.x, p.y, p.z, s);
                if (resid > thr) {
                    it.remove();
                    removed = true;
                }
            }
            if (removed && buf.size() >= minSamples) {
                GridTrilateration.Result r2 = safeEstimateWithCap(buf, epsilon, maxVolume);
                if (r2 != null) r = r2;
            }
        }

        if (r != null) {
            samplesLock.lock();
            try {
                if (generationAtStart == resetGeneration.get()) lastResult = r;
            } finally {
                samplesLock.unlock();
            }
        }
    }

    public GridTrilateration.Int3 getBestGuess() {
        return (lastResult == null) ? null : lastResult.best;
    }

    public double getConfidence() {
        if (lastResult == null) return 0.0;
        double c = lastResult.gapToNext / (1.0 + lastResult.score);
        if (c < 0) c = 0;
        else if (c > 1) c = 1;
        return c;
    }

    public String getDebugString() {
        if (lastResult == null) return "Triangulation: getting samples...";
        return "Triangulation: " + lastResult.toString() + "  conf=" + String.format(Locale.US, "%.2f", getConfidence());
    }

    public void reset() {
        samplesLock.lock();
        try {
            // Advance the generation while holding the sample lock so an in-flight estimate
            // based on pre-reset data can never publish after this reset completes.
            resetGeneration.incrementAndGet();
            estimatePending.set(false);
            samples.clear();
            lastResult = null;
            lastX = lastY = lastZ = lastD = Double.NaN;
        } finally {
            samplesLock.unlock();
        }
    }

    private static GridTrilateration.Result safeEstimateWithCap(List<GridTrilateration.Sample> list, double eps, int cap) {
        try {
            
            return GridTrilateration.estimate(list, eps, (int) Math.ceil(Math.max(2, eps)), cap);
            
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static double residual(int x, int y, int z, GridTrilateration.Sample s) {
        double dx = x - s.ox, dy = y - s.oy, dz = z - s.oz;
        return Math.abs(Math.sqrt(dx * dx + dy * dy + dz * dz) - s.d);
    }

    private boolean shouldAccept(double x, double y, double z, double d) {
        if (Double.isNaN(lastX)) {
            lastX = x;
            lastY = y;
            lastZ = z;
            lastD = d;
            return true;
        }
        double dx = x - lastX, dy = y - lastY, dz = z - lastZ;
        double move2 = dx * dx + dy * dy + dz * dz;
        double distDelta = Math.abs(d - lastD);
        if (move2 >= (minPlayerDelta * minPlayerDelta) || distDelta >= 0.75) {
            lastX = x;
            lastY = y;
            lastZ = z;
            lastD = d;
            return true;
        }
        return false;
    }

    private static final class RingBuffer {
        private GridTrilateration.Sample[] a;
        private int head = 0, tail = 0, size = 0;

        RingBuffer(int cap) {
            a = new GridTrilateration.Sample[cap];
        }

        int size() {
            return size;
        }

        void clear() {
            head = tail = size = 0;
        }

        void add(GridTrilateration.Sample s) {
            if (size == a.length) grow();
            a[tail] = s;
            tail = (tail + 1) % a.length;
            size++;
        }

        GridTrilateration.Sample poll() {
            if (size == 0) return null;
            GridTrilateration.Sample s = a[head];
            a[head] = null;
            head = (head + 1) % a.length;
            size--;
            return s;
        }

        GridTrilateration.Sample[] toArray(GridTrilateration.Sample[] out) {
            int n = size, i = 0, idx = head;
            while (i < n) {
                out[i++] = a[idx];
                idx = (idx + 1) % a.length;
            }
            return out;
        }

        GridTrilateration.Sample[] toArray() {
            GridTrilateration.Sample[] out = new GridTrilateration.Sample[size];
            return toArray(out);
        }

        private void grow() {
            int n = a.length, m = n << 1;
            GridTrilateration.Sample[] b = new GridTrilateration.Sample[m];
            toArray(b);
            a = b;
            head = 0;
            tail = size;
        }
    }
}
