/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.serverdata;

import com.nef.notenoughfakepixel.variables.*;
import lombok.Getter;
import lombok.Setter;

public class SkyblockData {

    @Getter @Setter private static String currentProfile = null; 
    
    @Getter @Setter private static boolean isSlayerActive = false;
    @Getter @Setter private static boolean isBossActive = false;
    @Getter @Setter private static Slayer currentSlayer = Slayer.NONE;

    // Session-local Slayer progress, populated from server chat while the overlay is active.
    @Getter @Setter private static int slayerLevel = 0;
    @Getter @Setter private static int slayerXp = 0;
    @Getter @Setter private static int slayerNextLevelXp = 0;
    @Getter @Setter private static int slayerXpToNextLevel = 0;
    @Getter @Setter private static float slayerRngesusMeter = 0.0f;
    @Getter @Setter private static int slayerSessionBosses = 0;
    @Getter @Setter private static long slayerSessionSeconds = 0L;
    
    @Getter @Setter private static Location currentLocation = Location.NONE;
    @Getter @Setter private static Gamemode currentGamemode = Gamemode.LOBBY;
    @Getter @Setter private static Area currentArea = Area.NONE;
    
    @Getter @Setter private static int sbHour = 0;
    @Getter @Setter private static int sbMinute = 0;
    @Getter @Setter private static boolean am = false;
    @Getter @Setter private static Season season = Season.SPRING;
    
    @Getter @Setter private static int mithrilPowder = 0;
    @Getter @Setter private static int gemstonePowder = 0;
    @Getter @Setter private static int heat = 0;
    
    @Getter @Setter private static DungeonFloor currentFloor = DungeonFloor.NONE;
    @Getter @Setter private static int clearedPercentage = -1;

    private SkyblockData() {}

    public enum Season {
        NONE,
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER;

        public static Season getByName(String name) {
            for (Season season : Season.values()) {
                if (season.name().equals(name.toUpperCase())) {
                    return season;
                }
            }

            return NONE;
        }
    }

}
