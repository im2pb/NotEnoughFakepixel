/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.detection;

import java.util.HashMap;
import java.util.Map;

/** Hub landmark lookup used to annotate Minos Inquisitor party alerts. */
public final class InquisitorLocationRegistry {
    private final Map<String, int[]> locations = new HashMap<>();
    private boolean initialized;

    public void initialize() {
        if (initialized) return;
        locations.clear();
        locations.put("Farm", new int[]{60, 72, -173});
        locations.put("Above Coal mine", new int[]{-40, 85, -196});
        // Retained legacy replacement: the second Farm entry intentionally remains the effective value.
        locations.put("Farm", new int[]{81, 72, -140});
        locations.put("2pb NPC", new int[]{83, 72, -102});
        locations.put("Colosseum left side", new int[]{101, 72, -73});
        locations.put("Wizard tower", new int[]{53, 72, 66});
        locations.put("Spider den portal", new int[]{-120, 76, -177});
        locations.put("Crypt entrance", new int[]{-173, 74, -92});
        locations.put("Dante statue in the graveyard", new int[]{-98, 72, -135});
        locations.put("Graveyard entrance", new int[]{-120, 71, -77});
        locations.put("Lumber jack npc", new int[]{-114, 74, -32});
        locations.put("Park portal", new int[]{-192, 74, -23});
        locations.put("Castle / ruins", new int[]{-209, 91, 70});
        locations.put("Museum", new int[]{-108, 68, 102});
        locations.put("High level", new int[]{-6, 71, 164});
        locations.put("Dark auction hut", new int[]{84, 74, 176});
        locations.put("Fairy lake in wilderness", new int[]{110, 66, 114});
        locations.put("Colosseum", new int[]{143, 76, -17});
        initialized = true;
    }

    public String findNearest(int x, int y, int z) {
        if (!initialized) initialize();
        String nearestLocation = null;
        double minDistance = Double.MAX_VALUE;
        for (Map.Entry<String, int[]> entry : locations.entrySet()) {
            int[] coords = entry.getValue();
            double distance = Math.sqrt(Math.pow(x - coords[0], 2)
                    + Math.pow(y - coords[1], 2) + Math.pow(z - coords[2], 2));
            if (distance < minDistance) {
                minDistance = distance;
                nearestLocation = entry.getKey();
            }
        }
        return nearestLocation;
    }

    public void clear() {
        locations.clear();
        initialized = false;
    }
}
