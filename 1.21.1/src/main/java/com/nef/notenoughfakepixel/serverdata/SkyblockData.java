package com.nef.notenoughfakepixel.serverdata;

import com.nef.notenoughfakepixel.variables.Area;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import com.nef.notenoughfakepixel.variables.Gamemode;
import com.nef.notenoughfakepixel.variables.Location;
import com.nef.notenoughfakepixel.variables.Mayor;
import com.nef.notenoughfakepixel.variables.Slayer;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Accessors(fluent = true)
public final class SkyblockData {
    // Profile
    @Getter private static String currentProfile;

    // Slayer
    @Getter private static boolean slayerActive;
    @Getter private static boolean bossActive;
    @Getter private static Slayer currentSlayer = Slayer.NONE;
    @Getter private static int slayerLevel;
    @Getter private static int slayerXp;
    @Getter private static int nextLevelXp;
    @Getter private static int xpToNextLevel;
    @Getter private static int sessionBosses;
    @Getter private static float rngesusMeter;
    @Getter private static double totalSeconds;

    // Location and held item
    @Getter private static Location currentLocation = Location.NONE;
    @Getter private static Gamemode currentGamemode = Gamemode.LOBBY;
    @Getter private static Area currentArea = Area.NONE;
    @Getter private static Mayor currentMayor = Mayor.NONE;
    @Getter private static boolean sandbox;
    @Getter private static String lastHandItemId = "";

    // SkyBlock clock
    @Getter private static int sbHour;
    @Getter private static int sbMinute;
    @Getter private static boolean am;
    @Getter private static Season season = Season.SPRING;

    // Mining
    @Getter private static int mithrilPowder;
    @Getter private static int gemstonePowder;
    @Getter private static List<String> commissions = List.of();
    @Getter private static int heat;

    // Dungeons
    @Getter private static DungeonFloor currentFloor = DungeonFloor.NONE;
    @Getter private static int clearedPercentage = -1;

    // Garden
    @Getter private static HashMap<Integer, Integer> activePests = new HashMap<>();
    @Getter private static List<String> activeVisitors = new ArrayList<>();
    @Getter private static List<String> cropMilestone = new ArrayList<>();

    private SkyblockData() {
    }

    public enum Season {
        NONE, SPRING, SUMMER, AUTUMN, WINTER;

        public static Season fromName(String name) {
            for (Season season : values()) {
                if (season.name().equalsIgnoreCase(name)) return season;
            }
            return NONE;
        }
    }

    public static boolean isSkyblock() {
        return currentGamemode.isSkyblock();
    }

    public static boolean isMayor(Mayor mayor) {
        return currentMayor == mayor;
    }

    public static void resetSlayerData() {
        updateSlayerProgress(0, 0, 0);
        sessionBosses = 0;
        rngesusMeter = 0;
        totalSeconds = 0;
    }

    static boolean changeProfile(String profile) {
        if (Objects.equals(currentProfile, profile)) return false;

        currentProfile = profile;
        clearSlayerProfile();
        return true;
    }

    static void clearProfile() {
        currentProfile = null;
        clearSlayerProfile();
    }

    static boolean observeSlayer(Slayer slayer) {
        if (slayer == Slayer.NONE || slayer == currentSlayer) return false;

        currentSlayer = slayer;
        resetSlayerData();
        return true;
    }

    static void updateSlayerActivity(boolean active, boolean fightingBoss) {
        slayerActive = active;
        bossActive = fightingBoss;
    }

    static void recordSlayerCompletion(double elapsedSeconds) {
        sessionBosses++;
        totalSeconds += elapsedSeconds;
    }

    static void updateSlayerProgress(int level, int remainingXp) {
        if (currentSlayer == Slayer.NONE || level < 0 || level > currentSlayer.maxLevel()) return;
        if (level == currentSlayer.maxLevel()) {
            updateSlayerProgress(level, 0, 0);
            return;
        }

        int threshold = currentSlayer.xpForNextLevel(level);
        if (remainingXp >= 0 && remainingXp <= threshold) {
            updateSlayerProgress(level, threshold, remainingXp);
        }
    }

    private static void updateSlayerProgress(int level, int threshold, int remainingXp) {
        slayerLevel = level;
        slayerXp = threshold - remainingXp;
        nextLevelXp = threshold;
        xpToNextLevel = remainingXp;
    }

    static void recordRngesusMeter(float percent) {
        if (Float.isFinite(percent)) rngesusMeter = percent;
    }

    static void updateContext(Gamemode mode, String heldItemId) {
        currentGamemode = mode;
        lastHandItemId = heldItemId;
    }

    static void applySidebar(DungeonFloor floor, int cleared, int miningHeat, Season currentSeason,
                             int hour, int minute, boolean morning) {
        currentFloor = floor;
        clearedPercentage = cleared;
        heat = miningHeat;
        season = currentSeason;
        sbHour = hour;
        sbMinute = minute;
        am = morning;
    }

    static void applyTabList(SkyblockTabList tabList) {
        currentLocation = tabList.location();
        currentArea = tabList.area();
        sandbox = tabList.sandbox();
        if (tabList.mayor() != null) currentMayor = tabList.mayor();
        mithrilPowder = tabList.mithril();
        gemstonePowder = tabList.gemstone();
        commissions = tabList.commissions();

        if (currentLocation == Location.GARDEN) {
            activePests = tabList.pests();
            activeVisitors = tabList.visitors();
            cropMilestone = tabList.milestones();
        } else {
            clearGardenData();
        }
    }

    static void clearWorldData() {
        updateSlayerActivity(false, false);
        updateContext(Gamemode.LOBBY, "");
        applySidebar(DungeonFloor.NONE, -1, 0, Season.SPRING, 0, 0, false);
        currentLocation = Location.NONE;
        currentArea = Area.NONE;
        currentMayor = Mayor.NONE;
        sandbox = false;
        mithrilPowder = 0;
        gemstonePowder = 0;
        commissions = List.of();
        clearGardenData();
    }

    private static void clearSlayerProfile() {
        currentSlayer = Slayer.NONE;
        resetSlayerData();
        updateSlayerActivity(false, false);
    }

    private static void clearGardenData() {
        activePests = new HashMap<>();
        activeVisitors = new ArrayList<>();
        cropMilestone = new ArrayList<>();
    }
}
