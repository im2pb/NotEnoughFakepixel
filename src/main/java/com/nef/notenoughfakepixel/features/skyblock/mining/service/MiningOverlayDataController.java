/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

import java.util.Collections;
import java.util.List;

/**
 * Owns the mining HUD refresh lifecycle and the latest immutable presentation lines.
 * Call {@link #refresh()} from the client-tick adapter; render callbacks should only
 * call {@link #getLines()} and never query live game state.
 */
public final class MiningOverlayDataController {
    private static final MiningOverlayDataController INSTANCE = new MiningOverlayDataController();

    private final MiningOverlayDataProvider provider;
    private MiningOverlayDataSnapshot formattedSnapshot;
    private volatile List<String> lines = Collections.emptyList();
    private boolean sampledSinceReset;

    private MiningOverlayDataController() {
        this(new MiningOverlayDataProvider());
    }

    /** Visible for focused tests and isolated lifecycle owners. */
    public MiningOverlayDataController(MiningOverlayDataProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider must not be null");
        this.provider = provider;
    }

    public static MiningOverlayDataController getInstance() {
        return INSTANCE;
    }

    /** Samples current mining state and rebuilds presentation only when display data changes. */
    public void refresh() {
        MiningOverlayDataSnapshot snapshot = provider.collect();
        sampledSinceReset = true;
        if (snapshot == formattedSnapshot) return;

        List<String> updatedLines = MiningOverlayLineFormatter.format(snapshot);
        lines = updatedLines;
        formattedSnapshot = snapshot;
    }

    /** Returns the last tick-refreshed immutable lines; performs no game-state reads. */
    public List<String> getLines() {
        return lines;
    }

    /** Clears cached data when the client world unloads or the owner is reset. */
    public void reset() {
        // Disabled ticks can call reset repeatedly. Once cleared, avoid repeating
        // the provider reset and replacing the already-empty presentation list.
        if (!sampledSinceReset && formattedSnapshot == null && lines.isEmpty()) return;

        provider.reset();
        formattedSnapshot = null;
        lines = Collections.emptyList();
        sampledSinceReset = false;
    }
}
