/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorBoolean;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigOption;

public class Fishing {

    @Expose
    @ConfigOption(name = "Fishing Countdown", desc = "Notify when a fish is ready to be caught.")
    @ConfigEditorBoolean
    public boolean fishingCountdown = true;

    @Expose
    @ConfigOption(name = "Notify Legendary Creatures", desc = "Notify when a legendary creature is caught.")
    @ConfigEditorBoolean
    public boolean fishingLegendaryCreatures = true;

    @Expose
    @ConfigOption(name = "Advanced Fishing Approach Alert", desc = "Extra alert with ETA and Slug Mode. Disable it if you use the regular countdown.")
    @ConfigEditorBoolean
    public boolean fishingAlertEnabled = false;

    @Expose
    @ConfigOption(name = "Fishing Incoming Message", desc = "Show INCOMING when a wake chain approaches the bobber and no valid ETA is available.")
    @ConfigEditorBoolean
    public boolean fishingIncomingMessage = true;

    @Expose
    @ConfigOption(name = "Fishing Bite ETA", desc = "Show the estimated seconds until the wake reaches the bobber.")
    @ConfigEditorBoolean
    public boolean fishingBiteEta = true;

    @Expose
    @ConfigOption(name = "Slug Mode", desc = "Delay the approach alert while your bobber remains in lava, matching the Slug fishing behavior.")
    @ConfigEditorBoolean
    public boolean fishingSlugMode = false;

    @Expose
    @ConfigOption(name = "Trophy Fish Notifier", desc = "Show a title with the fish name when you catch a Trophy Fish in the Crimson Isle.")
    @ConfigEditorBoolean
    public boolean fishingTrophyFish = true;
}
