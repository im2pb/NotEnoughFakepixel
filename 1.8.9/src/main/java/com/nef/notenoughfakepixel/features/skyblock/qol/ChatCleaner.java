package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Pattern;

@RegisterEvents
public class ChatCleaner {

    private final Pattern sellingRankPattern = Pattern.compile("(?<rank>\\[[A-Za-z0-9_+]+\\] )?(?<username>\\w+:) (?<message>.*\\bselling\\b.*\\brank(s)?\\b.*)");
    private final Pattern watchdogPattern = Pattern.compile("§4\\[WATCHDOG ANNOUNCEMENT]\n");
    private final Pattern infoPattern = Pattern.compile("§b\\[PLAYER INFORMATION]\n");
    private final Pattern friendJoinPattern = Pattern.compile("§aFriend > ");
    private final Pattern potatoDropPattern = Pattern.compile("§r§6§lRARE DROP! §r§fPotato§r§b");
    private final Pattern poisonousPotatoDropPattern = Pattern.compile("§r§6§lRARE DROP! §r§fPoisonous Potato§r§b");
    private final Pattern carrotDropPattern = Pattern.compile("§r§6§lRARE DROP! §r§fCarrot§r§b");
    private final Pattern gameInvitePattern = Pattern.compile("§finvites you to play §6", Pattern.DOTALL);
    private final Pattern serverAnnouncementPattern = Pattern.compile(
            "§7\\[(?:VISIT|OPEN|GET|JOIN)\\b[^\\]]*\\]"
                    + "|Use §6/(?:help|report|streams)\\b"
                    + "|§fis the best rank for price and benefits"
                    + "|Protect yourself from §6scams"
                    + "|§fSpotted a cheater"
                    + "|A special §6discount"
                    + "|§fis streaming §fon the server"
                    + "|§8\\?(?:§r)?\\s*$",
            Pattern.DOTALL);

    @SubscribeEvent
    public void onChatRecieve(ClientChatReceivedEvent event) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;
        if (ChatUtils.middleBar.matcher(event.message.getFormattedText()).matches()) return;
        cancelMessage(Config.feature.qol.qolDisableSellingRanks, event, sellingRankPattern);
        cancelMessage(Config.feature.qol.qolDisableWatchdogInfo, event, watchdogPattern, true);
        cancelMessage(Config.feature.qol.qolDisableWatchdogInfo, event, infoPattern, true);
        cancelMessage(Config.feature.qol.qolDisableFriendJoin, event, friendJoinPattern, true);
        cancelMessage(Config.feature.qol.qolDisableZombieRareDrops, event, potatoDropPattern, true);
        cancelMessage(Config.feature.qol.qolDisableZombieRareDrops, event, poisonousPotatoDropPattern, true);
        cancelMessage(Config.feature.qol.qolDisableZombieRareDrops, event, carrotDropPattern, true);

        // Check game invites first so they don't get swallowed by the broader announcement match.
        String formatted = event.message.getFormattedText();
        if (gameInvitePattern.matcher(formatted).find()) {
            if (Config.feature.qol.qolDisableGameInvites) event.setCanceled(true);
        } else if (Config.feature.qol.qolDisableServerAnnouncements
                && serverAnnouncementPattern.matcher(formatted).find()) {
            event.setCanceled(true);
        }
    }

    private void cancelMessage(boolean option, ClientChatReceivedEvent e, Pattern pattern, boolean formatted) {
        if (!option) return;
        String message = e.message.getUnformattedText();
        if (formatted) message = e.message.getFormattedText();

        if (pattern.matcher(message).find() || pattern.matcher(message).matches()) {
            e.setCanceled(true);
        }
    }

    private void cancelMessage(boolean option, ClientChatReceivedEvent e, Pattern pattern) {
        cancelMessage(option, e, pattern, false);
    }

}
