package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.QualityOfLife;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.regex.Pattern;

@RegisterEvents
public final class ChatCleaner implements ClientModInitializer {
    private static final String RANKS = "(?:\\[[\\w+ -]+]\\h*)*";
    private static final String PLAYER = "[A-Za-z0-9_]{1,16}";
    private static final Pattern PLAYER_CHAT = Pattern.compile(
            "(?:(?:Guild|Party|Co-?op|Officer)\\h*>\\h*|(?:From|To)\\h+)?" + RANKS
                    + "(?:" + PLAYER + ":\\h*|<" + PLAYER + ">\\h*)(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern RANK_SALE = Pattern.compile("\\bselling\\b.*\\branks?\\b",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern INFO_HEADER = Pattern.compile(
            "\\[(?:WATCHDOG ANNOUNCEMENT|PLAYER INFORMATION)](?:\\s.*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FRIEND_JOIN_OR_LEAVE = Pattern.compile(
            "Friend\\h*>\\h+" + RANKS + PLAYER + "\\h+(?:joined|left)(?:\\h+the\\h+server)?[.!]?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ZOMBIE_DROP = Pattern.compile(
            "RARE DROP!\\h+(?:Poisonous Potato|Potato|Carrot)(?:[.!]|\\h*[(\\[].*|\\h*\\+\\d.*)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GAME_INVITE = Pattern.compile(
            "(?:" + RANKS + PLAYER + "\\h+)?invites you to play\\h+\\S.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROMO_TEXT = Pattern.compile(
            "\\bUse\\h+/(?:help|report|streams)\\b"
                    + "|\\bis the best rank for price and benefits\\b"
                    + "|\\bProtect yourself from\\h+scams\\b"
                    + "|\\bSpotted a cheater\\b"
                    + "|\\bA special\\h+discount\\b"
                    + "|\\bis streaming\\h+on the server\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PROMO_BUTTON = Pattern.compile("\\[(?:VISIT|OPEN|GET|JOIN)\\b[^\\]\\n]*]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSTYLED_PROMO_BUTTON = Pattern.compile(
            "\\[(?:VISIT|OPEN|GET|JOIN)\\h+[^\\]\\n]*\\b(?:SHOP|STORE|DISCORD|WEBSITE|RANKS?|SOCIALS?)\\b[^\\]\\n]*]",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                overlay || !active() || !hideGameMessage(ClientConfig.configuration().qol, message));
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, receivedAt) ->
                !active() || !ClientConfig.configuration().qol.qolDisableSellingRanks
                        || !isRankSale(Formatting.strip((signed == null ? message : signed.getContent()).getString())));
    }

    private static boolean active() {
        return SkyblockData.isSkyblock() && MinecraftClient.getInstance().player != null;
    }

    static boolean hideGameMessage(QualityOfLife settings, Text message) {
        String plain = Formatting.strip(message.getString()).strip();
        // Fakepixel can relay player chat in system packets on translated connections.
        var player = PLAYER_CHAT.matcher(plain);
        if (player.matches()) return settings.qolDisableSellingRanks && isRankSale(player.group(1));

        List<String> lines = plain.lines().map(String::strip).toList();
        if (lines.stream().anyMatch(line -> GAME_INVITE.matcher(line).matches())) return settings.qolDisableGameInvites;
        if (lines.stream().anyMatch(line -> INFO_HEADER.matcher(line).matches())) return settings.qolDisableWatchdogInfo;
        if (lines.stream().anyMatch(line -> FRIEND_JOIN_OR_LEAVE.matcher(line).matches())) return settings.qolDisableFriendJoin;
        if (lines.stream().anyMatch(line -> ZOMBIE_DROP.matcher(line).matches())) return settings.qolDisableZombieRareDrops;

        return settings.qolDisableServerAnnouncements && (lines.stream().anyMatch(line ->
                PROMO_TEXT.matcher(line).find() || UNSTYLED_PROMO_BUTTON.matcher(line).matches())
                || ((plain.endsWith("?") || PROMO_BUTTON.matcher(plain).find()) && hasStyledPromoMarker(message)));
    }

    private static boolean isRankSale(String text) {
        return RANK_SALE.matcher(text).find();
    }

    private static boolean hasStyledPromoMarker(Text message) {
        int gray = Formatting.GRAY.getColorValue();
        int darkGray = Formatting.DARK_GRAY.getColorValue();
        StringBuilder grayText = new StringBuilder();
        boolean[] trailingQuestion = {false};
        TextVisitFactory.visitFormatted(message, Style.EMPTY, (index, style, codePoint) -> {
            int color = style.getColor() == null ? -1 : style.getColor().getRgb();
            if (color == gray) grayText.appendCodePoint(codePoint);
            else if (!grayText.isEmpty() && grayText.charAt(grayText.length() - 1) != '\n') grayText.append('\n');

            if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)) {
                trailingQuestion[0] = codePoint == '?' && color == darkGray;
            }
            return true;
        });
        return trailingQuestion[0] || PROMO_BUTTON.matcher(grayText).find();
    }
}
