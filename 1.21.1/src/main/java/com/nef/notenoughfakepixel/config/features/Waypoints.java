package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.FairySouls;
import io.github.notenoughupdates.moulconfig.annotations.*;

public class Waypoints {

    @Expose
    @ConfigOption(name = "Enable Waypoints", desc = "Show or hide all NotEnoughFakepixel waypoint markers")
    @ConfigEditorBoolean
    public boolean generalWaypointToggle = true;

    @Expose
    @Category(name = "Fairy Soul Waypoints", desc = "Settings for fairy soul waypoints")
    public FairySoulWaypointsSettings fairySoulWaypointsSettings = new FairySoulWaypointsSettings();

    @Expose
    @Category(name = "Gift Waypoints", desc = "Enable gifts waypoints in Jerry Island")
    public GiftWaypointsSettings giftWaypointsSettings = new GiftWaypointsSettings();

    @Expose
    @Category(name = "Dwarven Waypoints Settings", desc = "Settings for dwarven waypoints")
    public DwarvenWaypointsSettings dwarvenWaypointsSettings = new DwarvenWaypointsSettings();

    @Expose
    @Category(name = "Crimson Waypoints Settings", desc = "Settings for crimson waypoints")
    public CrimsonWaypointsSettings crimsonWaypointsSettings = new CrimsonWaypointsSettings();

    public static class FairySoulWaypointsSettings {

            @Expose
            @ConfigOption(name = "Enable Fairy Soul Waypoints", desc = "Enable fairy soul waypoints")
            @ConfigEditorBoolean
            public boolean fairySoulWaypoints = false;

            @Expose
            @ConfigOption(name = "Fairy Soul Waypoints Color", desc = "Color of fairy soul waypoints")
            @ConfigEditorColour
            public String fairySoulWaypointsColor = "0:100:255:255:255";

            @ConfigOption(name = "Reset Fairy Soul Progress", desc = "Clear locally recorded Fairy Souls for all SkyBlock profiles")
            @ConfigEditorButton(buttonText = "Reset")
            public transient Runnable fairySoulsReset = FairySouls::confirmReset;

    }

    public static class GiftWaypointsSettings {

            @Expose
            @ConfigOption(name = "Enable Gift Waypoints", desc = "Show uncollected gift markers on Jerry's Island")
            @ConfigEditorBoolean
            public boolean giftWaypoints = false;

            @Expose
            @ConfigOption(name = "Gift Waypoints Color", desc = "Color of gift waypoints")
            @ConfigEditorColour
            public String giftWaypointsColor = "0:255:0:255:255";

            @Expose
            @ConfigOption(name = "Show St. Jerry", desc = "Show a beacon and label for St. Jerry on Jerry's Island")
            @ConfigEditorBoolean
            public boolean stJerryLocation = false;
    }

    public static class DwarvenWaypointsSettings {

            @Expose
            @ConfigOption(name = "Enable Dwarven Waypoints", desc = "Enable Area Waypoints in the Dwarven Mines")
            @ConfigEditorBoolean
            public boolean miningDwarvenWaypoints = true;

            @Expose
            @ConfigOption(name = "Enable Dwarven Waypoint beacons", desc = "Enable beacons on every waypoint (waypoint must be enabled)")
            @ConfigEditorBoolean
            public boolean miningDwarvenBeacons = true;

            @Expose
            @ConfigOption(name = "Beacons Color", desc = "Color of waypoint beacons")
            @ConfigEditorColour
            public String miningDwarvenBeaconsColor = "0:190:0:255:0";
    }

    public static class CrimsonWaypointsSettings {

            @Expose
            @ConfigOption(name = "Enable Crimson Waypoints", desc = "Enable Area Waypoints in the Crimson Isle")
            @ConfigEditorBoolean
            public boolean crimsonWaypoints = true;

            @Expose
            @ConfigOption(name = "Enable Crimson Waypoint beacons", desc = "Enable beacons on every waypoint (waypoint must be enabled)")
            @ConfigEditorBoolean
            public boolean crimsonBeacons = true;

            @Expose
            @ConfigOption(name = "Beacons Color", desc = "Color of waypoint beacons")
            @ConfigEditorColour
            public String crimsonBeaconsColor = "0:190:0:255:0";
    }

}
