package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;

@RegisterEvents
public final class ShowNotOpenedChestsRed extends HightlightSlot {
    public ShowNotOpenedChestsRed() {
        super("Croesus", "No more chests to open!");
    }

    @Override
    protected boolean enabled(Configuration config) {
        return config.dungeons.general.dungeonsShowOpenedChests;
    }

    @Override
    protected int color(Configuration config) {
        return RED;
    }
}
