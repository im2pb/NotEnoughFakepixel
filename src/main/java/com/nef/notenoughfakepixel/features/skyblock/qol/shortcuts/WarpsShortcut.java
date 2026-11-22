/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol.shortcuts;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.config.gui.core.config.KeybindHelper;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import java.util.HashSet;
import java.util.Set;

@RegisterEvents
public class WarpsShortcut implements GatedListener {

    @Override
    public boolean isListenerActive() {
        return Config.feature.qol.qolShortcutWarps && SkyblockData.getCurrentGamemode().isSkyblock();
    }

    private final Set<Integer> activeKeySet = new HashSet<>();

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        if (!Config.feature.qol.qolShortcutWarps) return;
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;

        checkWarp(player, Config.feature.qol.qolShortcutWarpIs, "/warp is");
        checkWarp(player, Config.feature.qol.qolShortcutWarpHub, "/warp hub");
        checkWarp(player, Config.feature.qol.qolShortcutWarpDh, "/warp dh");
    }

    private void checkWarp(EntityPlayerSP player, int keyBind, String command) {

        boolean keyPressed = KeybindHelper.isKeyDown(keyBind);

        if (keyPressed && !activeKeySet.contains(keyBind)) {
            
            player.sendChatMessage(command);

            activeKeySet.add(keyBind);
        }

        if (!KeybindHelper.isKeyDown(keyBind)) {
            activeKeySet.remove(keyBind);
        }
    }
}
