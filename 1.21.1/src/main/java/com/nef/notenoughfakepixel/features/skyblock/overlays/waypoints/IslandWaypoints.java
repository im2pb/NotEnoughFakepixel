package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Waypoints;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import java.util.List;

public abstract class IslandWaypoints implements ClientModInitializer {
    private final WaypointRenderer renderer = new WaypointRenderer();
    private final ConfigColor color = new ConfigColor();

    @Override
    public final void onInitializeClient() {
        WorldRenderEvents.LAST.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    protected abstract Location island();

    protected abstract List<Waypoint> waypoints();

    protected abstract boolean enabled(Waypoints settings);

    protected abstract boolean beaconsEnabled(Waypoints settings);

    protected abstract String beaconColor(Waypoints settings);

    protected double labelHeight() {
        return 3.5;
    }

    protected boolean showDistance() {
        return true;
    }

    private void render(WorldRenderContext context) {
        if (!SkyblockData.isSkyblock() || SkyblockData.currentLocation() != island()
                || context.matrixStack() == null || context.world() == null) return;

        Waypoints settings = ClientConfig.configuration().waypoints;
        if (!settings.generalWaypointToggle || !enabled(settings)) return;

        boolean beacons = beaconsEnabled(settings);
        renderer.render(context, waypoints(), beacons, beacons ? color.resolve(beaconColor(settings)) : 0,
                labelHeight(), showDistance());
    }

    /** The coordinates give the exact center of the beam base. A null RGB override uses the island setting. */
    public record Waypoint(String name, double x, double y, double z, Integer beamColorOverride) {
        public Waypoint(String name, double x, double y, double z) {
            this(name, x, y, z, null);
        }

        public static Waypoint atBlock(String name, int x, int y, int z) {
            return atBlock(name, x, y, z, null);
        }

        public static Waypoint atBlock(String name, int x, int y, int z, Integer beamColorOverride) {
            return new Waypoint(name, x + 0.5, y, z + 0.5, beamColorOverride);
        }
    }
}
