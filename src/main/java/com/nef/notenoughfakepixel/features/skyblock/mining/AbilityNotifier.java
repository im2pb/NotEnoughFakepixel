/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import com.nef.notenoughfakepixel.features.skyblock.mining.service.MiningAbilityCooldownService;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class AbilityNotifier {

    private static final MiningAbilityCooldownService COOLDOWN = new MiningAbilityCooldownService();

    private static final String READY_MESSAGE = EnumChatFormatting.GREEN + "Ready";
    private static final Pattern ABILITY_PATTERN = Pattern.compile("You used your (.+) Pickaxe Ability!");

    @SubscribeEvent
    public void onChat(@NotNull ClientChatReceivedEvent e) {
        if (checkEssentials()) return;

        Matcher matcher = ABILITY_PATTERN.matcher(e.message.getUnformattedText());
        if (matcher.find()) {
            COOLDOWN.recordAbilityUse(System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (checkEssentials()) return;
        if (!Config.feature.mining.miningAbilityNotifier) return;
        if (!COOLDOWN.isReminderDue(System.currentTimeMillis())) return;

        reminder();
    }

    public static void reminder() {
        Minecraft.getMinecraft().ingameGUI.displayTitle(EnumChatFormatting.GOLD + "Mining Ability Ready", "", 2, 70, 2);
        COOLDOWN.markReminderDisplayed();
    }

    public static String cdSecondsRemaining() {
        long remaining = COOLDOWN.getRemainingMillis(System.currentTimeMillis());
        if (remaining <= 0L) return READY_MESSAGE;
        return EnumChatFormatting.RED + String.valueOf(remaining / 1000L) + "s";
    }

    /** Raw cooldown value for HUD data collection; zero means the ability is ready. */
    public static long cooldownRemainingMillis() {
        return COOLDOWN.getRemainingMillis(System.currentTimeMillis());
    }

    private static boolean checkEssentials() {
        return (Minecraft.getMinecraft().thePlayer == null) ||
                (!SkyblockData.getCurrentGamemode().isSkyblock()) ||
                ((!SkyblockData.getCurrentLocation().equals(Location.DWARVEN) && (!SkyblockData.getCurrentLocation().equals(Location.CRYSTAL_HOLLOWS))));
    }

}
