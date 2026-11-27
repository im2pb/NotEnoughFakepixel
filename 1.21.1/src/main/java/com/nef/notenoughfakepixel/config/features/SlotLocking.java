package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.*;
import org.lwjgl.glfw.GLFW;

public class SlotLocking {

    @Expose
    @ConfigOption(
            name = "\u00A7cWarning",
            desc = "Make sure you have SBA's and NEU's locked slots off before you turn NEF's on"
    )
    @ConfigEditorBoolean
    public boolean slotLockWarning = false;

    @Expose
    @ConfigOption(
            name = "Enable Slot Locking",
            desc = "Allows you to lock slots and create slot bindings"
    )
    @ConfigEditorBoolean
    public boolean enableSlotLocking = false;

    @Expose
    @ConfigOption(
            name = "Enable Slot Binding",
            desc = "Allows you to create slot bindings\nNote: \"Enable Slot Locking\" must be on"
    )
    @ConfigEditorBoolean
    public boolean enableSlotBinding = true;

    @Expose
    @ConfigOption(
            name = "Don't Drop Bound Slots",
            desc = "Slot bindings also act as locked slots (prevents dropping / moving in inventory)"
    )
    @ConfigEditorBoolean
    public boolean bindingAlsoLocks = false;

    @Expose
    @ConfigOption(
            name = "Slot Lock Key",
            desc = "Tap over a slot to lock or unlock it. Hold over an inventory or armor slot, move to a hotbar slot, and release to bind. Shift-click either slot to swap."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_L)
    public int slotLockKey = GLFW.GLFW_KEY_L;

    @Expose
    @ConfigOption(
            name = "Lock Slots in Trade",
            desc = "Prevents trading locked items in the custom trade windows"
    )
    @ConfigEditorBoolean
    public boolean lockSlotsInTrade = true;

    @Expose
    @ConfigOption(
            name = "Disable Locking in Storage",
            desc = "Disables slot locking and binding in the storage gui"
    )
    @ConfigEditorBoolean
    public boolean disableInStorage = false;

    @Expose
    @ConfigOption(
            name = "Item Swap drop delay",
            desc = "Set the delay between swapping to another item and being able to drop it.\n" +
                    "This is to fix a bug that allowed you to drop slot locked items"
    )
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 500,
            minStep = 5
    )
    public int slotLockSwapDelay = 100;

    @Expose
    @ConfigOption(
            name = "Slot Lock Sound",
            desc = "Play a ding when locking/unlocking slots"
    )
    @ConfigEditorBoolean
    public boolean slotLockSound = true;

    @Expose
    @ConfigOption(
            name = "Slot Lock Sound Vol",
            desc = "Set the volume of the ding sound"
    )
    @ConfigEditorSlider(
            minValue = 0,
            maxValue = 100,
            minStep = 1
    )
    public float slotLockSoundVol = 20;

    @ConfigOption(name = "Reset Locked Slots", desc = "Clear slot locks and bindings on the current SkyBlock profile")
    @ConfigEditorButton(buttonText = "Reset")
    public transient Runnable resetSlotLocking = com.nef.notenoughfakepixel.features.skyblock.slotlocking.SlotLocking::confirmReset;

}

