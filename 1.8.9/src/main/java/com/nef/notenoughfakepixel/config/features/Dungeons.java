package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.position.Position;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder;

public class Dungeons {

    @Expose
    @Category(name = "General Settings", desc = "General dungeon settings.")
    @ConfigOrder(0)
    public General general = new General();

    @Expose
    @Category(name = "Map Settings", desc = "Settings for the dungeon map.")
    @ConfigOrder(1)
    public MapSettings map = new MapSettings();

    @Expose
    @Category(name = "Puzzle Solvers", desc = "Settings for puzzle solvers.")
    @ConfigOrder(2)
    public Puzzles puzzles = new Puzzles();

    @Expose
    @Category(name = "Mob Settings", desc = "Settings for mob displays.")
    @ConfigOrder(3)
    public Mobs mobs = new Mobs();

    @Expose
    @Category(name = "Terminal Settings", desc = "Settings for terminal GUIs and solvers.")
    @ConfigOrder(4)
    public Terminals terminals = new Terminals();

    @Expose
    @Category(name = "Score and Secrets", desc = "Settings for score notifications and secrets.")
    @ConfigOrder(5)
    public ScoreAndSecrets scoreSecrets = new ScoreAndSecrets();

    @Expose
    @Category(name = "Master Mode 7", desc = "Features related to M7.")
    @ConfigOrder(6)
    public MasterMode7 masterMode7 = new MasterMode7();

    @Expose
    @ConfigOption(name = "Highlight WitherDoors", desc = "Box wither doors.")
    @ConfigEditorBoolean
    @ConfigOrder(7)
    public boolean dungeonsWitherDoors = true;

    @Expose
    @ConfigOption(name = "Active Witherdoor color", desc = "Active Witherdoor color.")
    @ConfigEditorColour
    @ConfigOrder(8)
    public String dungeonsWitherDoorsActive = "0:255:0:255:0";

    @Expose
    @ConfigOption(name = "Inactive Witherdoor color", desc = "Inactive Witherdoor color.")
    @ConfigEditorColour
    @ConfigOrder(9)
    public String dungeonsWitherDoorsInactive = "0:255:255:0:0";

    @Expose
    @ConfigOption(name = "Floor 7 Withers box", desc = "Box withers in f7.")
    @ConfigEditorBoolean
    @ConfigOrder(10)
    public boolean dungeonsWithersBox = true;

    @Expose
    @ConfigOption(name = "Floor 7 Withers box color", desc = "Floor 7 Withers box color.")
    @ConfigEditorColour
    @ConfigOrder(11)
    public String dungeonsWithersBoxColor = "0:255:0:255:0";

    public static class General {
        @Expose
        @ConfigOption(name = "Is Paul Active", desc = "Check if Paul is active as mayor with EZPZ perk.")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean dungeonsIsPaul = false;

        @Expose
        @ConfigOption(name = "Auto Close Chests", desc = "Automatically close chests in dungeons.")
        @ConfigEditorBoolean
        @ConfigOrder(1)
        public boolean dungeonsAutoCloseChests = true;

        @Expose
        @ConfigOption(name = "Wither & Blood Keys Tracers", desc = "Show tracers on wither and blood keys.")
        @ConfigEditorBoolean
        @ConfigOrder(2)
        public boolean dungeonsKeyTracers = true;

        @Expose
        @ConfigOption(name = "Mute Irrelevant Messages", desc = "Mute bosses and crowd dialogs in chat.")
        @ConfigEditorBoolean
        @ConfigOrder(3)
        public boolean dungeonsMuteIrrelevantMessages = true;

        @Expose
        @ConfigOption(name = "Salvage Items Saver", desc = "Prevent salvaging important and legendary+ items.")
        @ConfigEditorBoolean
        @ConfigOrder(4)
        public boolean dungeonsSalvageItemsPrevention = true;

        @Expose
        @ConfigOption(name = "Show Correct Livid", desc = "Show the correct Livid.")
        @ConfigEditorBoolean
        @ConfigOrder(5)
        public boolean dungeonsLividFinder = true;

        @Expose
        @ConfigOption(name = "Livid Display", desc = "Display style for Livid.")
        @ConfigEditorDropdown(values = {"Box", "Outline"})
        @ConfigOrder(6)
        public int dungeonsLividFinderRender = 0;

        @Expose
        @ConfigOption(name = "Show Opened Chests in Croesus", desc = "Show opened chests in Croesus.")
        @ConfigEditorBoolean
        @ConfigOrder(7)
        public boolean dungeonsShowOpenedChests = true;

        @Expose
        @ConfigOption(name = "Custom Spirit Leap GUI", desc = "Use custom GUI for Spirit Leap.")
        @ConfigEditorBoolean
        @ConfigOrder(8)
        public boolean dungeonsSpiritLeapGUI = true;

        @Expose
        @ConfigOption(name = "Announce Leaped to Player", desc = "Announce leaped to player in party chat.")
        @ConfigEditorBoolean
        @ConfigOrder(9)
        public boolean dungeonsLeapAnnounce = true;

        @Expose
        @ConfigOption(name = "Spirit Bow Tracer", desc = "Spirit bow tracer.")
        @ConfigEditorBoolean
        @ConfigOrder(10)
        public boolean dungeonsSpiritBow = true;

        @Expose
        @ConfigOption(name = "Announce Blood Room Done", desc = "Announce when blood room is done spawning.")
        @ConfigEditorBoolean
        @ConfigOrder(11)
        public boolean dungeonsBloodReady = true;
    }

    public static class MapSettings {
        @Expose
        @ConfigOption(name = "Dungeons Map", desc = "Enable dungeons map.")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean dungeonsMap = true;

        @Expose
        @ConfigOption(name = "Dungeons Map Border Color", desc = "Color of the dungeons map border.")
        @ConfigEditorColour
        @ConfigOrder(1)
        public String dungeonsMapBorderColor = "0:255:0:0:0";

        @Expose
        @ConfigOption(name = "Dungeons Map Scale", desc = "Scale of the dungeons map.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 10.0f, minStep = 0.1f)
        @ConfigOrder(2)
        public float dungeonsMapScale = 1.0f;

        @Expose
        @ConfigOption(name = "Dungeons Map Rotation", desc = "Enable rotation of the dungeons map.")
        @ConfigEditorBoolean
        @ConfigOrder(3)
        public boolean dungeonsRotateMap = true;

        @Expose
        public Position dungeonsMapPos = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "Edit Map Position", desc = "Adjust the dungeons map position visually.")
        @ConfigEditorButton(runnableId = 1, buttonText = "Edit Position")
        @ConfigOrder(4)
        public String editDungeonsMapPositionButton = "";
    }

    public static class Puzzles {
        @Expose
        @ConfigOption(name = "Three Weirdos Solver", desc = "Enable Three Weirdos puzzle solver.")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean dungeonsThreeWeirdos = true;

        @Expose
        @ConfigOption(name = "Creeper Solver", desc = "Enable Creeper puzzle solver.")
        @ConfigEditorBoolean
        @ConfigOrder(1)
        public boolean dungeonsCreeper = true;

        @Expose
        @ConfigOption(name = "Boulder Solver", desc = "Enable Boulder puzzle solver.")
        @ConfigEditorBoolean
        @ConfigOrder(2)
        public boolean dungeonsBoulderSolver = true;

        @Expose
        @ConfigOption(name = "Silverfish Solver", desc = "Enable Silverfish puzzle solver.")
        @ConfigEditorBoolean
        @ConfigOrder(3)
        public boolean dungeonsSilverfishSolver = true;

        @Expose
        @ConfigOption(name = "Water Solver", desc = "Enable Water puzzle solver.")
        @ConfigEditorBoolean
        @ConfigOrder(4)
        public boolean dungeonsWaterSolver = true;

        @Expose
        @ConfigOption(name = "Teleport Maze Solver", desc = "Enable Teleport Maze puzzle solver.")
        @ConfigEditorBoolean
        @ConfigOrder(5)
        public boolean dungeonsTeleportMaze = true;
    }

    public static class Mobs {
        @Expose
        @ConfigOption(name = "Fel Mobs Display", desc = "Display Fel mobs.")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean dungeonsFelMob = true;

        @Expose
        @ConfigOption(name = "Fel Mob Color", desc = "Color of Fel mobs.")
        @ConfigEditorColour
        @ConfigOrder(1)
        public String dungeonsFelColor = "0:92:154:255:255";

        @Expose
        @ConfigOption(name = "Bat Mobs Display", desc = "Display Bat mobs.")
        @ConfigEditorBoolean
        @ConfigOrder(2)
        public boolean dungeonsBatMobs = true;

        @Expose
        @ConfigOption(name = "Bat Mob Color", desc = "Color of Bat mobs.")
        @ConfigEditorColour
        @ConfigOrder(3)
        public String dungeonsBatColor = "0:92:0:255:0";

        @Expose
        @ConfigOption(name = "Starred Mobs Display", desc = "Display style for starred mobs.")
        @ConfigEditorDropdown(values = {"Box", "Outline", "Disabled"})
        @ConfigOrder(4)
        public int dungeonsStarredMobs = 0;

        @Expose
        @ConfigOption(name = "Starred Mobs Color", desc = "Color of starred mobs.")
        @ConfigEditorColour
        @ConfigOrder(5)
        public String dungeonsStarredBoxColor = "0:92:154:255:255";

        @Expose
        @ConfigOption(name = "Starred Mobs ESP", desc = "Render starred mobs hitboxes through walls.")
        @ConfigEditorBoolean
        @ConfigOrder(6)
        public boolean dungeonsStarredMobsEsp = true;

        @Expose
        @ConfigOption(name = "Withermancer Color", desc = "Color of Withermancer.")
        @ConfigEditorColour
        @ConfigOrder(7)
        public String dungeonsWithermancerColor = "0:169:169:169:255";

        @Expose
        @ConfigOption(name = "Zombie Commander Color", desc = "Color of Zombie Commander.")
        @ConfigEditorColour
        @ConfigOrder(8)
        public String dungeonsZombieCommanderColor = "0:255:0:0:255";

        @Expose
        @ConfigOption(name = "Skeleton Master Color", desc = "Color of Skeleton Master.")
        @ConfigEditorColour
        @ConfigOrder(9)
        public String dungeonsSkeletonMasterColor = "0:255:100:0:255";

        @Expose
        @ConfigOption(name = "Stormy Color", desc = "Color of Stormy.")
        @ConfigEditorColour
        @ConfigOrder(10)
        public String dungeonsStormyColor = "0:173:216:230:255";
    }

    public static class Terminals {
        @Expose
        @ConfigOption(name = "Terminal tracker", desc = "Shows how many terminals done on the current phase of goldor.")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean dungeonsTerminalTracker = true;

        @Expose
        @ConfigOption(name = "Terminal tracker Scale", desc = "Scale of the terminal tracker text.")
        @ConfigEditorSlider(minValue = 1.0f, maxValue = 5.0f, minStep = 0.1f)
        @ConfigOrder(1)
        public float dungeonsTerminalTrackerScale = 1.0f;

        @Expose
        @ConfigOption(name = "Terminal tracker Color", desc = "Color of Stormy.")
        @ConfigEditorColour
        @ConfigOrder(2)
        public String dungeonsTerminalTrackerColor = "0:173:216:230:255";

        @Expose
        public Position terminalTrackerPos = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "Edit Terminal tracker Position", desc = "Adjust the Terminal tracker position visually")
        @ConfigEditorButton(runnableId = 3, buttonText = "Edit Position")
        @ConfigOrder(3)
        public String editTerminalTrackerPositionButton = "";

        @Expose
        @ConfigOption(name = "Custom Click in Order Terminal GUI", desc = "Use custom GUI for Click in Order terminal.")
        @ConfigEditorBoolean
        @ConfigOrder(4)
        public boolean dungeonsCustomGuiClickIn = true;

        @Expose
        @ConfigOption(name = "Custom Colors Terminal GUI", desc = "Use custom GUI for Colors terminal.")
        @ConfigEditorBoolean
        @ConfigOrder(5)
        public boolean dungeonsCustomGuiColors = true;

        @Expose
        @ConfigOption(name = "Custom Maze Terminal GUI", desc = "Use custom GUI for Maze terminal.")
        @ConfigEditorBoolean
        @ConfigOrder(6)
        public boolean dungeonsCustomGuiMaze = true;

        @Expose
        @ConfigOption(name = "Custom Starts With Terminal GUI", desc = "Use custom GUI for Starts With terminal.")
        @ConfigEditorBoolean
        @ConfigOrder(7)
        public boolean dungeonsCustomGuiStartsWith = true;

        @Expose
        @ConfigOption(name = "Custom Panes Terminal GUI", desc = "Use custom GUI for Panes terminal.")
        @ConfigEditorBoolean
        @ConfigOrder(8)
        public boolean dungeonsCustomGuiPanes = true;

        @Expose
        @ConfigOption(name = "Custom Terminal Scale", desc = "Scale of custom terminal GUIs.")
        @ConfigEditorSlider(minValue = 1.0f, maxValue = 5.0f, minStep = 0.1f)
        @ConfigOrder(9)
        public float dungeonsTerminalsScale = 3.0f;

        @Expose
        @ConfigOption(name = "Terminal Starts With Solver", desc = "Enable Starts With terminal solver.")
        @ConfigEditorBoolean
        @ConfigOrder(10)
        public boolean dungeonsTerminalStartsWithSolver = true;

        @Expose
        @ConfigOption(name = "Terminal Select Colors Solver", desc = "Enable Select Colors terminal solver.")
        @ConfigEditorBoolean
        @ConfigOrder(11)
        public boolean dungeonsTerminalSelectColorsSolver = true;

        @Expose
        @ConfigOption(name = "Terminal Click In Order Solver", desc = "Enable Click In Order terminal solver.")
        @ConfigEditorBoolean
        @ConfigOrder(12)
        public boolean dungeonsTerminalClickInOrderSolver = true;

        @Expose
        @ConfigOption(name = "Terminal Maze Solver", desc = "Enable Maze terminal solver.")
        @ConfigEditorBoolean
        @ConfigOrder(13)
        public boolean dungeonsTerminalMazeSolver = true;

        @Expose
        @ConfigOption(name = "Terminal Correct Panes Solver", desc = "Enable Correct Panes terminal solver.")
        @ConfigEditorBoolean
        @ConfigOrder(14)
        public boolean dungeonsTerminalCorrectPanesSolver = true;

        @Expose
        @ConfigOption(name = "Hide Terminal Incorrect Slots", desc = "Hide incorrect slots in terminals.")
        @ConfigEditorBoolean
        @ConfigOrder(15)
        public boolean dungeonsTerminalHideIncorrect = true;

        @Expose
        @ConfigOption(name = "Prevent Terminal Missclicks", desc = "Prevent missclicks in terminals.")
        @ConfigEditorBoolean
        @ConfigOrder(16)
        public boolean dungeonsPreventMissclicks = true;

        @Expose
        @ConfigOption(name = "Hide Tooltips", desc = "Hide tooltips in terminals.")
        @ConfigEditorBoolean
        @ConfigOrder(17)
        public boolean dungeonsHideTooltips = true;

        @Expose
        @ConfigOption(name = "First Device Solver", desc = "Enable first device solver.")
        @ConfigEditorBoolean
        @ConfigOrder(18)
        public boolean dungeonsFirstDeviceSolver = true;

        @Expose
        @ConfigOption(name = "Third Device Solver", desc = "Enable third device solver.")
        @ConfigEditorBoolean
        @ConfigOrder(19)
        public boolean dungeonsThirdDeviceSolver = true;

        @Expose
        @ConfigOption(name = "Correct Color", desc = "Color for correct choices.")
        @ConfigEditorColour
        @ConfigOrder(20)
        public String dungeonsCorrectColor = "0:255:0:255:0";

        @Expose
        @ConfigOption(name = "Alternative Color", desc = "Alternative color for choices.")
        @ConfigEditorColour
        @ConfigOrder(21)
        public String dungeonsAlternativeColor = "0:255:255:255:0";

        @Expose
        @ConfigOption(name = "Terminal Waypoints", desc = "Show waypoints for terminals.")
        @ConfigEditorBoolean
        @ConfigOrder(22)
        public boolean dungeonsTerminalWaypoints = true;
    }

    public static class ScoreAndSecrets {
        @Expose
        @ConfigOption(name = "S+ Notifier", desc = "Notify when S+ is virtually reached.")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean dungeonsSPlusNotifier = true;

        @Expose
        @ConfigOption(name = "Score Overlay", desc = "Enable the dungeon score overlay.")
        @ConfigEditorBoolean
        @ConfigOrder(1)
        public boolean dungeonsScoreOverlay = true;

        @Expose
        @ConfigOption(name = "Simple Score Overlay", desc = "Enable the dungeon simple score overlay.")
        @ConfigEditorBoolean
        @ConfigOrder(2)
        public boolean dungeonsScoreSimple = true;

        @Expose
        @ConfigOption(name = "Score Overlay Scale", desc = "Scale of the score overlay text.")
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
        @ConfigOrder(3)
        public float scoreOverlayScale = 1.0f;

        @Expose
        @ConfigOption(name = "Score Overlay Background Color", desc = "Background color of the score overlay.")
        @ConfigEditorColour
        @ConfigOrder(4)
        public String scoreOverlayBackgroundColor = "0:150:0:0:0";

        @Expose
        public Position scoreOverlayPos = new Position(10, 10, false, true);

        @Expose
        @ConfigOption(name = "Edit Score Overlay Position", desc = "Adjust the score overlay position visually")
        @ConfigEditorButton(runnableId = 2, buttonText = "Edit Position")
        @ConfigOrder(5)
        public String editScoreOverlayPositionButton = "";

        @Expose
        @ConfigOption(name = "S Notifier", desc = "Notify when S is reached (actually reached not virtual).")
        @ConfigEditorBoolean
        @ConfigOrder(6)
        public boolean dungeonsSNotifier = true;

        @Expose
        @ConfigOption(name = "S+ Message on Chat", desc = "Send a message when dungeon is about to be done.")
        @ConfigEditorBoolean
        @ConfigOrder(7)
        public boolean dungeonsSPlusMessage = true;

        @Expose
        @ConfigOption(name = "Custom S+ Message", desc = "Custom message for S+ notification.")
        @ConfigEditorText(forbidden = "")
        @ConfigOrder(8)
        public String dungeonsSPlusCustom = "";

        @Expose
        @ConfigOption(name = "Show Item Secrets and Wither Essences", desc = "Show item secrets and wither essences through walls.")
        @ConfigEditorBoolean
        @ConfigOrder(9)
        public boolean dungeonsItemSecretsDisplay = true;

        @Expose
        @ConfigOption(name = "Item Secrets Color", desc = "Color of item secrets.")
        @ConfigEditorColour
        @ConfigOrder(10)
        public String dungeonsItemSecretsColor = "0:255:255:0:255";

        @Expose
        @ConfigOption(name = "Make Item Secrets Big", desc = "Make item secrets larger.")
        @ConfigEditorBoolean
        @ConfigOrder(11)
        public boolean dungeonsItemSecretsBig = true;

        @Expose
        @ConfigOption(name = "Item Secrets Scale", desc = "Scale of item secrets.")
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 5.0f, minStep = 0.1f)
        @ConfigOrder(12)
        public float dungeonsScaleItemDrop = 3.5f;
    }

    public static class MasterMode7 {
        @Expose
        @ConfigOption(name = "M7 Relic waypoints", desc = "Render waypoints for relics")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean m7Relics = true;

        @Expose
        @ConfigOption(name = "Distance Box", desc = "Render box around dragons statues.")
        @ConfigEditorBoolean
        @ConfigOrder(1)
        public boolean distBox = true;

        @Expose
        @ConfigOption(name = "Dragon Outline", desc = "Outlines each dragon with their respective colors for better visibility.")
        @ConfigEditorBoolean
        @ConfigOrder(2)
        public boolean dragOutline = true;
    }
}

