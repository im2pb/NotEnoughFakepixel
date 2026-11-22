/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class HellionShieldFilter {
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Config.feature.slayer.slayerHellionShieldFilter || event == null || event.message == null) return;
        if (!SkyblockData.getCurrentLocation().isCrimson()) return;
        String message = event.message.getUnformattedText();
        if (message.contains("Your hit was reduced by Hellion Shield!")
                || message.contains("attunement on your dagger!")) {
            event.setCanceled(true);
        }
    }
}
