package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ConfigColor;

@RegisterEvents
public final class ShowCurrentPet extends HightlightSlot {
    private final ConfigColor petColor = new ConfigColor();

    public ShowCurrentPet() {
        super("Pets", "Click to despawn!");
    }

    @Override
    protected boolean enabled(Configuration config) {
        return config.pets.qolShowPetEquipped;
    }

    @Override
    protected int color(Configuration config) {
        return petColor.resolve(config.pets.qolPetEquippedColor);
    }

    @Override
    protected boolean highlightOnlyFirst() {
        return true;
    }
}
