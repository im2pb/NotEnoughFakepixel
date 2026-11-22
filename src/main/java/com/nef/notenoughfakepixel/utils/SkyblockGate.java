/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class SkyblockGate {
    private SkyblockGate() {}

    private static final class HostCheck {
        final String ip;
        final boolean fakepixel;

        HostCheck(String ip, boolean fakepixel) {
            this.ip = ip;
            this.fakepixel = fakepixel;
        }
    }

    private static volatile HostCheck lastCheck = new HostCheck(null, false);

    public static boolean isOnFakepixel() {
        try {
            ServerData data = Minecraft.getMinecraft().getCurrentServerData();
            if (data == null || data.serverIP == null) return false;

            String ip = data.serverIP;
            HostCheck check = lastCheck;
            if (check.ip != ip) {
                check = new HostCheck(ip, ip.toLowerCase(java.util.Locale.ROOT).contains("fakepixel"));
                lastCheck = check;
            }
            return check.fakepixel;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isFakepixelSkyblock() {
        return isOnFakepixel() && SkyblockData.getCurrentGamemode().isSkyblock();
    }

    public static boolean isFakepixelDungeon() {
        return isFakepixelSkyblock() && SkyblockData.getCurrentLocation().isDungeon();
    }
}
