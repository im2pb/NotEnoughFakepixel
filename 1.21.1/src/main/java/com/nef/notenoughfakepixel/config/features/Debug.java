package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.*;

public class Debug {

    @Expose
    @ConfigOption(name = "Debug Logs", desc = "Enable debug mode for logging")
    @ConfigEditorBoolean
    public boolean debug = false;

    @Expose
    @ConfigOption(name = "Sound Debug", desc = "Show Sounds debug")
    @ConfigEditorBoolean
    public boolean showSounds = false;

    @Expose
    @ConfigOption(name = "Force Pojav", desc = "Force Pojav detection in the mod")
    @ConfigEditorBoolean
    public boolean forcePojav = false;

    @Expose
    @ConfigOption(name = "Enable out of fakepixel", desc = "Enables mod out of fakepixel server")
    @ConfigEditorBoolean
    public boolean enableOutOfFakepixel = false;

    @Expose
    @ConfigOption(name = "Copy NBT", desc = "Allows copying mouse item NBT into clipboard \u00a7c[R CTRL]\u00a77")
    @ConfigEditorBoolean
    public boolean debugCopyNbt = false;

}
