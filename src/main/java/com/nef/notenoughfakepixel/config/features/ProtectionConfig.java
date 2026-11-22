/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.*;

public class ProtectionConfig {
    @Expose @ConfigOption(name = "Enabled", desc = "Protect items by their SkyBlock item UUID.") @ConfigEditorBoolean
    public boolean enabled = true;
    @Expose @ConfigOption(name = "Show Protected Star", desc = "Show a small star on protected items.") @ConfigEditorBoolean
    public boolean showProtectedStar = true;
    @Expose @ConfigOption(name = "Star Opacity", desc = "Opacity of the protected-item indicator.") @ConfigEditorSlider(minValue = 0f, maxValue = 1f, minStep = 0.05f)
    public float starOpacity = 1f;
    @Expose @ConfigOption(name = "Chat Notifications", desc = "Notify when an item is protected or an unsafe action is blocked.") @ConfigEditorBoolean
    public boolean showChatNotifications = true;
}
