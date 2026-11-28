package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SlayerAlerts implements ClientModInitializer {
    private static final long ECHO_WINDOW_MILLIS = 750;

    @RegisterInstance
    public static final SlayerAlerts INSTANCE = new SlayerAlerts();

    private final Map<BlockPos, Long> spawns = new HashMap<>();

    private SlayerAlerts() {}

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> spawns.clear());
    }

    public void observeSound(MinecraftClient client, PlaySoundS2CPacket packet) {
        if (client.player == null || !SkyblockData.isSkyblock() || !spawnSound(packet)) return;
        var settings = ClientConfig.configuration().slayer.minibossSettings;
        if (!settings.slayerMinibossTitle && !settings.slayerMinibossSound) return;

        long now = Util.getMeasuringTimeMs();
        spawns.values().removeIf(last -> now - last > ECHO_WINDOW_MILLIS);
        var origin = BlockPos.ofFloored(packet.getX(), packet.getY(), packet.getZ());
        // Fakepixel sends a burst of matching sounds at the same position.
        if (spawns.put(origin, now) != null) return;

        if (settings.slayerMinibossTitle) showTitle(client, "MINIBOSS!");
        if (settings.slayerMinibossSound) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f));
        }
    }

    private static boolean spawnSound(PlaySoundS2CPacket packet) {
        return packet.getSound().value().getId().equals(SoundEvents.ENTITY_GENERIC_EXPLODE.value().getId())
                && Math.abs(packet.getVolume() - 0.6f) < 0.001f && Math.abs(packet.getPitch() - 9f / 7) < 0.001f;
    }

    public static void completed(double seconds) {
        var client = MinecraftClient.getInstance();
        if (client.player == null || !ClientConfig.configuration().slayer.bossSettings.slayerBossTimer) return;
        client.inGameHud.getChatHud().addMessage(
                Text.literal(String.format(Locale.ROOT, "[NEF] Boss took %.3f seconds!", seconds)).formatted(Formatting.GREEN));
    }

    static void showTitle(MinecraftClient client, String title) {
        client.inGameHud.setTitleTicks(0, 20, 3);
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitle(Text.literal(title).formatted(Formatting.RED));
    }
}
