/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.*;

public class SearchBarConfig {
    @Expose @ConfigOption(name = "Search Bar", desc = "Highlight matching items in supported inventories without hiding or moving any item.") @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose @ConfigOption(name = "Show Item Highlight", desc = "Draw a subtle tint and outline around matching item slots.") @ConfigEditorBoolean
    public boolean showItemHighlight = true;

    @Expose @ConfigOption(name = "Width", desc = "Search bar width.") @ConfigEditorSlider(minValue = 60f, maxValue = 240f, minStep = 1f)
    public float width = 140f;

    @Expose @ConfigOption(name = "Height", desc = "Search bar height.") @ConfigEditorSlider(minValue = 12f, maxValue = 30f, minStep = 1f)
    public float height = 18f;

    @Expose @ConfigOption(name = "X", desc = "Horizontal offset from the active inventory panel.") @ConfigEditorSlider(minValue = -400f, maxValue = 400f, minStep = 1f)
    public float x = 0f;

    @Expose @ConfigOption(name = "Y", desc = "Vertical offset above the active inventory panel.") @ConfigEditorSlider(minValue = 0f, maxValue = 300f, minStep = 1f)
    public float y = 8f;

    @Expose @ConfigOption(name = "Centered", desc = "Center the search bar within the active inventory panel.") @ConfigEditorBoolean
    public boolean centered = true;

    @Expose @ConfigOption(name = "Persist Search Text", desc = "Keep the main search query between supported inventories.") @ConfigEditorBoolean
    public boolean persistSearchText = true;

    @Expose @ConfigOption(name = "Persist Item List Search", desc = "Keep the item-list search query between supported GUIs.") @ConfigEditorBoolean
    public boolean persistItemListSearch = false;

    @Expose @ConfigOption(name = "Persist Storage Search", desc = "Keep the storage search query between storage pages and vaults.") @ConfigEditorBoolean
    public boolean persistStorageSearch = true;

    @Expose public String searchText = "";
    @Expose public String itemListSearchText = "";
    @Expose public String storageSearchText = "";
    @Expose public String lastCalculationInput = "";
    @Expose public String lastCalculationResult = "";

    @Expose @ConfigOption(name = "Copy Calculator Result on Enter", desc = "Copy a valid calculator result to the system clipboard when Enter is pressed.") @ConfigEditorBoolean
    public boolean calcEnterCopyResult = true;

    @Expose @ConfigOption(name = "Clear Calculator on Enter", desc = "Clear the calculator expression after Enter when a result was produced.") @ConfigEditorBoolean
    public boolean calcEnterClearText = true;

    @Expose @ConfigOption(name = "Placeholder", desc = "Text shown when the field is empty.") @ConfigEditorText
    public String placeholder = "Search...";

    @Expose @ConfigOption(name = "Background", desc = "Search bar background color.") @ConfigEditorColour
    public String background = "0:0:0:0:160";

    @Expose @ConfigOption(name = "Border", desc = "Search bar border color.") @ConfigEditorColour
    public String border = "0:255:255:255:180";

    @Expose @ConfigOption(name = "Text Color", desc = "Search text color.") @ConfigEditorColour
    public String textColor = "0:255:255:255:255";
}
