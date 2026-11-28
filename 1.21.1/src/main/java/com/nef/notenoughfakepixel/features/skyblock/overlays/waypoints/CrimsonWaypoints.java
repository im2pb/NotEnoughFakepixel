package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.nef.notenoughfakepixel.config.features.Waypoints;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.crimson.AshfangHelper;
import com.nef.notenoughfakepixel.variables.Location;

import java.util.List;

@RegisterEvents
public final class CrimsonWaypoints extends IslandWaypoints {
    private static final int BOSS_COLOR = 0x9F1F1F;
    private static final Waypoint ASHFANG = Waypoint.atBlock("Ashfang", -483, 135, -1014);
    private static final List<Waypoint> WAYPOINTS = List.of(
            Waypoint.atBlock("Barbarian Duke", -535, 117, -902, BOSS_COLOR),
            Waypoint.atBlock("Mage Outlaw", -179, 105, -857, BOSS_COLOR),
            Waypoint.atBlock("Bladesoul", -294, 82, -517, BOSS_COLOR),
            Waypoint.atBlock("Stronghold", -360, 95, -516),
            Waypoint.atBlock("Mystic Marsh", -197, 77, -767),
            Waypoint.atBlock("Blazing Volcano", -364, 119, -768),
            ASHFANG,
            Waypoint.atBlock("Smoldering", -275, 130, -979),
            Waypoint.atBlock("Burning Desert", -506, 91, -774),
            Waypoint.atBlock("Dragontail", -622, 199, -817),
            Waypoint.atBlock("Scarleton", -78, 107, -803),
            Waypoint.atBlock("Dojo", -235, 107, -596)
    );
    private static final List<Waypoint> WITHOUT_ASHFANG = WAYPOINTS.stream().filter(waypoint -> waypoint != ASHFANG).toList();

    @Override
    protected Location island() {
        return Location.CRIMSON_ISLE;
    }

    @Override
    protected List<Waypoint> waypoints() {
        return AshfangHelper.INSTANCE.showsBossWaypoint() ? WITHOUT_ASHFANG : WAYPOINTS;
    }

    @Override
    protected boolean enabled(Waypoints settings) {
        return settings.crimsonWaypointsSettings.crimsonWaypoints;
    }

    @Override
    protected boolean beaconsEnabled(Waypoints settings) {
        return settings.crimsonWaypointsSettings.crimsonBeacons;
    }

    @Override
    protected String beaconColor(Waypoints settings) {
        return settings.crimsonWaypointsSettings.crimsonBeaconsColor;
    }
}
