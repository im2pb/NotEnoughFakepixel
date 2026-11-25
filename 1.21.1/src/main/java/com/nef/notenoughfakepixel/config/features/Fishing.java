package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Fishing {

    @Expose
    @Category(name = "Fishing Alert", desc = "Configure the fishing bite alert")
    public FishingAlert alert = new FishingAlert();

    @Expose
    @Category(name = "Catch Notifications", desc = "Configure fishing catch title notifications")
    public CatchNotifications notifications = new CatchNotifications();

    public static class FishingAlert {
        @Expose
        @ConfigOption(name = "Enable Alert", desc = "Activate the fishing alert feature")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Enable Incoming Message", desc = "Show INCOMING when a fish is approaching the bobber")
        @ConfigEditorBoolean
        public boolean incomingMessage = true;

        @Expose
        @ConfigOption(name = "Enable Bite ETA", desc = "Show a countdown until the fish bites")
        @ConfigEditorBoolean
        public boolean biteEta = true;

        @Expose
        @ConfigOption(name = "Enable Slug Mode", desc = "Suppresses all alerts for 20s after the bobber enters lava, matching the Slug fish catch requirement")
        @ConfigEditorBoolean
        public boolean slugMode = false;
    }

    public static class CatchNotifications {
        @Expose
        @ConfigOption(name = "Notify Legendary Creatures", desc = "Notify when a legendary creature is caught")
        @ConfigEditorBoolean
        public boolean legendaryCreatures = true;

        @Expose
        @ConfigOption(name = "Notify on Trophy Fish", desc = "Notify when a trophy fish is caught")
        @ConfigEditorBoolean
        public boolean trophyFish = true;
    }
}
