package com.nef.notenoughfakepixel.features.skyblock.qol.shortcuts;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;

@RegisterEvents
public class EquipmentShortcut extends KeyShortcut{

    @Override
    public boolean getConfigOption() {
        return Config.feature.qol.shortcuts.qolShortcutEq;
    }

    @Override
    public int getKeyBind() {
        return Config.feature.qol.shortcuts.qolEqKey;
    }

    @Override
    public String getCommand() {
        return "/equipment";
    }

    @Override
    public String getMenuTitle() {
        return "Your Equipment";
    }
}

