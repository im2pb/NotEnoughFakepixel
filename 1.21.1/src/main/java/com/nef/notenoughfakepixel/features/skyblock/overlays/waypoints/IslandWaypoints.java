package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Waypoints;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import java.util.List;
import java.util.Objects;

public abstract class IslandWaypoints implements ClientModInitializer {
    private final WaypointRenderer renderer = new WaypointRenderer();
    private String colorSetting;
    private int staticColor = 0xFFFFFF;
    private ChromaColour animatedColor;

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

    private void render(WorldRenderContext context) {
        if (!SkyblockData.isSkyblock() || SkyblockData.currentLocation() != island()
                || context.matrixStack() == null || context.world() == null) return;

        Waypoints settings = ClientConfig.configuration().waypoints;
        if (!settings.generalWaypointToggle || !enabled(settings)) return;

        boolean beacons = beaconsEnabled(settings);
        renderer.render(context, waypoints(), beacons, beacons ? resolveColor(beaconColor(settings)) : 0);
    }

    private int resolveColor(String value) {
        if (!Objects.equals(colorSetting, value)) {
            colorSetting = value;
            ChromaColour parsed = parseColor(value);
            staticColor = parsed == null ? 0xFFFFFF : parsed.getEffectiveColour().getRGB();
            animatedColor = parsed != null && parsed.getTimeForFullRotationInMillis() > 0 ? parsed : null;
        }
        return animatedColor == null ? staticColor : animatedColor.getEffectiveColour().getRGB();
    }

    @SuppressWarnings("deprecation") // MoulConfig's color editor saves this setting in the legacy string format.
    private static ChromaColour parseColor(String value) {
        if (value == null) return null;

        String[] components = value.split(":", -1);
        if (components.length != 5) return null;

        try {
            for (String component : components) {
                int channel = Integer.parseInt(component);
                if (channel < 0 || channel > 255) return null;
            }
        } catch (NumberFormatException error) {
            return null;
        }
        return ChromaColour.forLegacyString(value);
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
