package com.nef.notenoughfakepixel.features.skyblock.fishing;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.regex.Pattern;

@RegisterEvents
public final class FishingCatchNotifications implements ClientModInitializer {
    private static final List<LegendaryCatch> LEGENDARY_CATCHES = List.of(
            new LegendaryCatch("The Sea Emperor arises", "The Sea Emperor"),
            new LegendaryCatch("The Water Hydra has come", "Water Hydra"),
            new LegendaryCatch("An Abyssal Miner breaks", "Abyssal Miner"),
            new LegendaryCatch("The spirit of a long lost Phantom ", "Phantom Fisher"),
            new LegendaryCatch("This can't be! The manifestation of death himself!", "Grim Reaper"),
            new LegendaryCatch("WOAH! A Plhlegblast appeared.", "Plhlegblast"),
            new LegendaryCatch("What is this creature!?", "Yeti"),
            new LegendaryCatch("A Reindrake forms from the depths.", "Reindrake"),
            new LegendaryCatch("Hide no longer, a Great White Shark", "Great White Shark"),
            new LegendaryCatch("You hear a massive rumble as", "Thunder"),
            new LegendaryCatch("You have angered a legendary creature.", "Lord Jawbus")
    );
    private static final Pattern TROPHY_CATCH = Pattern.compile("TROPHY FISH!\\s+You caught an?\\s+(\\S.*)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
    }

    private void onGameMessage(Text message, boolean overlay) {
        if (overlay || !SkyblockData.isSkyblock()) return;
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;

        var settings = ClientConfig.configuration().fishing.notifications;
        boolean trophyEnabled = settings.trophyFish && SkyblockData.currentLocation().isCrimson();
        if (!settings.legendaryCreatures && !trophyEnabled) return;
        for (String raw : message.getString().lines().toList()) {
            String line = Formatting.strip(raw).strip();
            String legendary = settings.legendaryCreatures ? legendaryCatch(line) : null;
            if (legendary != null) {
                show(client, Text.literal("Legendary Catch!").formatted(Formatting.GOLD), legendary, 2, 70);
                return;
            }

            String trophy = trophyEnabled ? trophyCatch(line) : null;
            if (trophy != null) {
                show(client, Text.literal("TROPHY FISH"), trophy, 1, 20);
                return;
            }
        }
    }

    static String legendaryCatch(String line) {
        for (var catchType : LEGENDARY_CATCHES) {
            String prefix = catchType.prefix();
            if (line.startsWith(prefix) && (line.length() == prefix.length()
                    || !Character.isLetterOrDigit(prefix.charAt(prefix.length() - 1))
                    || !Character.isLetterOrDigit(line.charAt(prefix.length())))) return catchType.creature();
        }
        return null;
    }

    static String trophyCatch(String line) {
        var match = TROPHY_CATCH.matcher(line);
        return match.matches() ? match.group(1) : null;
    }

    private static void show(MinecraftClient client, Text title, String subtitle, int fade, int duration) {
        client.inGameHud.setTitleTicks(fade, duration, fade);
        client.inGameHud.setSubtitle(Text.literal(subtitle));
        client.inGameHud.setTitle(title);
    }

    private record LegendaryCatch(String prefix, String creature) {
    }
}
