/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.fishing;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Area;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Displays a title for Trophy Fish catches without changing the original chat message. */
@RegisterEvents
public class TrophyFishNotifier {
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event == null || event.type != 1 || !Config.feature.fishing.fishingTrophyFish) return;
        Area area = SkyblockData.getCurrentArea();
        if (area != Area.CRIMSON && area != Area.CRIMSON_FIELDS && area != Area.SCARELTON
                && area != Area.ASHFANG && area != Area.VOLCANO) return;
        String message = event.message == null ? "" : event.message.getUnformattedText();
        if (!message.toLowerCase(java.util.Locale.ROOT).contains("trophy fish!")) return;
        String fish = message.replace("TROPHY FISH!", "").replace("You caught a ", "").trim();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.ingameGUI != null) mc.ingameGUI.displayTitle("TROPHY FISH", fish, 1, 20, 1);
    }
}
