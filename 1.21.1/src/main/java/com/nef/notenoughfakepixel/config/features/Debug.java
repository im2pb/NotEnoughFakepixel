package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.env.VersionChecker;
import com.nef.notenoughfakepixel.features.debug.DebugModule;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Debug {

    @Expose
    @ConfigOption(name = "Debug Logs", desc = "Enable automatic debug logging")
    @ConfigEditorBoolean
    public boolean debug = false;

    @Expose
    @ConfigOption(name = "Chat Output", desc = "Show debug reports and logs in your local chat")
    @ConfigEditorBoolean
    public boolean chatLogs = true;

    @Expose
    @ConfigOption(name = "Console Output", desc = "Write debug reports and logs to the game log and run console")
    @ConfigEditorBoolean
    public boolean consoleLogs = true;

    @Expose
    @ConfigOption(name = "SkyBlock Changes", desc = "Log changes to the profile, location, area, and slayer quest")
    @ConfigEditorBoolean
    public boolean traceSkyblockChanges = false;

    @Expose
    @ConfigOption(name = "Sound Debug", desc = "Log played sound IDs, categories, volumes, and pitches")
    @ConfigEditorBoolean
    public boolean showSounds = false;

    @Expose
    @ConfigOption(name = "Force Pojav", desc = "Report Pojav as active in the client environment report")
    @ConfigEditorBoolean
    public boolean forcePojav = false;

    @Expose
    @ConfigOption(name = "Trace Outside SkyBlock", desc = "Allow automatic traces in lobbies, other worlds, and singleplayer")
    @ConfigEditorBoolean
    public boolean enableOutOfFakepixel = false;

    @Expose
    @ConfigOption(name = "Copy NBT", desc = "Hover over an inventory item and press Right Ctrl to copy its item data")
    @ConfigEditorBoolean
    public boolean debugCopyNbt = false;

    @Expose
    @Category(name = "Loggers", desc = "Inspect the currently loaded data and its inputs")
    public Loggers loggers = new Loggers();

    public static class Loggers {
        @ConfigOption(name = "Log Location", desc = "Show the stored server location, area, and sandbox state")
        @ConfigEditorButton(buttonText = "Log")
        public transient Runnable logLocation = DebugModule::logLocation;

        @ConfigOption(name = "Log Gamemode", desc = "Show the detected gamemode and whether SkyBlock is active")
        @ConfigEditorButton(buttonText = "Log")
        public transient Runnable logGamemode = DebugModule::logGamemode;

        @ConfigOption(name = "Log Scoreboard", desc = "Show the active sidebar as read by the SkyBlock parser")
        @ConfigEditorButton(buttonText = "Log")
        public transient Runnable logScoreboard = DebugModule::logScoreboard;

        @ConfigOption(name = "Log Tab List", desc = "Show the listed tab entries in the order read by the SkyBlock parser")
        @ConfigEditorButton(buttonText = "Log")
        public transient Runnable logTabList = DebugModule::logTabList;

        @ConfigOption(name = "Show Held Item SkyBlock ID", desc = "Read the server ID from your main-hand item's custom data")
        @ConfigEditorButton(buttonText = "Show")
        public transient Runnable logHeldItem = DebugModule::logHeldItem;

        @ConfigOption(name = "Log SkyBlock Data", desc = "Show the loaded profile, time, slayer, mining, dungeon, and Garden data")
        @ConfigEditorButton(buttonText = "Log")
        public transient Runnable logSkyblockData = DebugModule::logSkyblockData;

        @ConfigOption(name = "Copy SkyBlock Data", desc = "Copy the loaded SkyBlock data report to the clipboard")
        @ConfigEditorButton(buttonText = "Copy")
        public transient Runnable copySkyblockData = DebugModule::copySkyblockData;

        @ConfigOption(name = "Log Client Environment", desc = "Show the connection type, Java version, operating system, and Pojav status")
        @ConfigEditorButton(buttonText = "Log")
        public transient Runnable logClientEnvironment = DebugModule::logClientEnvironment;

        @ConfigOption(name = "Fabric Version", desc = "Show the installed version and repository update information in chat")
        @ConfigEditorButton(buttonText = "Check")
        public transient Runnable version = VersionChecker::reportVersion;
    }
}
