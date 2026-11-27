package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;

@RegisterEvents
public final class ShowActiveWardrobe extends HightlightSlot {
    public ShowActiveWardrobe() {
        super("Wardrobe", "Equipped", "");
    }

    @Override
    protected boolean enabled(Configuration config) {
        return config.qol.qolShowWardrobeSlot;
    }
}
