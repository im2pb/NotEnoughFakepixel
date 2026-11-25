package com.nef.notenoughfakepixel.config.features;

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.minecraft.util.Util;

public final class About {
    @ConfigOption(name = "Fabric Port", desc = "These settings are saved for the Fabric port. Gameplay features will be added separately")
    @ConfigEditorInfoText
    public transient String status = "";

    @ConfigOption(name = "Discord", desc = "Join the NotEnoughFakepixel Discord server")
    @ConfigEditorButton(buttonText = "Open")
    public transient Runnable discord = () -> Util.getOperatingSystem().open("https://discord.gg/xjFy4GQmbp");

    @ConfigOption(name = "GitHub", desc = "View the NotEnoughFakepixel source code")
    @ConfigEditorButton(buttonText = "Open")
    public transient Runnable github = () -> Util.getOperatingSystem().open("https://github.com/davidbelesp/NotEnoughFakepixel");

    @ConfigOption(name = "MoulConfig", desc = "Configuration GUI library by NotEnoughUpdates")
    @ConfigEditorButton(buttonText = "Open")
    public transient Runnable moulConfig = () -> Util.getOperatingSystem().open("https://notenoughupdates.org/MoulConfig/");
}
