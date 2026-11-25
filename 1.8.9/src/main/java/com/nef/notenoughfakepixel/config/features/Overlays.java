package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.position.Position;
import io.github.notenoughupdates.moulconfig.annotations.*;

public class Overlays {

//    @Expose
//    @ConfigOption(name = "Storage", desc = "Storage Overlay Settings")
//    @ConfigEditorAccordion(id = 0)
//    public boolean storage = false;
//
//    @Expose
//    @ConfigOption(name = "Storage Overlay", desc = "Redesign of Storage GUI")
//    @ConfigEditorBoolean
//    @ConfigAccordionId(id = 0)
//    public boolean storageOverlay = false;
//
//
//    @Expose
//    @ConfigOption(name = "Ender Chest Tint", desc = "Add a tint to the ender chest texture")
//    @ConfigEditorColour
//    @ConfigAccordionId(id = 0)
//    public String enderTint = "0:255:255:255:255";
//
//    @Expose
//    @ConfigOption(name = "Backpack Tint", desc = "Add a tint to the backpack texture")
//    @ConfigEditorColour
//    @ConfigAccordionId(id = 0)
//    public String bagTint = "0:255:255:255:255";
//
//    @Expose
//    @ConfigOption(name = "Inventory Tint", desc = "Add a tint to the inventory texture")
//    @ConfigEditorColour
//    @ConfigAccordionId(id = 0)
//    public String invTint = "0:255:255:255:255";
//
//    @Expose
//    @ConfigOption(name = "Button Editor", desc = "Change Position or edit/add any inventory button")
//    @ConfigEditorButton(buttonText = "EDIT", runnableId = "nefButtons")
//    public String editor = "";
//
//    @Expose
//    @ConfigOption(name = "Inventory Buttons" , desc = "Settings for Inventory Buttons")
//    @ConfigEditorAccordion(id = 7)
//    public boolean invButtonsAccordion = false;
//
//    @Expose
//    @ConfigOption(name = "Enable Inventory Buttons", desc = "Show Inventory Buttons in the inventory")
//    @ConfigEditorBoolean
//    @ConfigAccordionId(id = 7)
//    public boolean invButtons = false;
//
//    @Expose
//    @ConfigOption(name = "Snap to Grid", desc = "Snap items to grid when moving them in storage")
//    @ConfigEditorBoolean
//    @ConfigAccordionId(id = 0)
//    public boolean snapToGrid = false;
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

            @Expose
            @ConfigOption(name = "Stat Bar Position", desc = "Edit position of stat bars")
            @ConfigEditorButton(runnableId = 9,buttonText = "Edit Positions")
            public String statEditor = "";

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

