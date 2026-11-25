package com.nef.notenoughfakepixel.features.skyblock.qol.shortcuts;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;

@RegisterEvents
public class PetsShortcut extends KeyShortcut {

    @Override
    public boolean getConfigOption() {
        return Config.feature.pets.qolShortcutPets;
    }

    @Override
    public int getKeyBind() {
        return Config.feature.pets.qolPetsKey;
    }

    @Override
    public String getCommand() {
        return "/pets";
    }

    @Override
    public String getMenuTitle() {
        return "Pets";
    }


}
