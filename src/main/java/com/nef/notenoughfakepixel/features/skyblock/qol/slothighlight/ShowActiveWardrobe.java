/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;

@RegisterEvents
public class ShowActiveWardrobe extends HightlightSlot {

    @Override
    public String getLoreLine() {
        return "";
    }

    @Override
    public String getName() {
        return "Equipped";
    }

    @Override
    public String getContainerName() {
        return "Wardrobe";
    }

    @Override
    public boolean getConfigOption() {
        return Config.feature.qol.qolShowWardrobeSlot;
    }

}
