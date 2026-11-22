/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.crimson;

import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;

public class Crimson {

    private static final int[][] ashfangArea = new int[][]{{-510, 100, -1040}, {-450, 200, -990}};

    public static boolean checkEssentials() {
        return (Minecraft.getMinecraft().thePlayer == null) ||
                (!SkyblockData.getCurrentGamemode().isSkyblock()) ||
                (!SkyblockData.getCurrentLocation().isCrimson());
    }

    public static boolean checkAshfangArea(int[] coords) {
        return coords != null && coords.length >= 3
                && checkAshfangArea(coords[0], coords[1], coords[2]);
    }

    public static boolean checkAshfangArea(int x, int y, int z) {
        return x >= ashfangArea[0][0] && x <= ashfangArea[1][0]
                && y >= ashfangArea[0][1] && y <= ashfangArea[1][1]
                && z >= ashfangArea[0][2] && z <= ashfangArea[1][2];
    }
}
