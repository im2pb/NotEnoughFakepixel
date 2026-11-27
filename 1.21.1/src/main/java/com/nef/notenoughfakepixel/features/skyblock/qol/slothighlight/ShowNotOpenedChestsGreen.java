package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;

@RegisterEvents
public final class ShowNotOpenedChestsGreen extends HightlightSlot {
    public ShowNotOpenedChestsGreen() {
        super("Croesus", "No Chests Opened!");
    }

    @Override
    protected boolean enabled(Configuration config) {
        return config.dungeons.general.dungeonsShowOpenedChests;
    }
}
