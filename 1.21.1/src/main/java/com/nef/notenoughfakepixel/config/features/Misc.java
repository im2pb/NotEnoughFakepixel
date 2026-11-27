package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class Misc {

    @Expose
    @ConfigOption(name = "Scrollable Tooltips", desc = "Scroll tall item tooltips with the mouse wheel or arrow keys")
    @ConfigEditorBoolean
    public boolean qolScrollableTooltips = true;

    @Expose
    @ConfigOption(name = "Copy Chat Message", desc = "Add a clipboard button to chat messages")
    @ConfigEditorBoolean
    public boolean qolCopyChatMsg = true;

    @Expose
    @ConfigOption(name = "Copy Chat Button", desc = "Button to copy chat")
    @ConfigEditorDropdown(values = {
            "Legacy Emoji",
            "[COPY]"
    })
    public String copyChatString = "Legacy Emoji";


    @Expose
    @ConfigOption(name = "Always Sprint", desc = "Sprint automatically whenever normal movement allows it")
    @ConfigEditorBoolean
    public boolean qolAlwaysSprint = true;

    @Expose
    @ConfigOption(name = "Sounds", desc = "Enable or disable sounds")
    @ConfigEditorBoolean
    public boolean enableSounds = true;

}

