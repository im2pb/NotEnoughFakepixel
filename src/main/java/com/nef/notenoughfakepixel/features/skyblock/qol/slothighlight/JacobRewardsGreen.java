/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;

@RegisterEvents
public class JacobRewardsGreen extends HightlightSlot {

    @Override
    public String getLoreLine() {
        return "Click to claim reward!";
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getContainerName() {
        return "Your contests";
    }

    @Override
    public boolean getConfigOption() {
        return Config.feature.qol.qolShowJacobRewards;
    }

}
