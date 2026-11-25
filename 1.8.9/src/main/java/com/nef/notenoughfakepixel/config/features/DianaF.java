package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.position.Position;
import io.github.notenoughupdates.moulconfig.annotations.*;
import org.lwjgl.input.Keyboard;

public class DianaF {

    @Expose
    @ConfigOption(name = "Minos Inquisitor Alert", desc = "Alert when Minos Inquisitor is dug.")
    @ConfigEditorBoolean
    public boolean dianaMinosInquisitorAlert = true;

    @Expose
    @ConfigOption(name = "Minos Inquisitor Outline", desc = "Outline Minos Inquisitors (the outline color is linked to starred mobs color).")
    @ConfigEditorBoolean
    public boolean dianaMinosInquisitorOutline = true;

    @Expose
    @ConfigOption(name = "Inq outline Color", desc = "Color of the inquisitor outline.")
    @ConfigEditorColour
    public String dianaInqOutlineColor = "0:100:0:255:255";

    @Expose
    @ConfigOption(name = "Waypoint Sounds", desc = "Enable sounds for waypoint creation.")
    @ConfigEditorBoolean
    public boolean dianaWaypointSounds = true;

    @Expose
    @ConfigOption(name = "Disable Explosion Sounds", desc = "Disable burrow digging explosion sounds.")
    @ConfigEditorBoolean
    public boolean dianaDisableDianaExplosionSounds = false;

    @Expose
    @ConfigOption(name = "Disable Ancestral Spade Cooldown Message", desc = "Mute Ancestral Spade cooldown message.")
    @ConfigEditorBoolean
    public boolean dianaCancelCooldownSpadeMessage = true;

    @Expose
    @Category(name = "Burrow Settings", desc = "Settings for burrow waypoints and visuals.")
    public BurrowSettings burrowSettings = new BurrowSettings();

    @Expose
    @Category(name = "Mob Settings", desc = "Settings for Gaia Construct and Siamese.")
    public MobSettings mobSettings = new MobSettings();

    public static class BurrowSettings {

            @Expose
            @ConfigOption(name = "Show Burrow Guess", desc = "Show a guess to the burrow.")
            @ConfigEditorBoolean
            public boolean dianaBurrowGuess = true;

            @Expose
            @ConfigOption(name = "Show Tracer to Burrow Guess", desc = "Draws tracer to burrow guess.")
            @ConfigEditorBoolean
            public boolean dianaTracerBurrowGuess = true;

            @Expose
            @ConfigOption(name = "Burrow Guess Tracer Color", desc = "Color of burrow guess tracer.")
            @ConfigEditorColour
            public String dianaGuessBurrowTracerColor = "0:100:0:255:255";

            @Expose
            @ConfigOption(name = "Warp helper", desc = "Show the closest warp to the guess.")
            @ConfigEditorBoolean
            public boolean dianaWarpHelper = true;

            @Expose
            @ConfigOption(name = "Warp Keybind", desc = "Keybind to quickly warp to the nearest guess burrow.")
            @ConfigEditorKeybind(defaultKey = Keyboard.KEY_H)
            public int warpKeybind = Keyboard.KEY_H;

            @Expose
            @ConfigOption(name = "Warp helper scale", desc = "Scale of the warp text.")
            @ConfigEditorSlider(minValue = 1.0f, maxValue = 5.0f, minStep = 0.1f)
            public float warpHelperScale = 1.0f;

            @Expose
            @ConfigOption(name = "Dark Auction warp", desc = "")
            @ConfigEditorBoolean
            public boolean dianaWarpDa = true;

            @Expose
            @ConfigOption(name = "Museum warp", desc = "")
            @ConfigEditorBoolean
            public boolean dianaWarpMuseum = true;

            @Expose
            @ConfigOption(name = "Crypts warp", desc = "")
            @ConfigEditorBoolean
            public boolean dianaWarpCrypts = true;

            @Expose
            @ConfigOption(name = "Castle warp", desc = "")
            @ConfigEditorBoolean
            public boolean dianaWarpCastle = true;

            @Expose
            @ConfigOption(name = "Edit Warp helper Position", desc = "Adjust the Warp helper position visually")
            @ConfigEditorButton(runnableId = 19, buttonText = "Edit Position")
            public String editWarpHelperPositionButton = "";

            @Expose
            @ConfigOption(name = "Show Waypoints on Burrows", desc = "Show waypoints on burrows.")
            @ConfigEditorBoolean
            public boolean dianaShowWaypointsBurrows = true;

            @Expose
            @ConfigOption(name = "Empty Burrow Color", desc = "Color of empty burrows.")
            @ConfigEditorColour
            public String dianaEmptyBurrowColor = "0:100:0:255:255";

            @Expose
            @ConfigOption(name = "Show Labels on Waypoints", desc = "Show labels on burrow waypoints.")
            @ConfigEditorBoolean
            public boolean dianaShowLabelsWaypoints = true;

            @Expose
            @ConfigOption(name = "Mob Burrow Color", desc = "Color of mob burrows.")
            @ConfigEditorColour
            public String dianaMobBurrowColor = "0:255:255:255:255";

            @Expose
            @ConfigOption(name = "Show Tracers on Waypoints", desc = "Show tracers on burrow waypoints.")
            @ConfigEditorBoolean
            public boolean dianaShowTracersWaypoints = true;

            @Expose
            @ConfigOption(name = "Treasure Burrow Color", desc = "Color of treasure burrows.")
            @ConfigEditorColour
            public String dianaTreasureBurrowColor = "0:255:0:0:255";

            @Expose
            public Position warpHelperPos = new Position(10, 10, false, true);
    }

    public static class MobSettings {

            @Expose
            @ConfigOption(name = "Track Gaia Hits", desc = "Track when Gaia Construct can be damaged.")
            @ConfigEditorBoolean
            public boolean dianaGaiaConstruct = true;

            @Expose
            @ConfigOption(name = "Gaia Hittable Color", desc = "Color when Gaia is hittable.")
            @ConfigEditorColour
            public String dianaGaiaHittableColor = "0:250:255:0:255";

            @Expose
            @ConfigOption(name = "Show Hittable Siamese", desc = "Show when Siamese can be damaged.")
            @ConfigEditorBoolean
            public boolean dianaSiamese = true;

            @Expose
            @ConfigOption(name = "Gaia Un-hittable Color", desc = "Color when Gaia is not hittable.")
            @ConfigEditorColour
            public String dianaGaiaUnhittableColor = "0:255:0:0:255";

            @Expose
            @ConfigOption(name = "Siamese Hittable Color", desc = "Color when Siamese is hittable.")
            @ConfigEditorColour
            public String dianaSiameseHittableColor = "0:250:255:0:255";
    }

}
