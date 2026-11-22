/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol.abiphone;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterKeybind;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@RegisterEvents
public final class AbiphoneKeys {

    @RegisterKeybind
    public static final KeyBinding OPEN_ABIPHONE =
            new KeyBinding("Open Abiphone Vault", Keyboard.KEY_NONE, "NotEnoughFakepixel");

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!OPEN_ABIPHONE.isPressed()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;
        if (Config.feature == null || !Config.feature.qol.qolAbiphone) return;
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;

        mc.displayGuiScreen(new AbiphonePanel(Config.feature.qol.qolAbiphoneContacts));
    }
}
