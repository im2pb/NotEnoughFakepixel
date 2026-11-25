package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder;

public class Fishing {

    @Expose
    @Category(name = "Fishing Alert", desc = "Configure the fishing bite alert.")
    @ConfigOrder(0)
    public FishingAlert alert = new FishingAlert();

    @Expose
    @Category(name = "Catch Notifications", desc = "Configure fishing catch title notifications.")
    @ConfigOrder(1)
    public CatchNotifications notifications = new CatchNotifications();

    @Expose(serialize = false, deserialize = true)
    private Boolean fishingCountdown;

    @Expose(serialize = false, deserialize = true)
    private Boolean fishingIncomingMessage;

    @Expose(serialize = false, deserialize = true)
    private Boolean fishingBiteEta;

    @Expose(serialize = false, deserialize = true)
    private Boolean fishingSlugMode;

    @Expose(serialize = false, deserialize = true)
    private Boolean fishingLegendaryCreatures;

    @Expose(serialize = false, deserialize = true)
    private Boolean fishingTrophyFish;

    public void migrateLegacyOptions() {
        if (fishingCountdown != null) {
            alert.enabled = fishingCountdown;
        }
        if (fishingIncomingMessage != null) {
            alert.incomingMessage = fishingIncomingMessage;
        }
        if (fishingBiteEta != null) {
            alert.biteEta = fishingBiteEta;
        }
        if (fishingSlugMode != null) {
            alert.slugMode = fishingSlugMode;
        }
        if (fishingLegendaryCreatures != null) {
            notifications.legendaryCreatures = fishingLegendaryCreatures;
        }
        if (fishingTrophyFish != null) {
            notifications.trophyFish = fishingTrophyFish;
        }
    }

    public static class FishingAlert {
        @Expose
        @ConfigOption(name = "Enable Alert", desc = "Activate the fishing alert feature.")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Enable Incoming Message", desc = "Show INCOMING when a fish is approaching the bobber.")
        @ConfigEditorBoolean
        @ConfigOrder(1)
        public boolean incomingMessage = true;

        @Expose
        @ConfigOption(name = "Enable Bite ETA", desc = "Show a countdown until the fish bites.")
        @ConfigEditorBoolean
        @ConfigOrder(2)
        public boolean biteEta = true;

        @Expose
        @ConfigOption(name = "Enable Slug Mode", desc = "Suppresses all alerts for 20s after the bobber enters lava, matching the Slug fish catch requirement.")
        @ConfigEditorBoolean
        @ConfigOrder(3)
        public boolean slugMode = false;
    }

    public static class CatchNotifications {
        @Expose
        @ConfigOption(name = "Notify Legendary Creatures", desc = "Notify when a legendary creature is caught.")
        @ConfigEditorBoolean
        @ConfigOrder(0)
        public boolean legendaryCreatures = true;

        @Expose
        @ConfigOption(name = "Notify on Trophy Fish", desc = "Notify when a trophy fish is caught.")
        @ConfigEditorBoolean
        @ConfigOrder(1)
        public boolean trophyFish = true;
    }
}
