/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import com.nef.notenoughfakepixel.config.gui.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;

public class SoundUtils {

    public static void playSound(int[] cords, String sound, float volume, float pitch) {
        playGlobalSound(sound, volume, pitch);
    }

    public static void playSound(BlockPos pos, String sound, float volume, float pitch) {
        playGlobalSound(sound, volume, pitch);
    }

    public static void playSound(int x, int y, int z, String sound, float volume, float pitch) {
        playGlobalSound(sound, volume, pitch);
    }

    public static void playGlobalSound(String sound, float volume, float pitch) {
        if (!Config.feature.misc.enableSounds || sound == null || sound.trim().isEmpty()) return;

        final ResourceLocation resource;
        try {
            resource = new ResourceLocation(sound.trim());
        } catch (IllegalArgumentException ignored) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        mc.addScheduledTask(() -> {
            if (mc.theWorld == null || mc.thePlayer == null) return;
            mc.theWorld.playSound(
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ,
                    resource.toString(), volume, pitch, false
            );
        });
    }
}
