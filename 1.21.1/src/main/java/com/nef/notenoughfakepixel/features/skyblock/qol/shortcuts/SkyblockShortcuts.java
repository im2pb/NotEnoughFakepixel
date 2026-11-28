package com.nef.notenoughfakepixel.features.skyblock.qol.shortcuts;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.QualityOfLife;
import com.nef.notenoughfakepixel.features.skyblock.pets.PetTracker;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public final class SkyblockShortcuts {
    private static final int FIRST_WARDROBE_BUTTON = 36;

    private SkyblockShortcuts() {}

    public static boolean handleKey(MinecraftClient client, int key, int action) {
        if ((action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) || key == GLFW.GLFW_KEY_UNKNOWN
                || !client.isWindowFocused() || !SkyblockData.isSkyblock() || client.player == null
                || client.getOverlay() != null) return false;

        boolean pressed = action == GLFW.GLFW_PRESS;
        var config = ClientConfig.configuration();
        if (client.currentScreen == null) {
            if (pressed) {
                String command = command(config, key);
                if (command != null) client.player.networkHandler.sendChatCommand(command);
            }
            return false;
        }

        if (!(client.currentScreen instanceof GenericContainerScreen menu)
                || client.player.currentScreenHandler != menu.getScreenHandler()) return false;

        var shortcuts = config.qol.shortcuts;
        String title = Formatting.strip(menu.getTitle().getString()).strip();
        boolean wardrobe = title.startsWith("Wardrobe");
        if ((wardrobe && shortcuts.qolShortcutWardrobe && key == shortcuts.qolWardrobeKey)
                || (title.startsWith("Your Equipment") && shortcuts.qolShortcutEq && key == shortcuts.qolEqKey)
                || (config.pets.qolShortcutPets && key == config.pets.qolPetsKey && PetTracker.isPetsTitle(title))) {
            if (pressed) client.player.closeHandledScreen();
            return true;
        }

        var handler = menu.getScreenHandler();
        if (!wardrobe || !shortcuts.qolShortcutSlotsWardrobe || handler.getRows() < 5
                || client.interactionManager == null) return false;

        int slot = wardrobeSlot(shortcuts, key);
        if (slot < 0) return false;

        if (pressed) {
            client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
        }
        return true;
    }

    private static String command(Configuration config, int key) {
        var shortcuts = config.qol.shortcuts;
        if (shortcuts.qolShortcutWardrobe && key == shortcuts.qolWardrobeKey) return "wardrobe";
        if (shortcuts.qolShortcutEq && key == shortcuts.qolEqKey) return "equipment";
        if (config.pets.qolShortcutPets && key == config.pets.qolPetsKey) return "pets";
        if (shortcuts.qolShortcutWarps) {
            if (key == shortcuts.qolShortcutWarpIs) return "warp is";
            if (key == shortcuts.qolShortcutWarpHub) return "warp hub";
            if (key == shortcuts.qolShortcutWarpDh) return "warp dh";
        }
        return null;
    }

    private static int wardrobeSlot(QualityOfLife.Shortcuts shortcuts, int key) {
        int[] keys = {shortcuts.qolWardrobeKey1, shortcuts.qolWardrobeKey2, shortcuts.qolWardrobeKey3,
                shortcuts.qolWardrobeKey4, shortcuts.qolWardrobeKey5, shortcuts.qolWardrobeKey6,
                shortcuts.qolWardrobeKey7, shortcuts.qolWardrobeKey8, shortcuts.qolWardrobeKey9};
        for (int index = 0; index < keys.length; index++) {
            if (keys[index] == key) return FIRST_WARDROBE_BUTTON + index;
        }
        return -1;
    }
}
