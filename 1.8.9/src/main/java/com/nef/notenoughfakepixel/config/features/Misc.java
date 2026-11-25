package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Misc {

    @Expose
    @ConfigOption(name = "Scrollable Tooltips", desc = "Enable scrollable tooltips.")
    @ConfigEditorBoolean
    public boolean qolScrollableTooltips = true;

    @Expose
    @ConfigOption(name = "Copy Chat Message", desc = "Enable copying chat messages.")
    @ConfigEditorBoolean
    public boolean qolCopyChatMsg = true;

    final String copyEmoji = Character.toString((char) Integer.parseInt("270D", 16));

    @Expose
    @ConfigOption(name = "Copy Chat Button", desc = "Button to copy chat.")
    @ConfigEditorDropdown(values = {
            "Legacy Emoji",
            "[COPY]"
    })
    public String copyChatString = "Legacy Emoji";


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
    @ConfigEditorButton(runnableId = 11, buttonText = "Open")
    public String termSim = "";

}

