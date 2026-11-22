/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

/**
 * Owns the clock and reminder state for the mining pickaxe ability cooldown.
 * It deliberately has no Minecraft event or world dependencies so the timing
 * rules can be tested independently from the event adapter.
 */
public final class MiningAbilityCooldownService {

    public static final long COOLDOWN_MILLIS = 120000L;

    private long lastUsed = -1L;
    private long readyAt = -1L;
    private boolean reminderPending;

    public void recordAbilityUse(long timestampMillis) {
        lastUsed = timestampMillis;
        readyAt = timestampMillis + COOLDOWN_MILLIS;
        reminderPending = true;
    }

    public boolean isReminderDue(long nowMillis) {
        return lastUsed >= 0L && readyAt >= 0L && reminderPending && nowMillis >= readyAt;
    }

    public void markReminderDisplayed() {
        reminderPending = false;
    }

    public long getLastUsedMillis() {
        return lastUsed;
    }

    public long getReadyAtMillis() {
        return readyAt;
    }

    public long getRemainingMillis(long nowMillis) {
        if (lastUsed < 0L || readyAt < 0L) return 0L;
        return Math.max(0L, readyAt - nowMillis);
    }

    /**
     * Legacy string helper retained for source compatibility with the initial service extraction.
     * New event/render adapters should format from {@link #getRemainingMillis(long)} instead.
     * The method uses the stable Minecraft formatting code as a plain string, keeping this service
     * independent from Minecraft and Forge classes.
     *
     * @deprecated Prefer raw remaining milliseconds and format at the presentation boundary.
     */
    @Deprecated
    public String getFormattedTimeRemaining(long nowMillis, String readyMessage) {
        long remaining = getRemainingMillis(nowMillis);
        if (lastUsed < 0L || remaining <= 0L) return readyMessage;
        return "\u00a7c" + (remaining / 1000L) + "s";
    }

}
