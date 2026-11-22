/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.*;

public class Waypoints {

    @Expose
    @ConfigOption(name = "Fairy Soul Waypoints", desc = "Settings for fairy soul waypoints.")
    @ConfigEditorAccordion(id = 1)
    public boolean fairySoulAccordion = false;

    @Expose
    @ConfigOption(name = "Enable Fairy Soul Waypoints", desc = "Enable fairy soul waypoints.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean fairySoulWaypoints = false;

    @Expose
    @ConfigOption(name = "Fairy Soul Waypoints Color", desc = "Color of fairy soul waypoints.")
    @ConfigEditorColour
    @ConfigAccordionId(id = 1)
    public String fairySoulWaypointsColor = "0:100:255:255:255";

    @Expose
    @ConfigOption(name = "Reset Fairy Soul Progress", desc = "Deletes the locally tracked Fairy Soul progress and starts tracking again.")
    @ConfigEditorButton(runnableId = "fairySoulsReset", buttonText = "Reset")
    @ConfigAccordionId(id = 1)
    public String fairySoulsReset = "";

    @Expose
    @ConfigOption(name = "Gift Waypoints", desc = "Enable gifts waypoints in Jerry Island.")
    @ConfigEditorBoolean
    @ConfigEditorAccordion(id = 2)
    public boolean giftWaypointsAccordion = false;

    @Expose
    @ConfigOption(name = "Enable Gift Waypoints", desc = "Enable gift waypoints in Jerry Island.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean giftWaypoints = false;

    @Expose
    @ConfigOption(name = "Gift Waypoints Color", desc = "Color of gift waypoints.")
    @ConfigEditorColour
    @ConfigAccordionId(id = 2)
    public String giftWaypointsColor = "0:255:0:255:255";

    @Expose
    @ConfigOption(name = "Show St. Jerry", desc = "Shows St. Jerry location.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean stJerryLocation = false;


    @Expose
    @ConfigOption(name = "General Waypoint Toggle", desc = "Master switch for island waypoint groups.")
    @ConfigEditorBoolean
    public boolean generalWaypointToggle = true;

    @Expose
    @ConfigOption(name = "Crimson Isle Waypoints", desc = "Show major Crimson Isle locations and boss areas.")
    @ConfigEditorBoolean
    public boolean crimsonWaypoints = true;

    @Expose
    @ConfigOption(name = "Crimson Isle Waypoint Beacons", desc = "Show beacon beams at Crimson Isle waypoint locations.")
    @ConfigEditorBoolean
    public boolean crimsonBeacons = true;

    @Expose
    @ConfigOption(name = "Crimson Isle Beacon Color", desc = "Beacon color for the Crimson Isle waypoint set.")
    @ConfigEditorColour
    public String crimsonBeaconsColor = "0:190:0:255:0";

}
