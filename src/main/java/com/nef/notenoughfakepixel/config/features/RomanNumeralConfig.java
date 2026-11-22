/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorBoolean;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigOption;

public class RomanNumeralConfig {
    @Expose @ConfigOption(name = "Roman Numerals in Tooltips", desc = "Convert Roman numerals (I, V, X, VII, ...) in SkyBlock item tooltips.") @ConfigEditorBoolean
    public boolean tooltips = true;

    @Expose @ConfigOption(name = "Roman Numerals in Tab List", desc = "Show Roman numerals as digits in the tab list (display only; NEF's own tab parsing is not affected).") @ConfigEditorBoolean
    public boolean tabList = true;

    @Expose @ConfigOption(name = "Roman Numerals in Tab Parsing", desc = "Convert strict Roman numerals in NEF tab-list data.") @ConfigEditorBoolean
    public boolean tabParsing = true;
}
