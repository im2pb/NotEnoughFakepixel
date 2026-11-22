/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.SkyblockGate;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterEvents
public final class HideBlessing {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChat(ClientChatReceivedEvent event) {
        if (!Config.feature.dungeons.dungeonsHideBlessing || !SkyblockGate.isFakepixelDungeon() || event == null || event.message == null) return;
        if (event.type != 0) return;
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());
        if (message == null) return;
        String clean = message.trim().toLowerCase(java.util.Locale.ROOT);
        if (clean.contains("blessing")) event.setCanceled(true);
    }
}
