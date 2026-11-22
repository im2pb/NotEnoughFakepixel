/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

/**
 * Mutable metadata collected while a dungeon run is active.
 * Keeps chat-derived values and server-clock anchoring state together,
 * separate from the run lifecycle and phase timeline.
 */
final class DungeonRunMetadata {
    private long dungeonXp = -1L;
    private long serverClearMs = -1L;
    private boolean startAnchored;
    private String tabClockAtBegin = "";

    void begin(String tabClock) {
        reset();
        tabClockAtBegin = tabClock == null ? "" : tabClock;
    }

    void apply(long serverClearTimeMs, long xp) {
        if (serverClearTimeMs >= 0L) serverClearMs = serverClearTimeMs;
        if (xp >= 0L) dungeonXp = xp;
    }

    void markStartAnchored() {
        startAnchored = true;
    }

    void reset() {
        dungeonXp = -1L;
        serverClearMs = -1L;
        startAnchored = false;
        tabClockAtBegin = "";
    }

    long getDungeonXp() { return dungeonXp; }
    long getServerClearMs() { return serverClearMs; }
    boolean isStartAnchored() { return startAnchored; }
    String getTabClockAtBegin() { return tabClockAtBegin; }
}
