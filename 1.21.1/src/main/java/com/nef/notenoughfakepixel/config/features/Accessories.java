package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import org.lwjgl.glfw.GLFW;

public class Accessories {

    @Expose
    @ConfigOption(name = "Enable Missing Accessories", desc = "Enable/Disable Missing accessories GUI")
    @ConfigEditorBoolean
    public boolean enable = true;

    @Expose
    @ConfigOption(name = "Show MP Estimation", desc = "Shows MP estimation in accessories GUI")
    @ConfigEditorBoolean
    public boolean showMpEstimation = true;

    @Expose
    @ConfigOption(name = "Show Missing Accessories List", desc = "Shows a list of missing accessories in accessories GUI")
    @ConfigEditorBoolean
    public boolean showMissingAccessoriesList = true;

    @Expose
    @ConfigOption(name = "Scroll Up Key", desc = "Keybind to scroll up in accessories GUI")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UP)
    public int accessoriesDataScrollUpKey = GLFW.GLFW_KEY_UP;

    @Expose
    @ConfigOption(name = "Scroll Down Key", desc = "Keybind to scroll down in accessories GUI")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_DOWN)
    public int accessoriesDataScrollDownKey = GLFW.GLFW_KEY_DOWN;

}

