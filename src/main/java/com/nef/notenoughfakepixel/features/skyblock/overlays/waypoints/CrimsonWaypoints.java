/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;

/** Static Crimson Isle landmarks and boss areas, separate from the dynamic Ashfang helper. */
@RegisterEvents
public class CrimsonWaypoints {
    private static final Color BOSS_COLOR = new Color(159, 31, 31);
    private static final Point[] POINTS = new Point[]{
            new Point("Barbarian Duke", -535, 117, -902, BOSS_COLOR),
            new Point("Mage Outlaw", -179, 105, -857, BOSS_COLOR),
            new Point("Bladesoul", -294, 82, -517, BOSS_COLOR),
            new Point("Stronghold", -360, 95, -516),
            new Point("Mystic Marsh", -197, 77, -767),
            new Point("Blazing Volcano", -364, 119, -768),
            new Point("Ashfang", -483, 135, -1014),
            new Point("Smoldering", -275, 130, -979),
            new Point("Burning Desert", -506, 91, -774),
            new Point("Dragontail", -622, 199, -817),
            new Point("Scarleton", -78, 107, -803),
            new Point("Dojo", -235, 107, -596)
    };

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!Config.feature.waypoints.generalWaypointToggle || !Config.feature.waypoints.crimsonWaypoints) return;
        if (SkyblockData.getCurrentLocation() != Location.CRIMSON_ISLE) return;
        Color configured = ColorUtils.getColor(Config.feature.waypoints.crimsonBeaconsColor);
        for (Point point : POINTS) {
            BlockPos pos = new BlockPos(point.x, point.y, point.z);
            if (Config.feature.waypoints.crimsonBeacons) {
                Color beam = point.overrideColor == null ? configured : point.overrideColor;
                RenderUtils.renderBeaconBeam(pos, beam.getRGB(), 1.0f, event.partialTicks);
            }
            RenderUtils.renderWaypointText(point.name, pos.up(3), event.partialTicks);
        }
    }

    private static final class Point {
        final String name; final int x, y, z; final Color overrideColor;
        Point(String name, int x, int y, int z) { this(name, x, y, z, null); }
        Point(String name, int x, int y, int z, Color overrideColor) {
            this.name = name; this.x = x; this.y = y; this.z = z; this.overrideColor = overrideColor;
        }
    }
}
