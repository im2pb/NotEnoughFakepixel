/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.services;

import net.minecraft.client.Minecraft;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared scheduler for Diana modules.
 *
 * Delayed work is performed off-thread only for timing. Any Minecraft state access is dispatched back to
 * the client thread. A generation token invalidates work that was already queued when the world changes.
 */
public final class DianaSchedulerService implements AutoCloseable {
    private final ScheduledThreadPoolExecutor executor;
    private final AtomicLong generation = new AtomicLong();
    private volatile boolean closed;

    public DianaSchedulerService() {
        executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "NEF-Diana-Scheduler");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    /** Dispatches a task to Minecraft's client thread. Tasks invalidated by a world reset are ignored. */
    public void executeOnClientThread(Runnable task) {
        if (task == null || closed) return;

        final long taskGeneration = generation.get();
        final Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) return;

        minecraft.addScheduledTask(() -> {
            if (!closed && generation.get() == taskGeneration) task.run();
        });
    }

    /** Schedules a delay, then dispatches the task to Minecraft's client thread. */
    public void scheduleOnClientThread(Runnable task, long delay, TimeUnit unit) {
        if (task == null || unit == null || closed) return;
        final long taskGeneration = generation.get();
        final long safeDelay = Math.max(0L, delay);
        executor.schedule(() -> {
            if (closed || generation.get() != taskGeneration) return;
            final Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null) return;
            minecraft.addScheduledTask(() -> {
                if (!closed && generation.get() == taskGeneration) task.run();
            });
        }, safeDelay, unit);
    }

    /** Invalidates queued work and removes delayed tasks without shutting down the service. */
    public void clearPendingTasks() {
        generation.incrementAndGet();
        for (Runnable queuedTask : executor.getQueue()) {
            executor.remove(queuedTask);
        }
        executor.purge();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        clearPendingTasks();
        executor.shutdownNow();
    }

    public boolean isClosed() {
        return closed;
    }
}
