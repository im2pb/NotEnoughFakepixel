package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.position.Position;
import io.github.notenoughupdates.moulconfig.annotations.*;
import org.lwjgl.input.Keyboard;

public class Mining {

    /* General Mining Settings */
    @Expose
    @ConfigOption(name = "Enable Mining Ability Notifier", desc = "Notify when mining ability is ready.")
    @ConfigEditorBoolean
    public boolean miningAbilityNotifier = true;

    @Expose
    @ConfigOption(name = "Fix Drill Animation Reset", desc = "Fix drill animation resetting on fuel update.")
    @ConfigEditorBoolean
    public boolean miningDrillFix = true;

    @Expose
    @ConfigOption(name = "Hide Overlay on Chat open", desc = "Hides overlays on when chat is opened.")
    @ConfigEditorBoolean
    public boolean miningOverlayHideOnChat = true;

    @Expose
    @ConfigOption(name = "Mining Overlay", desc = "Enable the mining overlay in Dwarven Mines & Crystal Hollows.")
    @ConfigEditorBoolean
    public boolean miningOverlay = true;

    @Expose
    @Category(name = "Dwarven Mines", desc = "Settings for Dwarven Mines.")
    public DwarvenMines dwarvenMines = new DwarvenMines();

    @Expose
    @Category(name = "Mining Overlay Settings", desc = "Settings for the mining overlay.")
    public MiningOverlaySettings miningOverlaySettings = new MiningOverlaySettings();

    @Expose
    @Category(name = "Crystal Hollows", desc = "Settings for Crystal Hollows.")
    public CrystalHollows crystalHollows = new CrystalHollows();

    public static class DwarvenMines {

            @Expose
            @ConfigOption(name = "Puzzler Solver", desc = "Enable Puzzler block solver.")
            @ConfigEditorBoolean
            public boolean miningPuzzlerSolver = true;

            @Expose
            @ConfigOption(name = "Remove Ghosts Invisibility", desc = "Remove invisibility from ghosts.")
            @ConfigEditorBoolean
            public boolean miningShowGhosts = true;

            @Expose
            @ConfigOption(name = "Disable Don Espresso Messages", desc = "Disable Don Espresso event messages.")
            @ConfigEditorBoolean
            public boolean miningDisableDonEspresso = true;
    }

    public static class MiningOverlaySettings {

            @Expose
            @ConfigOption(name = "Mining Overlay Offset X", desc = "Horizontal offset of the mining overlay.")
            @ConfigEditorSlider(minValue = 0.0f, maxValue = 1800.0f, minStep = 1.0f)
            public float miningOverlayOffsetX = 10.0f;

            @Expose
            @ConfigOption(name = "Mining Overlay Offset Y", desc = "Vertical offset of the mining overlay.")
            @ConfigEditorSlider(minValue = 0.0f, maxValue = 1250.0f, minStep = 1.0f)
            public float miningOverlayOffsetY = 10.0f;

            @Expose
            @ConfigOption(name = "Mining Overlay Scale", desc = "Scale of the mining overlay text.")
            @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
            public float miningOverlayScale = 1.0f;

            @Expose
            @ConfigOption(name = "Mining Overlay Background Color", desc = "Background color of the mining overlay.")
            @ConfigEditorColour
            public String miningOverlayBackgroundColor = "0:150:0:0:0";

            @Expose
            @ConfigOption(name = "Show Ability Cooldown", desc = "Show the mining ability cooldown in the overlay.")
            @ConfigEditorBoolean
            public boolean miningAbilityCooldown = true;

            @Expose
            @ConfigOption(name = "Show Mithril Powder", desc = "Show mithril powder in the overlay.")
            @ConfigEditorBoolean
            public boolean miningMithrilPowder = true;

            @Expose
            @ConfigOption(name = "Show Gemstone Powder", desc = "Show gemstone powder in the overlay.")
            @ConfigEditorBoolean
            public boolean miningGemstonePowder = true;

            @Expose
            @ConfigOption(name = "Show Drill Fuel", desc = "Show drill fuel in the overlay.")
            @ConfigEditorBoolean
            public boolean miningDrillFuel = true;

            @Expose
            @ConfigOption(name = "Show Commissions", desc = "Show commissions in the overlay.")
            @ConfigEditorBoolean
            public boolean miningCommissions = true;
    }

    public static class CrystalHollows {

            @Expose
            @ConfigOption(name = "Show Scavenged owned", desc = "Shows the scavenged owned in your inventory at Mines of Divan")
            @ConfigEditorBoolean
            public boolean scavengedOverlay = true;

            @Expose
            @ConfigOption(name = "Crystal Hollows Map", desc = "Crystal Hollows map settings.")
            @ConfigEditorBoolean
            public boolean miningCrystalMap = true;

            @Expose
            @ConfigOption(name = "Metal Detector Waypoint", desc = "Triangulates the possible position for the treasure in Mines of Divan.")
            @ConfigEditorBoolean
            public boolean crystalMetalDetector = true;

            @Expose
            @ConfigOption(name = "Full block Crystal Panes", desc = "Modifies the crystal pane hitbox to be a full block instead of a thin pane.")
            @ConfigEditorBoolean
            public boolean crystalFullBlockPane = true;

            @Expose
            @ConfigOption(name = "Enables CH Waypoints", desc = "Enables render of saved Crystal Hollows waypoints.")
            @ConfigEditorBoolean
            public boolean crystalWaypoints = true;

            @Expose
            @ConfigOption(name = "Automaton Display", desc = "Shows Automatons close to you.")
            @ConfigEditorBoolean
            public boolean crystalShowAutomaton = true;

            @Expose
            @ConfigOption(name = "Powder Mining Notifier", desc = "Notifies in your screen when you found a powder chest.")
            @ConfigEditorBoolean
            public boolean crystalPowderNotifier = true;

            @Expose
            @ConfigOption(name = "Worm Notifier", desc = "Notifies in your screen when a worm spawned.")
            @ConfigEditorBoolean
            public boolean crystalWormNotifier = false;

            @Expose
            @ConfigOption(name = "Treasure Chest Helper", desc = "Shows a square where to aim when a treasure chest spawns.")
            @ConfigEditorBoolean
            public boolean lockedTreasureChest = true;

            @Expose
            @ConfigOption(name = "Worm Timer", desc = "Shows a timer for the worm cooldown.")
            @ConfigEditorBoolean
            public boolean wormTimerCooldown = true;

            @Expose
            @ConfigOption(name = "Heat Notifier", desc = "Notifies you when you are over given heat.")
            @ConfigEditorBoolean
            public boolean crystalHeatNotifier = true;

            @Expose
            @ConfigOption(name = "Heat Notifier Level", desc = "Set the heat level to get notified.")
            @ConfigEditorSlider(minValue = 50.0f, maxValue = 100.0f, minStep = 1.0f)
            public int crystalHeatLevel = 80;

            @Expose
            @ConfigOption(name = "Chest Tracker", desc = "Highlights nearby chests and draws a trail to them.")
            @ConfigEditorBoolean
            public boolean chestTracker = true;

            @Expose
            @ConfigOption(name = "Chest Tracker Clear Key", desc = "Keybind to clear all tracked chests.")
            @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
            public int chestTrackerClearKey = Keyboard.KEY_NONE;

    }

    @Expose
    @Category(name = "Crystal Hollows Map Settings", desc = "Settings for the Crystal Hollows map.")
    public CrystalHollowsMap crystalHollowsMap = new CrystalHollowsMap();

    @Expose
    @Category(name = "Scavenged Overlay Settings", desc = "Settings for Scavenged Overlay.")
    public ScavengedOverlaySettings scavengedOverlaySettings = new ScavengedOverlaySettings();

    @Expose
    @Category(name = "Metal Detector Settings", desc = "Settings for the metal detector triangulator.")
    public MetalDetector metalDetector = new MetalDetector();

    @Expose
    @Category(name = "Waypoint Settings", desc = "Settings for the custom waypoints.")
    public WaypointSettings waypointSettings = new WaypointSettings();

    @Expose
    @Category(name = "Automaton Overlay", desc = "Settings for Automaton Overlay.")
    public AutomatonOverlaySettings automatonOverlaySettings = new AutomatonOverlaySettings();

    @Expose
    @Category(name = "Worm Timer", desc = "Settings for Worm Timer.")
    public WormTimerSettings wormTimerSettings = new WormTimerSettings();

    public static class CrystalHollowsMap {

            @Expose
            @ConfigOption(name = "Open Waypoints GUI Key", desc = "Keybind to open Waypoints GUI")
            @ConfigEditorKeybind(defaultKey = Keyboard.KEY_M)
            public int chWaypointsGUI = Keyboard.KEY_M;

            @Expose
            @ConfigOption(name = "Create new Waypoint Key", desc = "Keybind to open GUI to create a new Waypoint")
            @ConfigEditorKeybind(defaultKey = Keyboard.KEY_B)
            public int chNewWaypointGUI = Keyboard.KEY_B;

            @Expose
            @ConfigOption(name = "Crystal Hollows Map Type", desc = "Choose the type of Crystal Hollows map.")
            @ConfigEditorDropdown(
                    values = {"Gemstones", "Zones"}
            )
            public String miningCrystalMapType = "Gemstones";

            @Expose
            @ConfigOption(name = "Edit Crystal Hollows Map Pos", desc = "Adjust the CH map position visually")
            @ConfigEditorButton(runnableId = 20, buttonText = "Edit")
            public String editCrystalHollowsMapPosition = "";

            @Expose
            @ConfigOption(name = "Crystal Hollows Map Width", desc = "Width of the Crystal Hollows map in pixels.")
            @ConfigEditorSlider(minValue = 32.0f, maxValue = 160.0f, minStep = 1.0f)
            public int miningCrystalMapWidth = 64;

            @Expose
            public Position crystalMapPos = new Position(10, 10, false, false);
    }

    public static class ScavengedOverlaySettings {

            @Expose
            @ConfigOption(name = "Edit Scavenged Overlay Pos", desc = "Adjust the Scavenged overlay position visually")
            @ConfigEditorButton(runnableId = 21, buttonText = "Edit")
            public String editScavengedPosition = "";

            @Expose
            @ConfigOption(name = "Scavenged Overlay Scale", desc = "Scale of the Scavenged Overlay text.")
            @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
            public float scavengedOverlayScale = 1.0f;

            @Expose
            public Position scavengedOverlayPos = new Position(0, 0, true, true);
    }

    public static class MetalDetector {

            @Expose
            @ConfigOption(name = "Treasure Waypoint color", desc = "Color of the waypoint approximation.")
            @ConfigEditorColour
            public String crystalDivanWaypointColor = "0:190:0:255:0";
    }

    public static class WaypointSettings {

            @Expose
            @ConfigOption(name = "Waypoint color", desc = "Default waypoint color.")
            @ConfigEditorColour
            public String crystalWaypointColor = "0:255:0:0:0";

            @Expose
            @ConfigOption(name = "Show Beacons", desc = "Enables beacons in the waypoints.")
            @ConfigEditorBoolean
            public boolean crystalWaypointsBeacons = false;

            @Expose
            @ConfigOption(name = "Show Names", desc = "Enables names in the waypoints.")
            @ConfigEditorBoolean
            public boolean crystalWaypointsNames = true;
    }

    public static class AutomatonOverlaySettings {

            @Expose
            @ConfigOption(name = "Edit Automaton Overlay Pos", desc = "Adjust the Automaton overlay position visually")
            @ConfigEditorButton(runnableId = 22, buttonText = "Edit")
            public String editAutomatonPosition = "";

            @Expose
            @ConfigOption(name = "Automaton Overlay Scale", desc = "Scale of the Automaton Overlay text.")
            @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
            public float automatonOverlayScale = 1.0f;

            @Expose
            @ConfigOption(name = "Automaton Box Color", desc = "Color of the Automaton box.")
            @ConfigEditorColour
            public String automatonColor = "0:190:0:255:0";

            @Expose
            public Position automatonOverlayPos = new Position(0, 0, true, true);
    }

    public static class WormTimerSettings {

            @Expose
            @ConfigOption(name = "Scale", desc = "Scale of the Worm Timer.")
            @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
            public float wormTimerScale = 1.0f;

            @Expose
            @ConfigOption(name = "Position", desc = "Position of the Worm Timer.")
            @ConfigEditorButton(runnableId = 23, buttonText = "Edit")
            public String editWormTimerPos = "";

            @Expose
            public Position wormTimerPos = new Position(0, 0, true, true);
    }
}
