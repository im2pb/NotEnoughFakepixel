/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterKeybind;
import com.nef.notenoughfakepixel.variables.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

@RegisterEvents
public final class EntityTagKeys {

    @RegisterKeybind
    public static final KeyBinding TOGGLE_DAMAGE_TAGS =
            new KeyBinding("Toggle Damage Tags", Keyboard.KEY_NONE, "NotEnoughFakepixel");

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!TOGGLE_DAMAGE_TAGS.isPressed()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        boolean hidden = EntityTagFilter.toggleDamageTags();
        mc.thePlayer.addChatMessage(new ChatComponentText(
                Constants.PREFIX_INFO + "Damage tags " + (hidden ? "hidden" : "shown")));
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) EntityTagFilter.onWorldChanged();
    }
}
