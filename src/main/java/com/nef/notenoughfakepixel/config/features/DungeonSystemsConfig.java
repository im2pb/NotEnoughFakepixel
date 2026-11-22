/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.*;
import org.lwjgl.input.Keyboard;

public class DungeonSystemsConfig {
    @Expose @ConfigOption(name = "Dungeon Context", desc = "Track dungeon floor, phase and run timestamps.") @ConfigEditorBoolean
    public boolean context = true;

    @Expose @ConfigOption(name = "Personal Best Tracker", desc = "Store personal dungeon run and phase best times.") @ConfigEditorBoolean
    public boolean personalBest = true;

    @Expose @ConfigOption(name = "Dungeon Breaker Charges", desc = "Show charges for dungeon breaker-style tools when available in the item lore.") @ConfigEditorBoolean
    public boolean breakerCharges = true;

    @Expose @ConfigOption(name = "Breaker X", desc = "Horizontal position of the breaker charge overlay.") @ConfigEditorSlider(minValue = 0f, maxValue = 600f, minStep = 1f)
    public float breakerX = 10f;

    @Expose @ConfigOption(name = "Breaker Y", desc = "Vertical position of the breaker charge overlay.") @ConfigEditorSlider(minValue = 0f, maxValue = 400f, minStep = 1f)
    public float breakerY = 10f;

    @Expose @ConfigOption(name = "Map Player Names (Hold)", desc = "Hold this key to show player names over the vanilla map markers.") @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LCONTROL)
    public int mapPlayerNamesKey = Keyboard.KEY_LCONTROL;

    @Expose @ConfigOption(name = "Always Show Map Player Names", desc = "Always show player names; you do not need to hold Ctrl.") @ConfigEditorBoolean
    public boolean alwaysShowMapPlayerNames = false;

    @Expose @ConfigOption(name = "Reward Animation", desc = "Animate the currently visible dungeon reward contents.") @ConfigEditorBoolean
    public boolean rewardAnimation = true;

    @Expose @ConfigOption(name = "Reward Animation Time", desc = "Seconds used by the reward animation.") @ConfigEditorSlider(minValue = 0.5f, maxValue = 5f, minStep = 0.1f)
    public float rewardAnimationTime = 2.2f;

    @Expose @ConfigOption(name = "Reward Animation Text Scale", desc = "Scale of the reward animation text.") @ConfigEditorSlider(minValue = 0.5f, maxValue = 3f, minStep = 0.1f)
    public float rewardAnimationTextScale = 1f;

    @Expose @ConfigOption(name = "Reward Animation Item Names", desc = "Show item names in the reward animation.") @ConfigEditorBoolean
    public boolean rewardAnimationShowItemNames = true;

}
