package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.nef.notenoughfakepixel.config.features.Waypoints;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.variables.Location;

import java.util.List;

@RegisterEvents
public final class StJerryWaypoint extends IslandWaypoints {
    private static final List<Waypoint> WAYPOINTS = List.of(Waypoint.atBlock("St. Jerry", -23, 76, 92));

    @Override
    protected Location island() {
        return Location.JERRY;
    }

    @Override
    protected List<Waypoint> waypoints() {
        return WAYPOINTS;
    }

    @Override
    protected boolean enabled(Waypoints settings) {
        return settings.giftWaypointsSettings.stJerryLocation;
    }

    @Override
    protected boolean beaconsEnabled(Waypoints settings) {
        return true;
    }

    @Override
    protected String beaconColor(Waypoints settings) {
        return "0:255:224:78:78";
    }

    @Override
    protected double labelHeight() {
        return 1.5;
    }

    @Override
    protected boolean showDistance() {
        return false;
    }
}
