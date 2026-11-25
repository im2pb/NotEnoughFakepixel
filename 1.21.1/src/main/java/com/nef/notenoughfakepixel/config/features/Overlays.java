package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.position.Position;
import io.github.notenoughupdates.moulconfig.annotations.*;

public class Overlays {

    @Expose
    @ConfigOption(name = "Equipment Overlay", desc = "Shows what equipment u are wearing")
    @ConfigEditorBoolean
    public boolean equipment = true;

    @Expose
    @Category(name = "Stat Bars", desc = "Setting for Stat Bars")
    public StatBars statBars = new StatBars();

    public static class StatBars {

            @Expose
            @ConfigOption(name = "Enable Stat Bars", desc = "Enable/Disable this feature")
            @ConfigEditorBoolean
            public boolean statOverlay = false;

            @Expose
            @ConfigOption(name = "Disable Action Bar", desc = "Enable/Disable whether or not it should show ur stats in the action bar")
            @ConfigEditorBoolean
            public boolean disableActionBar = false;

            @Expose
            @ConfigOption(name = "Disable Default Render", desc = "Enable/Disable whether or not it should render vanilla minecraft's bars")
            @ConfigEditorBoolean
            public boolean disableIcons = false;

    }

    @Expose
    @Category(name = "Health Bar", desc = "Settings for the health bar")
    public HealthBarSettings healthBarSettings = new HealthBarSettings();

    @Expose
    @Category(name = "Mana Bar", desc = "Settings for the mana bar")
    public ManaBarSettings manaBarSettings = new ManaBarSettings();

    @Expose
    @Category(name = "Exp Bar", desc = "Settings for the exp bar")
    public ExpBarSettings expBarSettings = new ExpBarSettings();

    @Expose
    @Category(name = "Speed Bar", desc = "Settings for the speed bar")
    public SpeedBarSettings speedBarSettings = new SpeedBarSettings();

    @Expose
    @Category(name = "Defence Bar", desc = "Settings for the defence bar")
    public DefenceBarSettings defenceBarSettings = new DefenceBarSettings();

    public static class HealthBarSettings {

            @Expose
            @ConfigOption(name = "Health Bar", desc = "Enable/Disable health bar")
            @ConfigEditorBoolean
            public boolean healthBar = true;

            @Expose
            @ConfigOption(name = "Bar Length", desc = "How long the health bar is")
            @ConfigEditorDropdown(values = {"Tiny","Small","Medium","Large"})
            public int barLengthH = 2;

            @Expose
            public Position posHealth = new Position(100,100);
    }

    public static class ManaBarSettings {

            @Expose
            @ConfigOption(name = "Mana Bar", desc = "Enable/Disable mana bar")
            @ConfigEditorBoolean
            public boolean manaBar = true;

            @Expose
            @ConfigOption(name = "Bar Length", desc = "How long the mana bar is")
            @ConfigEditorDropdown(values = {"Tiny","Small","Medium","Large"})
            public int barLengthM = 2;

            @Expose
            public Position posMana = new Position(200,100);
    }

    public static class ExpBarSettings {

            @Expose
            @ConfigOption(name = "Exp Bar", desc = "Enable/Disable exp bar")
            @ConfigEditorBoolean
            public boolean expBar = true;

            @Expose
            @ConfigOption(name = "Bar Length", desc = "How long the exp bar is")
            @ConfigEditorDropdown(values = {"Tiny","Small","Medium","Large"})
            public int barLengthE = 3;

            @Expose
            public Position posExp = new Position(200,200);
    }

    public static class SpeedBarSettings {

            @Expose
            @ConfigOption(name = "Speed Bar", desc = "Enable/Disable speed bar")
            @ConfigEditorBoolean
            public boolean speedBar = true;

            @Expose
            @ConfigOption(name = "Bar Length", desc = "How long the speed bar is")
            @ConfigEditorDropdown(values = {"Tiny","Small","Medium","Large"})
            public int barLengthS = 2;

            @Expose
            public Position posSpeed = new Position(300,100);
    }

    public static class DefenceBarSettings {

            @Expose
            @ConfigOption(name = "Defence Bar", desc = "Enable/Disable defence bar")
            @ConfigEditorBoolean
            public boolean defenceBar = true;

            @Expose
            @ConfigOption(name = "Bar Length", desc = "How long the defence bar is")
            @ConfigEditorDropdown(values = {"Tiny","Small","Medium","Large"})
            public int barLengthD = 2;

            @Expose
            public Position posDefense = new Position(300,200);
    }
}

