package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public class HellionShieldFilter {

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Config.feature.slayer.infernoDemonlord.slayerHellionShieldFilter) return;
        if (!SkyblockData.getCurrentLocation().isCrimson()) return;

        String msg = event.message.getUnformattedText();
        if (msg.contains("Your hit was reduced by Hellion Shield!")
                || msg.contains("attunement on your dagger!")) {
            event.setCanceled(true);
        }
    }
}

