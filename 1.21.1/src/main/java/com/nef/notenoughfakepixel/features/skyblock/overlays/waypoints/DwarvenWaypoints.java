package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.nef.notenoughfakepixel.config.features.Waypoints;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.variables.Location;

import java.util.List;

@RegisterEvents
public final class DwarvenWaypoints extends IslandWaypoints {
    private static final List<Waypoint> WAYPOINTS = List.of(
            Waypoint.atBlock("The Lift", -62, 200, -121),
            Waypoint.atBlock("Dwarven Vilage", 7, 200, -121),
            Waypoint.atBlock("Lava Springs", 57, 207, -9),
            Waypoint.atBlock("The Forge", 0, 148, -53),
            Waypoint.atBlock("Rampart's Quarry", -77, 153, -10),
            Waypoint.atBlock("Far Reserve", -154, 149, -17),
            Waypoint.atBlock("Goblin Burrows", -137, 146, 128),
            Waypoint.atBlock("The Great Ice Wall", 0, 128, 160),
            Waypoint.atBlock("Royal Palace", 129, 195, 176),
            Waypoint.atBlock("Royal Mines", 150, 151, 33),
            Waypoint.atBlock("Cliffside Veins", 38, 128, 32),
            Waypoint.atBlock("Divan's Gateway", 0, 128, 96),
            Waypoint.atBlock("Upper Mines", -117, 181, -63),
            Waypoint.atBlock("The Mist", -12, 76, 109)
    );

    @Override
    protected Location island() {
        return Location.DWARVEN;
    }

    @Override
    protected List<Waypoint> waypoints() {
        return WAYPOINTS;
    }

    @Override
    protected boolean enabled(Waypoints settings) {
        return settings.dwarvenWaypointsSettings.miningDwarvenWaypoints;
    }

    @Override
    protected boolean beaconsEnabled(Waypoints settings) {
        return settings.dwarvenWaypointsSettings.miningDwarvenBeacons;
    }

    @Override
    protected String beaconColor(Waypoints settings) {
        return settings.dwarvenWaypointsSettings.miningDwarvenBeaconsColor;
    }
}
