/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.service;

import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.variables.Colors;
import net.minecraft.item.ItemStack;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Owns drill-fuel extraction, parsing, and display formatting. */
public final class DrillFuelDataService {
    private static final Pattern FUEL_PATTERN = Pattern.compile("§7Fuel: §2([\\d,]+)§8/(\\d+)k");

    private int fuel;
    private int fuelMax;

    public void updateFromHeldItem(ItemStack heldItem) {
        if (heldItem == null) return;

        int parsedFuel = ItemUtils.getExtraAttributesIntTag(heldItem, "fuel");
        if (parsedFuel != -1) fuel = parsedFuel;

        String fuelLore = ItemUtils.getLoreLine(heldItem, "Fuel:");
        if (fuelLore == null) return;
        int parsedMaximum = parseMaximum(fuelLore);
        if (parsedMaximum >= 0) fuelMax = parsedMaximum;
    }

    /** Returns the maximum fuel in units, or -1 when the lore is not recognized. */
    public static int parseMaximum(String fuelLore) {
        if (fuelLore == null) return -1;
        Matcher matcher = FUEL_PATTERN.matcher(fuelLore);
        if (!matcher.find()) return -1;
        try {
            return Integer.parseInt(matcher.group(2).replace(",", "")) * 1000;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public String format() {
        return formatFuel(fuel, fuelMax);
    }

    public String format(int currentFuel, int maximumFuel) {
        return formatFuel(currentFuel, maximumFuel);
    }

    /** Pure display formatter used by the mining HUD presentation layer. */
    public static String formatFuel(int currentFuel, int maximumFuel) {
        Colors color = currentFuel > maximumFuel * 0.5 ? Colors.GREEN
                : currentFuel > maximumFuel * 0.25 ? Colors.YELLOW : Colors.RED;
        return Colors.GRAY + "Drill Fuel: " + color + currentFuel
                + Colors.GRAY + "/" + maximumFuel;
    }

    public int getFuel() { return fuel; }
    public int getFuelMax() { return fuelMax; }
}
