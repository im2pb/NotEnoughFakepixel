package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@RegisterEvents
public final class DungeonAlerts implements ClientModInitializer {
    private static final String BLOOD_READY = "[BOSS] The Watcher: That will be enough for now.";

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register(DungeonAlerts::onGameMessage);
        ClientTickEvents.END_CLIENT_TICK.register(DungeonAlerts::closeSecretChest);
    }

    private static void onGameMessage(Text message, boolean overlay) {
        if (overlay || !DungeonContext.active() || !ClientConfig.configuration().dungeons.general.dungeonsBloodReady
                || !Formatting.strip(message.getString()).strip().startsWith(BLOOD_READY)) return;

        var client = MinecraftClient.getInstance();
        if (client.player == null) return;

        client.inGameHud.setTitleTicks(2, 36, 2);
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitle(Text.literal("BLOOD READY!").formatted(Formatting.RED));
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F));
    }

    private static void closeSecretChest(MinecraftClient client) {
        if (client.player == null || !(client.currentScreen instanceof GenericContainerScreen chest)
                || chest.getScreenHandler() != client.player.currentScreenHandler
                || !DungeonContext.active() || !ClientConfig.configuration().dungeons.general.dungeonsAutoCloseChests) return;

        if (Formatting.strip(chest.getTitle().getString()).strip().equals("Secret Chest")) chest.close();
    }
}
