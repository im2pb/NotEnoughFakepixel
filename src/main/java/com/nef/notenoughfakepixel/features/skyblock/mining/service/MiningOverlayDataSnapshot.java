/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reusable immutable display snapshot for the mining overlay.
 * It contains no Minecraft, Forge, configuration, or rendering dependencies.
 * The cooldown sample may be reused within the same displayed whole second;
 * it refreshes when the displayed second changes or the ability becomes ready.
 */
public final class MiningOverlayDataSnapshot {
    private static final MiningOverlayDataSnapshot EMPTY = new MiningOverlayDataSnapshot(
            -1L, false, 0, false, 0, false, 0, 0, Collections.<String>emptyList());

    private final long abilityCooldownRemainingMillis;
    private final boolean showMithrilPowder;
    private final int mithrilPowder;
    private final boolean showGemstonePowder;
    private final int gemstonePowder;
    private final boolean showDrillFuel;
    private final int drillFuel;
    private final int drillFuelMaximum;
    private final List<String> commissions;

    public MiningOverlayDataSnapshot(long abilityCooldownRemainingMillis,
                                     boolean showMithrilPowder,
                                     int mithrilPowder,
                                     boolean showGemstonePowder,
                                     int gemstonePowder,
                                     boolean showDrillFuel,
                                     int drillFuel,
                                     int drillFuelMaximum,
                                     List<String> commissions) {
        this.abilityCooldownRemainingMillis = abilityCooldownRemainingMillis;
        this.showMithrilPowder = showMithrilPowder;
        this.mithrilPowder = mithrilPowder;
        this.showGemstonePowder = showGemstonePowder;
        this.gemstonePowder = gemstonePowder;
        this.showDrillFuel = showDrillFuel;
        this.drillFuel = drillFuel;
        this.drillFuelMaximum = drillFuelMaximum;
        if (commissions == null || commissions.isEmpty()) {
            this.commissions = Collections.emptyList();
        } else {
            this.commissions = Collections.unmodifiableList(new ArrayList<String>(commissions));
        }
    }

    public static MiningOverlayDataSnapshot empty() {
        return EMPTY;
    }

    /**
     * Compares only values that can affect the visible HUD output. Cooldowns are
     * compared at their displayed whole-second precision, while keeping the
     * ready/disabled states distinct. This lets the provider reuse the previous
     * immutable snapshot between visible changes instead of allocating per frame.
     */
    public boolean hasSameDisplayValues(long cooldownRemainingMillis,
                                        boolean showMithrilPowder,
                                        int mithrilPowder,
                                        boolean showGemstonePowder,
                                        int gemstonePowder,
                                        boolean showDrillFuel,
                                        int drillFuel,
                                        int drillFuelMaximum,
                                        List<String> commissions) {
        return sameCooldownDisplay(this.abilityCooldownRemainingMillis, cooldownRemainingMillis)
                && this.showMithrilPowder == showMithrilPowder
                && this.mithrilPowder == mithrilPowder
                && this.showGemstonePowder == showGemstonePowder
                && this.gemstonePowder == gemstonePowder
                && this.showDrillFuel == showDrillFuel
                && this.drillFuel == drillFuel
                && this.drillFuelMaximum == drillFuelMaximum
                && this.commissions.equals(commissions == null ? Collections.<String>emptyList() : commissions);
    }

    private static boolean sameCooldownDisplay(long first, long second) {
        if (first < 0L || second < 0L) return first == second;
        if ((first == 0L) != (second == 0L)) return false;
        return first / 1000L == second / 1000L;
    }

    /**
     * Returns the most recently sampled remaining milliseconds, -1 when disabled,
     * or zero when ready. A reused display snapshot may retain this sample until
     * the whole-second text changes.
     */
    public long getAbilityCooldownRemainingMillis() { return abilityCooldownRemainingMillis; }
    public boolean shouldShowMithrilPowder() { return showMithrilPowder; }
    public int getMithrilPowder() { return mithrilPowder; }
    public boolean shouldShowGemstonePowder() { return showGemstonePowder; }
    public int getGemstonePowder() { return gemstonePowder; }
    public boolean shouldShowDrillFuel() { return showDrillFuel; }
    public int getDrillFuel() { return drillFuel; }
    public int getDrillFuelMaximum() { return drillFuelMaximum; }
    public List<String> getCommissions() { return commissions; }
}
