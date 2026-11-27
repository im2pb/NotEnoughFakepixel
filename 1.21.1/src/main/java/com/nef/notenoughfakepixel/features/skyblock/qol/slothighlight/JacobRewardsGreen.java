package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;

@RegisterEvents
public final class JacobRewardsGreen extends HightlightSlot {
    public JacobRewardsGreen() {
        super("Your contests", "Click to claim reward!");
    }

    @Override
    protected boolean enabled(Configuration config) {
        return config.qol.qolShowJacobRewards;
    }
}
