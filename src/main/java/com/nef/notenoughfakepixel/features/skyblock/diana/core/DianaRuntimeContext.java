/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.core;

import com.nef.notenoughfakepixel.features.skyblock.diana.services.DianaSchedulerService;

/** Shared runtime services owned by the Diana feature module. */
public final class DianaRuntimeContext implements AutoCloseable {
    private final DianaSchedulerService scheduler = new DianaSchedulerService();
    private boolean closed;

    public DianaSchedulerService getScheduler() {
        return scheduler;
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        scheduler.close();
    }

    public synchronized boolean isClosed() {
        return closed;
    }
}
