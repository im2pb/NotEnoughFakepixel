package com.nef.notenoughfakepixel.config.features;

import com.nef.notenoughfakepixel.env.VersionChecker;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import net.minecraft.util.Util;

public final class About {
    @ConfigOption(name = "Fabric Version", desc = "Show the installed version and repository update information in chat")
    @ConfigEditorButton(buttonText = "Check")
    public transient Runnable version = VersionChecker::reportVersion;

    @ConfigOption(name = "Discord", desc = "Join the NotEnoughFakepixel Discord server")
    @ConfigEditorButton(buttonText = "Open")
    public transient Runnable discord = VersionChecker::openDiscord;

    @ConfigOption(name = "GitHub", desc = "View the NotEnoughFakepixel source code")
    @ConfigEditorButton(buttonText = "Open")
    public transient Runnable github = () -> Util.getOperatingSystem().open("https://github.com/davidbelesp/NotEnoughFakepixel");

    @ConfigOption(name = "MoulConfig", desc = "Configuration GUI library by NotEnoughUpdates")
    @ConfigEditorButton(buttonText = "Open")
    public transient Runnable moulConfig = () -> Util.getOperatingSystem().open("https://notenoughupdates.org/MoulConfig/");
}
