package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;

@RegisterEvents
public final class JacobRewardsRed extends HightlightSlot {
    public JacobRewardsRed() {
        super("Your contests", "Rewards claimed!");
    }

    @Override
    protected boolean enabled(Configuration config) {
        return config.qol.qolShowJacobRewards;
    }

    @Override
    protected int color(Configuration config) {
        return RED;
    }
}
