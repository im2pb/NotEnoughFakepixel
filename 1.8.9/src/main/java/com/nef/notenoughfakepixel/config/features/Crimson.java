package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.position.Position;
import io.github.notenoughupdates.moulconfig.annotations.*;

public class Crimson {

    @Expose
    @Category(name = "Boss Notifiers", desc = "Notifications for boss spawns.")
    public BossNotifiers bossNotifiers = new BossNotifiers();

    @Expose
    @Category(name = "Ashfang Settings", desc = "Settings related to Ashfang.")
    public AshfangSettings ashfangSettings = new AshfangSettings();

    public static class BossNotifiers {

            @Expose
            @ConfigOption(name = "Bladesoul Notifier", desc = "Notify when Bladesoul boss spawns.")
            @ConfigEditorBoolean
            public boolean crimsonBladesoulNotifier = true;

            @Expose
            @ConfigOption(name = "Mage Outlaw Notifier", desc = "Notify when Mage Outlaw boss spawns.")
            @ConfigEditorBoolean
            public boolean crimsonMageOutlawNotifier = true;

            @Expose
            @ConfigOption(name = "Ashfang Notifier", desc = "Notify when Ashfang boss spawns.")
            @ConfigEditorBoolean
            public boolean crimsonAshfangNotifier = true;

            @Expose
            @ConfigOption(name = "Barbarian Duke X Notifier", desc = "Notify when Barbarian Duke X boss spawns.")
            @ConfigEditorBoolean
            public boolean crimsonBarbarianDukeXNotifier = true;
    }

    public static class AshfangSettings {

            @Expose
            @ConfigOption(name = "Ashfang Waypoint", desc = "Show waypoint on Ashfang.")
            @ConfigEditorBoolean
            public boolean crimsonAshfangWaypoint = true;

            @Expose
            @ConfigOption(name = "Ashfang Waypoint Color", desc = "Color of Ashfang waypoint.")
            @ConfigEditorColour
            public String crimsonAshfangWaypointColor = "0:100:255:0:255";

            @Expose
            @ConfigOption(name = "Ashfang Hitboxes", desc = "Show hitboxes for Ashfang minions.")
            @ConfigEditorBoolean
            public boolean crimsonAshfangHitboxes = true;

            @Expose
            @ConfigOption(name = "Ashfang Mute Chat", desc = "Mute chat messages from Ashfang minions.")
            @ConfigEditorBoolean
            public boolean crimsonAshfangMuteChat = true;

            @Expose
            @ConfigOption(name = "Ashfang Mute Sound", desc = "Mute sounds from Ashfang minions.")
            @ConfigEditorBoolean
            public boolean crimsonAshfangMuteSound = true;

            @Expose
            @ConfigOption(name = "Ashfang Hurt Sound", desc = "Play sound when Ashfang is hit by Blazing Soul.")
            @ConfigEditorBoolean
            public boolean crimsonAshfangHurtSound = true;

            @Expose
            @ConfigOption(name = "Edit Ashfang Overlay Position", desc = "Adjust the overlay position visually")
            @ConfigEditorButton(runnableId = 6, buttonText = "Edit Position")
            public String editAshOverlayPositionButton = "";

            @Expose
            @ConfigOption(name = "Ashfang Overlay", desc = "Show overlay for Ashfang HP and Blazing Souls.")
            @ConfigEditorBoolean
            public boolean crimsonAshfangOverlay = true;

            @Expose
            @ConfigOption(name = "Gravity Orb Waypoint", desc = "Show waypoint on Gravity Orb.")
            @ConfigEditorBoolean
            public boolean crimsonGravityOrbWaypoint = true;

            @Expose
            @ConfigOption(name = "Gravity Orb Waypoint Color", desc = "Color of Gravity Orb waypoint.")
            @ConfigEditorColour
            public String crimsonBlazingSoulWaypointColor = "0:255:255:0:255";

            @Expose
            public Position ashfangOverlayPos = new Position(10, 10, false, true);
    }

}
