package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.position.Position;
import io.github.notenoughupdates.moulconfig.annotations.*;

public class Slayer {

    // Top-Level Options
    @Expose
    @ConfigOption(name = "Faster Maddox Calling", desc = "Method for faster Maddox calling.")
    @ConfigEditorDropdown(values = {"Auto Open", "Semi Auto", "Disabled"})
    public int slayerMaddoxCalling = 1;

    @Expose
    @Category(name = "Slayer Overlay", desc = "Slayer Overlay Settings")
    public SlayerOverlaySettings slayerOverlaySettings = new SlayerOverlaySettings();

    @Expose
    @Category(name = "Miniboss Settings", desc = "Settings for slayer minibosses.")
    public MinibossSettings minibossSettings = new MinibossSettings();

    @Expose
    @Category(name = "Slayer Boss Settings", desc = "Settings for slayer bosses.")
    public BossSettings bossSettings = new BossSettings();

    @Expose
    @Category(name = "Voidgloom Beacon Settings", desc = "Settings for beacon waypoints.")
    public BeaconSettings beaconSettings = new BeaconSettings();

    @Expose
    @Category(name = "Inferno Demonlord", desc = "Settings for the Inferno Demonlord boss fight.")
    public InfernoDemonlord infernoDemonlord = new InfernoDemonlord();

    public static class SlayerOverlaySettings {

            @Expose
            @ConfigOption(name = "Slayer Overlay", desc = "Enable/Disable slayers overlay")
            @ConfigEditorBoolean
            public boolean slayerOverlay = true;

            @Expose
            @ConfigOption(name = "Position", desc = "Edit slayer overlay position.")
            @ConfigEditorButton(runnableId = 7, buttonText = "Edit")
            public String editSlayerOverlayPos = "";

            @Expose
            @ConfigOption(name = "Hide on Tab", desc = "Hide the slayer overlay when the tab list is open.")
            @ConfigEditorBoolean
            public boolean slayerOverlayHideOnTab = true;

            @Expose
            @ConfigOption(name = "Hide on Chat", desc = "Hide the slayer overlay when the chat is open.")
            @ConfigEditorBoolean
            public boolean slayerOverlayHideOnChat = true;

            @Expose
            @ConfigOption(name = "Slayer Overlay Scale", desc = "Scale of the slayer overlay text.")
            @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
            public float slayerOverlayScale = 1.0f;

            @Expose
            @ConfigOption(name = "Slayer Overlay Background Color", desc = "Background color of the slayer overlay.")
            @ConfigEditorColour
            public String slayerOverlayBackgroundColor = "0:150:0:0:0";

            @Expose
            public Position slayerOverlayPos = new Position(0, 0, false, false);
    }

    public static class MinibossSettings {

            @Expose
            @ConfigOption(name = "Slayer Minibosses Display", desc = "Draw a box around slayer minibosses.")
            @ConfigEditorBoolean
            public boolean slayerMinibosses = true;

            @Expose
            @ConfigOption(name = "Miniboss Spawn Title", desc = "Show a title when a miniboss spawns.")
            @ConfigEditorBoolean
            public boolean slayerMinibossTitle = true;

            @Expose
            @ConfigOption(name = "Miniboss Sound Notification", desc = "Play a sound when a miniboss spawns.")
            @ConfigEditorBoolean
            public boolean slayerMinibossSound = true;

            @Expose
            @ConfigOption(name = "Slayer Minibosses Color", desc = "Color of slayer minibosses.")
            @ConfigEditorColour
            public String slayerColor = "0:92:154:255:255";
    }

    public static class BossSettings {

            @Expose
            @ConfigOption(name = "Slayer Bosses Display", desc = "Draw a box around slayer bosses.")
            @ConfigEditorBoolean
            public boolean slayerBosses = true;

            @Expose
            @ConfigOption(name = "Slayer Bosses Outline", desc = "Draw an outline around slayer bosses.")
            @ConfigEditorBoolean
            public boolean slayerBossesOutline = false;

            @Expose
            @ConfigOption(name = "Slayer Boss Health Display", desc = "Displays the slayer health on screen.")
            @ConfigEditorBoolean
            public boolean slayerBossHP = true;

            @Expose
            @ConfigOption(name = "Edit Slayer HP Overlay Position", desc = "Adjust the slayer hp overlay position visually")
            @ConfigEditorButton(runnableId = 8, buttonText = "Edit Position")
            public String editSlayerOverlayPositionButton = "";

            @Expose
            @ConfigOption(name = "Slayer Bosses Color", desc = "Color of slayer bosses.")
            @ConfigEditorColour
            public String slayerBossColor = "0:92:154:255:255";

            @Expose
            @ConfigOption(name = "Slayer Boss Time", desc = "Show slayer boss time.")
            @ConfigEditorBoolean
            public boolean slayerBossTimer = true;

            @Expose
            public Position slayerBossHPPos = new Position(10, 10, false, true);
    }

    public static class BeaconSettings {

            @Expose
            @ConfigOption(name = "Show Beacon Waypoint", desc = "Show waypoint for beacon.")
            @ConfigEditorBoolean
            public boolean slayerShowBeaconPath = true;

            @Expose
            @ConfigOption(name = "Beacon Color", desc = "Color of beacon waypoint.")
            @ConfigEditorColour
            public String slayerBeaconColor = "0:128:0:128:255";

            @Expose
            @ConfigOption(name = "Show Beacon Tracer", desc = "Traces a line to the beacon.")
            @ConfigEditorBoolean
            public boolean showTracerToBeacon = true;

            @Expose
            @ConfigOption(name = "Beacon notifier", desc = "Shows a message middle screen when beacon is detected.")
            @ConfigEditorBoolean
            public boolean notifyBeaconInScreen = true;
    }

    public static class InfernoDemonlord {

            @Expose
            @ConfigOption(name = "Display Blaze Pillar Title", desc = "Display title when a blaze pillar is nearby.")
            @ConfigEditorBoolean
            public boolean slayerFirePillarDisplay = true;

            @Expose
            @ConfigOption(name = "Filter Hellion Shield Spam", desc = "Hide the 'Hellion Shield' and 'Strike using attunement' messages when hitting with the wrong attunement.")
            @ConfigEditorBoolean
            public boolean slayerHellionShieldFilter = true;

    }

    @Expose
    @Category(name = "Show Attunements Overlay", desc = "Settings for attunement outline and highlight.")
    public BlazeAttunementSettings blazeAttunementSettings = new BlazeAttunementSettings();

    public static class BlazeAttunementSettings {

            @Expose
            @ConfigOption(name = "Show Outline", desc = "Draw a coloured outline around attunement minions.")
            @ConfigEditorBoolean
            public boolean slayerBlazeAttunementOutline = true;

            @Expose
            @ConfigOption(name = "Show Full Attunement", desc = "Fill the attunement minion model with its attunement colour.")
            @ConfigEditorBoolean
            public boolean slayerBlazeAttunementFill = false;
    }
}
