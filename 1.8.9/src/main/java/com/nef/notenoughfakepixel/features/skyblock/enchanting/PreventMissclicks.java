package com.nef.notenoughfakepixel.features.skyblock.enchanting;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.SlotClickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class PreventMissclicks {
    @SubscribeEvent
    public void onClick(SlotClickEvent event) {
        if (Config.feature.experimentation.experimentationPreventMissclicks
                && (event.clickType == 0 || event.clickType == 1)
                && EnchantingSolvers.blocksClick(event.slot)) {
            event.cancel();
        }
    }
}
