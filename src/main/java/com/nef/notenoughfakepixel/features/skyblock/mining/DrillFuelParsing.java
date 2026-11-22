/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.mining.service.DrillFuelDataService;
import com.nef.notenoughfakepixel.features.skyblock.mining.service.DrillFuelSamplingService;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Forge event adapter for drill-fuel sampling. */
@RegisterEvents
public class DrillFuelParsing implements GatedListener {
    private static final DrillFuelSamplingService samplingService = DrillFuelSamplingService.getInstance();

    @Override
    public boolean isListenerActive() {
        return Config.feature.mining.miningDrillFuel;
    }

    /** Legacy compatibility mirror; the sampling service owns the actual counter. */
    public static int lastRead = 0;
    /** Legacy compatibility mirrors; DrillFuelDataService owns the actual values. */
    public static int fuel = 0;
    public static int fuelMax = 0;

    /** Retained for binary/source compatibility with callers that referenced this field. */
    public static Matcher fuelMatcher = Pattern.compile("§7Fuel: §2([\\d,]+)§8/(\\d+)k").matcher("");

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        // Deliberately preserve the original phase behavior: each event dispatch is
        // one sampling opportunity, and the legacy counter threshold remains 120.
        if (lastRead != samplingService.getLastRead()) {
            samplingService.setLastRead(lastRead);
        }
        boolean sampled = samplingService.sampleOnTick();
        lastRead = samplingService.getLastRead();
        if (sampled) {
            fuel = samplingService.getFuel();
            fuelMax = samplingService.getFuelMax();
        }
    }

    public static String getString() {
        return DrillFuelDataService.formatFuel(fuel, fuelMax);
    }
}
