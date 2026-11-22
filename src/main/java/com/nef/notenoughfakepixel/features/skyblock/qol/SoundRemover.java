/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.utils.InventoryUtils;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class SoundRemover {
    private final Minecraft mc = Minecraft.getMinecraft();
    private volatile boolean holdingJerry;
    private volatile boolean holdingAote;
    private volatile boolean zombieRareDropMode;
    private volatile long lastZombieDeath = -1L;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        holdingJerry = false;
        holdingAote = false;
        zombieRareDropMode = Config.feature.qol.qolDisableZombieRareDrops;
        if (mc.thePlayer == null) return;
        holdingJerry = Config.feature.qol.qolDisableJerryChineGunSounds &&
                InventoryUtils.getSlot("Jerry-chine Gun") == InventoryUtils.getCurrentSlot();
        updateAoteState();
    }

    private void updateAoteState() {
        if (mc.thePlayer == null) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        String internalName = ItemUtils.getInternalName(held);
        holdingAote = Config.feature.qol.qolDisableAoteSounds && "ASPECT_OF_THE_END".equals(internalName);
    }

    @SubscribeEvent
    public void onSoundPacketReceive(PacketReadEvent event) {
        Packet packet = event.packet;
        if (!(packet instanceof S29PacketSoundEffect)) return;
        S29PacketSoundEffect soundEffect = (S29PacketSoundEffect) packet;

        switch (soundEffect.getSoundName()) {
            case "mob.villager.yes":
            case "mob.villager.haggle":
                if (holdingJerry) event.setCanceled(true);
                break;
            case "mob.endermen.portal":
                if (holdingAote && Config.feature.qol.qolEtherwarpSound == 0) event.setCanceled(true);
                break;
            case "mob.zombie.death":
                if (zombieRareDropMode) lastZombieDeath = System.currentTimeMillis();
                break;
            case "note.pling":
                long last = lastZombieDeath;
                if (zombieRareDropMode && last > 0L && System.currentTimeMillis() - last < 1000L &&
                        (soundEffect.getVolume() == 0.4f || soundEffect.getVolume() == 0.8f)) {
                    event.setCanceled(true);
                }
                break;
        }
    }
}
