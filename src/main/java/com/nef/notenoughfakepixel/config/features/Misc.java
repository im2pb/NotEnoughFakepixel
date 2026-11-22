/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorBoolean;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorButton;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorSlider;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigOption;

public class Misc {

    @Expose
    @ConfigOption(name = "Scrollable Tooltips", desc = "Enable scrollable tooltips.")
    @ConfigEditorBoolean
    public boolean qolScrollableTooltips = true;

    @Expose
    @ConfigOption(name = "Copy Chat Message", desc = "Hold Shift in chat to enter copy mode, then click a line to copy it. Add Ctrl to copy it with its colors.")
    @ConfigEditorBoolean
    public boolean qolCopyChatMsg = true;

    @Expose
    @ConfigOption(name = "Copy Chat Icon", desc = "In copy mode (Shift held), highlights the line under your mouse and shows a copy icon next to it.")
    @ConfigEditorBoolean
    public boolean qolCopyChatIcon = true;

    @Expose
    @ConfigOption(name = "Chat Length", desc = "How many chat lines you can scroll back through. Normal Minecraft only keeps 100.")
    @ConfigEditorSlider(minValue = 100f, maxValue = 5000f, minStep = 100f)
    public int qolChatHistoryLines = 1000;

    @Expose
    @ConfigOption(name = "Always Sprint", desc = "Always sprint.")
    @ConfigEditorBoolean
    public boolean qolAlwaysSprint = true;

    @Expose
    @ConfigOption(name = "Sounds", desc = "Enable or disable sounds.")
    @ConfigEditorBoolean
    public boolean enableSounds = true;

    @Expose
    @ConfigOption(name = "Termsim", desc = "Termsim")
    @ConfigEditorButton(runnableId = "termSim", buttonText = "Open")
    public String termSim = "";

}
