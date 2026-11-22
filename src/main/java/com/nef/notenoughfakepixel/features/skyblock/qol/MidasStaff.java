/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@RegisterEvents
public class MidasStaff {

    private final Minecraft mc = Minecraft.getMinecraft();
    private volatile boolean holdingMidasStaff;
    private final AtomicInteger grassSoundCounter = new AtomicInteger();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        holdingMidasStaff = false;
        if (!Config.feature.qol.qolDisableMidaStaffAnimation ||
                !SkyblockData.getCurrentGamemode().isSkyblock() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        holdingMidasStaff = InventoryUtils.getSlot("Midas Staff") != -1;
        if (!holdingMidasStaff) return;

        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().expand(20, 20, 20);
        Collection<EntityFallingBlock> entities = mc.theWorld.getEntitiesWithinAABB(EntityFallingBlock.class, bb);
        for (EntityFallingBlock entity : entities) {
            if (entity.getBlock() != null && "minecraft:gold_block".equals(entity.getBlock().toString()) && !entity.isDead) {
                entity.setDead();
            }
        }
    }

    @SubscribeEvent
    public void onParticlePacketReceive(PacketReadEvent event) {
        if (!holdingMidasStaff || !Config.feature.qol.qolDisableMidaStaffAnimation) return;
        Packet packet = event.packet;
        if (packet instanceof S2APacketParticles) {
            S2APacketParticles particles = (S2APacketParticles) packet;
            if (particles.getParticleType() != null
                    && "lava".equals(particles.getParticleType().getParticleName())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onSoundPacketReceive(PacketReadEvent event) {
        if (!holdingMidasStaff || !Config.feature.qol.qolDisableMidaStaffAnimation) return;
        Packet packet = event.packet;
        if (packet instanceof S29PacketSoundEffect &&
                "dig.grass".equals(((S29PacketSoundEffect) packet).getSoundName())) {
            if (grassSoundCounter.incrementAndGet() > 1) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void handleClick(PlayerInteractEvent event) {
        if (!holdingMidasStaff) return;
        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_AIR ||
                event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            grassSoundCounter.set(0);
        }
    }
}
