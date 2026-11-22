/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorBoolean;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigOption;

public class Pets {
    @Expose
    @ConfigOption(name = "Pet Tracker", desc = "Shows collected, missing, and upgradeable pets with each pet’s potential score gain.")
    @ConfigEditorBoolean
    public boolean petTracker = true;
}
