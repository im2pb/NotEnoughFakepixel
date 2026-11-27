package com.nef.notenoughfakepixel.features.skyblock.pets;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@RegisterEvents
public final class PetsShortcut implements ClientModInitializer {
    private boolean held;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || !client.isWindowFocused()) {
            held = false;
            return;
        }

        var settings = ClientConfig.configuration().pets;
        int key = settings.qolPetsKey;
        boolean down = key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST
                && InputUtil.isKeyPressed(client.getWindow().getHandle(), key);
        boolean pressed = down && !held;
        held = down;
        if (!pressed || !settings.qolShortcutPets || !SkyblockData.isSkyblock()) return;

        if (client.currentScreen == null) {
            client.player.networkHandler.sendChatCommand("pets");
        } else if (PetTracker.isPetsMenu(client.currentScreen)) {
            client.player.closeHandledScreen();
        }
    }
}
