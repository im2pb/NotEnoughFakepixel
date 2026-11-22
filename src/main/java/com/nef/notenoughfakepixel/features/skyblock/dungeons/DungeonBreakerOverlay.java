/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.utils.SkyblockGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class DungeonBreakerOverlay implements GatedListener {

    @Override
    public boolean isListenerActive() {
        return Config.feature.dungeonSystems.breakerCharges && SkyblockGate.isFakepixelDungeon();
    }

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Pattern CHARGES = Pattern.compile("(?i)charges\\s*[:=]\\s*(\\d+)\\s*/\\s*(\\d+)");
    private static String cachedItemKey = "";
    private static int cachedCurrent = -1;
    private static int cachedMax = -1;

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!Config.feature.dungeonSystems.breakerCharges || !SkyblockGate.isFakepixelDungeon() || MC.thePlayer == null) return;
        ItemStack held = MC.thePlayer.getHeldItem();
        updateCache(held);
        if (cachedCurrent < 0 || cachedMax <= 0) return;

        ScaledResolution sr = new ScaledResolution(MC);
        int x = Math.max(0, Math.min(sr.getScaledWidth() - 90, Math.round(Config.feature.dungeonSystems.breakerX)));
        int y = Math.max(0, Math.min(sr.getScaledHeight() - 16, Math.round(Config.feature.dungeonSystems.breakerY)));
        float ratio = Math.max(0f, Math.min(1f, cachedCurrent / (float) cachedMax));
        int textColor = ratio > 0.5f ? 0xFF55FF55 : ratio > 0.2f ? 0xFFFFFF55 : 0xFFFF5555;
        Gui.drawRect(x - 3, y - 3, x + 84, y + 13, 0x99000000);
        MC.fontRendererObj.drawStringWithShadow("Charges: " + cachedCurrent + "/" + cachedMax, x, y, textColor);
    }

    private static void updateCache(ItemStack held) {
        if (held == null) { cachedItemKey = ""; cachedCurrent = cachedMax = -1; return; }
        String key = ItemUtils.getInternalName(held) + "|" + held.getDisplayName() + "|" + held.stackSize;
        if (key.equals(cachedItemKey)) return;
        cachedItemKey = key;
        cachedCurrent = cachedMax = -1;
        List<String> lore = ItemUtils.getLoreLines(held);
        for (String line : lore) {
            Matcher matcher = CHARGES.matcher(line == null ? "" : line);
            if (!matcher.find()) continue;
            try {
                cachedCurrent = Integer.parseInt(matcher.group(1));
                cachedMax = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException ignored) {
                cachedCurrent = cachedMax = -1;
            }
            break;
        }
    }
}
