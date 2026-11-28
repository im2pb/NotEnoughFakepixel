package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonMap;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Dungeons {

    @Expose
    @Category(name = "General Settings", desc = "General dungeon settings")
    public General general = new General();

    @Expose
    @Category(name = "Map Settings", desc = "Settings for the dungeon map")
    public MapSettings map = new MapSettings();

    @Expose
    @Category(name = "Puzzle Solvers", desc = "Settings for puzzle solvers")
    public Puzzles puzzles = new Puzzles();

    @Expose
    @Category(name = "Mob Settings", desc = "Settings for mob displays")
    public Mobs mobs = new Mobs();

    @Expose
    @Category(name = "Terminal Settings", desc = "Settings for terminal GUIs and solvers")
    public Terminals terminals = new Terminals();

    @Expose
    @Category(name = "Score and Secrets", desc = "Settings for score notifications and secrets")
    public ScoreAndSecrets scoreSecrets = new ScoreAndSecrets();

    @Expose
    @Category(name = "Master Mode 7", desc = "Features related to M7")
    public MasterMode7 masterMode7 = new MasterMode7();

    @Expose
    @ConfigOption(name = "Highlight WitherDoors", desc = "Box wither doors")
    @ConfigEditorBoolean
    public boolean dungeonsWitherDoors = true;

    @Expose
    @ConfigOption(name = "Active Witherdoor color", desc = "Active Witherdoor color")
    @ConfigEditorColour
    public String dungeonsWitherDoorsActive = "0:255:0:255:0";

    @Expose
    @ConfigOption(name = "Inactive Witherdoor color", desc = "Inactive Witherdoor color")
    @ConfigEditorColour
    public String dungeonsWitherDoorsInactive = "0:255:255:0:0";

    @Expose
    @ConfigOption(name = "Floor 7 Withers box", desc = "Box withers in f7")
    @ConfigEditorBoolean
    public boolean dungeonsWithersBox = true;

    @Expose
    @ConfigOption(name = "Floor 7 Withers box color", desc = "Floor 7 Withers box color")
    @ConfigEditorColour
    public String dungeonsWithersBoxColor = "0:255:0:255:0";

    public static class General {
        @Expose
        @ConfigOption(name = "Is Paul Active", desc = "Check if Paul is active as mayor with EZPZ perk")
        @ConfigEditorBoolean
        public boolean dungeonsIsPaul = false;

        @Expose
        @ConfigOption(name = "Auto Close Chests", desc = "Automatically close Secret Chest menus in dungeons")
        @ConfigEditorBoolean
        public boolean dungeonsAutoCloseChests = true;

        @Expose
        @ConfigOption(name = "Wither & Blood Keys Tracers", desc = "Show tracers on wither and blood keys")
        @ConfigEditorBoolean
        public boolean dungeonsKeyTracers = true;

        @Expose
        @ConfigOption(name = "Mute Irrelevant Messages", desc = "Mute bosses and crowd dialogs in chat")
        @ConfigEditorBoolean
        public boolean dungeonsMuteIrrelevantMessages = true;

        @Expose
        @ConfigOption(name = "Salvage Items Saver", desc = "Prevent salvaging important and legendary+ items")
        @ConfigEditorBoolean
        public boolean dungeonsSalvageItemsPrevention = true;

        @Expose
        @ConfigOption(name = "Show Correct Livid", desc = "Show the correct Livid")
        @ConfigEditorBoolean
        public boolean dungeonsLividFinder = true;

        @Expose
        @ConfigOption(name = "Livid Display", desc = "Display style for Livid")
        @ConfigEditorDropdown(values = {"Box", "Outline"})
        public int dungeonsLividFinderRender = 0;

        @Expose
        @ConfigOption(name = "Show Opened Chests in Croesus", desc = "Show opened chests in Croesus")
        @ConfigEditorBoolean
        public boolean dungeonsShowOpenedChests = true;

        @Expose
        @ConfigOption(name = "Custom Spirit Leap GUI", desc = "Use custom GUI for Spirit Leap")
        @ConfigEditorBoolean
        public boolean dungeonsSpiritLeapGUI = true;

        @Expose
        @ConfigOption(name = "Announce Leaped to Player", desc = "Announce leaped to player in party chat.")
        @ConfigEditorBoolean
        public boolean dungeonsLeapAnnounce = true;

        @Expose
        @ConfigOption(name = "Spirit Bow Tracer", desc = "Spirit bow tracer.")
        @ConfigEditorBoolean
        public boolean dungeonsSpiritBow = true;

        @Expose
        @ConfigOption(name = "Blood Ready Title", desc = "Show a title and play a sound when the Watcher finishes spawning mobs")
        @ConfigEditorBoolean
        public boolean dungeonsBloodReady = true;
    }

    public static class MapSettings {
        @Expose
        @ConfigOption(name = "Dungeons Map", desc = "Enable dungeons map.")
        @ConfigEditorBoolean
        public boolean dungeonsMap = true;

        @Expose
        @ConfigOption(name = "Dungeons Map Border Color", desc = "Color of the dungeons map border.")
        @ConfigEditorColour
        public String dungeonsMapBorderColor = "0:255:0:0:0";

        @Expose
        @ConfigOption(name = "Dungeons Map Scale", desc = "Scale of the dungeons map.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 10.0f, minStep = 0.1f)
        public float dungeonsMapScale = 1.0f;

        @Expose
        @ConfigOption(name = "Dungeons Map Rotation", desc = "Enable rotation of the dungeons map.")
        @ConfigEditorBoolean
        public boolean dungeonsRotateMap = true;

        @ConfigOption(name = "Edit Position", desc = "Move the Dungeon Map on the HUD")
        @ConfigEditorButton(buttonText = "Edit")
        public transient Runnable editDungeonsMapPosition = () -> DungeonMap.editPosition(
                this.dungeonsMapPos, this.dungeonsMapScale, this.dungeonsMapBorderColor,
                position -> this.dungeonsMapPos = position);

        @Expose
        public Position dungeonsMapPos = new Position(10, 10, false, true);

    }

    public static class Puzzles {
        @Expose
        @ConfigOption(name = "Three Weirdos Solver", desc = "Enable Three Weirdos puzzle solver.")
        @ConfigEditorBoolean
        public boolean dungeonsThreeWeirdos = true;

        @Expose
        @ConfigOption(name = "Creeper Solver", desc = "Enable Creeper puzzle solver.")
        @ConfigEditorBoolean
        public boolean dungeonsCreeper = true;

        @Expose
        @ConfigOption(name = "Boulder Solver", desc = "Enable Boulder puzzle solver.")
        @ConfigEditorBoolean
        public boolean dungeonsBoulderSolver = true;

        @Expose
        @ConfigOption(name = "Silverfish Solver", desc = "Enable Silverfish puzzle solver.")
        @ConfigEditorBoolean
        public boolean dungeonsSilverfishSolver = true;

        @Expose
        @ConfigOption(name = "Water Solver", desc = "Enable Water puzzle solver.")
        @ConfigEditorBoolean
        public boolean dungeonsWaterSolver = true;

        @Expose
        @ConfigOption(name = "Teleport Maze Solver", desc = "Enable Teleport Maze puzzle solver.")
        @ConfigEditorBoolean
        public boolean dungeonsTeleportMaze = true;
    }

    public static class Mobs {
        @Expose
        @ConfigOption(name = "Fel Mobs Display", desc = "Display Fel mobs.")
        @ConfigEditorBoolean
        public boolean dungeonsFelMob = true;

        @Expose
        @ConfigOption(name = "Fel Mob Color", desc = "Color of Fel mobs.")
        @ConfigEditorColour
        public String dungeonsFelColor = "0:92:154:255:255";

        @Expose
        @ConfigOption(name = "Bat Mobs Display", desc = "Display Bat mobs.")
        @ConfigEditorBoolean
        public boolean dungeonsBatMobs = true;

        @Expose
        @ConfigOption(name = "Bat Mob Color", desc = "Color of Bat mobs.")
        @ConfigEditorColour
        public String dungeonsBatColor = "0:92:0:255:0";

        @Expose
        @ConfigOption(name = "Starred Mobs Display", desc = "Display style for starred mobs.")
        @ConfigEditorDropdown(values = {"Box", "Outline", "Disabled"})
        public int dungeonsStarredMobs = 0;

        @Expose
        @ConfigOption(name = "Starred Mobs Color", desc = "Color of starred mobs.")
        @ConfigEditorColour
        public String dungeonsStarredBoxColor = "0:92:154:255:255";

        @Expose
        @ConfigOption(name = "Starred Mobs ESP", desc = "Render starred mobs hitboxes through walls.")
        @ConfigEditorBoolean
        public boolean dungeonsStarredMobsEsp = true;

        @Expose
        @ConfigOption(name = "Withermancer Color", desc = "Color of Withermancer.")
        @ConfigEditorColour
        public String dungeonsWithermancerColor = "0:169:169:169:255";

        @Expose
        @ConfigOption(name = "Zombie Commander Color", desc = "Color of Zombie Commander.")
        @ConfigEditorColour
        public String dungeonsZombieCommanderColor = "0:255:0:0:255";

        @Expose
        @ConfigOption(name = "Skeleton Master Color", desc = "Color of Skeleton Master.")
        @ConfigEditorColour
        public String dungeonsSkeletonMasterColor = "0:255:100:0:255";

        @Expose
        @ConfigOption(name = "Stormy Color", desc = "Color of Stormy.")
        @ConfigEditorColour
        public String dungeonsStormyColor = "0:173:216:230:255";
    }

    public static class Terminals {
        @Expose
        @ConfigOption(name = "Terminal tracker", desc = "Shows how many terminals done on the current phase of goldor.")
        @ConfigEditorBoolean
        public boolean dungeonsTerminalTracker = true;

        @Expose
        @ConfigOption(name = "Terminal tracker Scale", desc = "Scale of the terminal tracker text.")
        @ConfigEditorSlider(minValue = 1.0f, maxValue = 5.0f, minStep = 0.1f)
        public float dungeonsTerminalTrackerScale = 1.0f;

        @Expose
        @ConfigOption(name = "Terminal tracker Color", desc = "Color of Stormy.")
        @ConfigEditorColour
        public String dungeonsTerminalTrackerColor = "0:173:216:230:255";

        @Expose
        public Position terminalTrackerPos = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "Custom Click in Order Terminal GUI", desc = "Use custom GUI for Click in Order terminal.")
        @ConfigEditorBoolean
        public boolean dungeonsCustomGuiClickIn = true;

        @Expose
        @ConfigOption(name = "Custom Colors Terminal GUI", desc = "Use custom GUI for Colors terminal.")
        @ConfigEditorBoolean
        public boolean dungeonsCustomGuiColors = true;

        @Expose
        @ConfigOption(name = "Custom Maze Terminal GUI", desc = "Use custom GUI for Maze terminal.")
        @ConfigEditorBoolean
        public boolean dungeonsCustomGuiMaze = true;

        @Expose
        @ConfigOption(name = "Custom Starts With Terminal GUI", desc = "Use custom GUI for Starts With terminal.")
        @ConfigEditorBoolean
        public boolean dungeonsCustomGuiStartsWith = true;

        @Expose
        @ConfigOption(name = "Custom Panes Terminal GUI", desc = "Use custom GUI for Panes terminal.")
        @ConfigEditorBoolean
        public boolean dungeonsCustomGuiPanes = true;

        @Expose
        @ConfigOption(name = "Custom Terminal Scale", desc = "Scale of custom terminal GUIs.")
        @ConfigEditorSlider(minValue = 1.0f, maxValue = 5.0f, minStep = 0.1f)
        public float dungeonsTerminalsScale = 3.0f;

        @Expose
        @ConfigOption(name = "Terminal Starts With Solver", desc = "Enable Starts With terminal solver.")
        @ConfigEditorBoolean
        public boolean dungeonsTerminalStartsWithSolver = true;

        @Expose
        @ConfigOption(name = "Terminal Select Colors Solver", desc = "Enable Select Colors terminal solver.")
        @ConfigEditorBoolean
        public boolean dungeonsTerminalSelectColorsSolver = true;

        @Expose
        @ConfigOption(name = "Terminal Click In Order Solver", desc = "Enable Click In Order terminal solver.")
        @ConfigEditorBoolean
        public boolean dungeonsTerminalClickInOrderSolver = true;

        @Expose
        @ConfigOption(name = "Terminal Maze Solver", desc = "Enable Maze terminal solver.")
        @ConfigEditorBoolean
        public boolean dungeonsTerminalMazeSolver = true;

        @Expose
        @ConfigOption(name = "Terminal Correct Panes Solver", desc = "Enable Correct Panes terminal solver.")
        @ConfigEditorBoolean
        public boolean dungeonsTerminalCorrectPanesSolver = true;

        @Expose
        @ConfigOption(name = "Hide Terminal Incorrect Slots", desc = "Hide incorrect slots in terminals.")
        @ConfigEditorBoolean
        public boolean dungeonsTerminalHideIncorrect = true;

        @Expose
        @ConfigOption(name = "Prevent Terminal Missclicks", desc = "Prevent missclicks in terminals.")
        @ConfigEditorBoolean
        public boolean dungeonsPreventMissclicks = true;

        @Expose
        @ConfigOption(name = "Hide Tooltips", desc = "Hide tooltips in terminals.")
        @ConfigEditorBoolean
        public boolean dungeonsHideTooltips = true;

        @Expose
        @ConfigOption(name = "First Device Solver", desc = "Enable first device solver.")
        @ConfigEditorBoolean
        public boolean dungeonsFirstDeviceSolver = true;

        @Expose
        @ConfigOption(name = "Third Device Solver", desc = "Enable third device solver.")
        @ConfigEditorBoolean
        public boolean dungeonsThirdDeviceSolver = true;

        @Expose
        @ConfigOption(name = "Correct Color", desc = "Color for correct choices.")
        @ConfigEditorColour
        public String dungeonsCorrectColor = "0:255:0:255:0";

        @Expose
        @ConfigOption(name = "Alternative Color", desc = "Alternative color for choices.")
        @ConfigEditorColour
        public String dungeonsAlternativeColor = "0:255:255:255:0";

        @Expose
        @ConfigOption(name = "Terminal Waypoints", desc = "Show waypoints for terminals.")
        @ConfigEditorBoolean
        public boolean dungeonsTerminalWaypoints = true;
    }

    public static class ScoreAndSecrets {
        @Expose
        @ConfigOption(name = "S+ Notifier", desc = "Notify when S+ is virtually reached.")
        @ConfigEditorBoolean
        public boolean dungeonsSPlusNotifier = true;

        @Expose
        @ConfigOption(name = "Score Overlay", desc = "Enable the dungeon score overlay.")
        @ConfigEditorBoolean
        public boolean dungeonsScoreOverlay = true;

        @Expose
        @ConfigOption(name = "Simple Score Overlay", desc = "Enable the dungeon simple score overlay.")
        @ConfigEditorBoolean
        public boolean dungeonsScoreSimple = true;

        @Expose
        @ConfigOption(name = "Score Overlay Scale", desc = "Scale of the score overlay text.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
        public float scoreOverlayScale = 1.0f;

        @Expose
        @ConfigOption(name = "Score Overlay Background Color", desc = "Background color of the score overlay.")
        @ConfigEditorColour
        public String scoreOverlayBackgroundColor = "0:150:0:0:0";

        @Expose
        public Position scoreOverlayPos = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "S Notifier", desc = "Notify when S is reached (actually reached not virtual).")
        @ConfigEditorBoolean
        public boolean dungeonsSNotifier = true;

        @Expose
        @ConfigOption(name = "S+ Message on Chat", desc = "Send a message when dungeon is about to be done.")
        @ConfigEditorBoolean
        public boolean dungeonsSPlusMessage = true;

        @Expose
        @ConfigOption(name = "Custom S+ Message", desc = "Custom message for S+ notification.")
        @ConfigEditorText
        public String dungeonsSPlusCustom = "";

        @Expose
        @ConfigOption(name = "Show Item Secrets and Wither Essences", desc = "Show item secrets and wither essences through walls.")
        @ConfigEditorBoolean
        public boolean dungeonsItemSecretsDisplay = true;

        @Expose
        @ConfigOption(name = "Item Secrets Color", desc = "Color of item secrets.")
        @ConfigEditorColour
        public String dungeonsItemSecretsColor = "0:255:255:0:255";

        @Expose
        @ConfigOption(name = "Make Item Secrets Big", desc = "Make item secrets larger.")
        @ConfigEditorBoolean
        public boolean dungeonsItemSecretsBig = true;

        @Expose
        @ConfigOption(name = "Item Secrets Scale", desc = "Scale of item secrets.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 5.0f, minStep = 0.1f)
        public float dungeonsScaleItemDrop = 3.5f;
    }

    public static class MasterMode7 {
        @Expose
        @ConfigOption(name = "M7 Relic waypoints", desc = "Render waypoints for relics")
        @ConfigEditorBoolean
        public boolean m7Relics = true;

        @Expose
        @ConfigOption(name = "Distance Box", desc = "Render box around dragons statues.")
        @ConfigEditorBoolean
        public boolean distBox = true;

        @Expose
        @ConfigOption(name = "Dragon Outline", desc = "Outlines each dragon with their respective colors for better visibility.")
        @ConfigEditorBoolean
        public boolean dragOutline = true;
    }
}

