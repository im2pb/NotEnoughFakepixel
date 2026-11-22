/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.features.skyblock.mining.service.DrillPacketCorrectionPolicy;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class DrillFix {

    private final Minecraft mc = Minecraft.getMinecraft();
    /** One coherent snapshot shared with Netty's packet-read thread. */
    private volatile DrillPacketContext packetContext = DrillPacketContext.EMPTY;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        packetContext = DrillPacketContext.EMPTY;

        EntityPlayer player = mc.thePlayer;
        if (!Config.feature.mining.miningDrillFix || player == null ||
                !SkyblockData.getCurrentGamemode().isSkyblock()) return;

        Location location = SkyblockData.getCurrentLocation();
        if (location != Location.DWARVEN && location != Location.CRYSTAL_HOLLOWS) return;

        int selectedSlot = player.inventory.currentItem;
        if (selectedSlot < 0 || selectedSlot >= 9) return;

        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !DrillPacketCorrectionPolicy.isDrillInternalName(ItemUtils.getInternalName(heldItem))) {
            return;
        }

        // PacketReadEvent is posted by Netty. Never read mutable player inventory state from that thread.
        packetContext = new DrillPacketContext(
                mc.gameSettings.keyBindAttack.isKeyDown(),
                selectedSlot,
                heldItem.copy()
        );
    }

    @SubscribeEvent
    public void onPacketRead(PacketReadEvent event) {
        DrillPacketContext context = packetContext;
        if (context == DrillPacketContext.EMPTY || !context.attackHeld ||
                context.heldDrillSnapshot == null || context.selectedHotbarSlot < 0) return;

        if (event.packet instanceof S2FPacketSetSlot) {
            S2FPacketSetSlot packet = (S2FPacketSetSlot) event.packet;
            ItemStack packetItem = packet.func_149174_e();
            boolean suppress = DrillPacketCorrectionPolicy.shouldSuppressSingleSlotUpdate(
                    true,
                    context.attackHeld,
                    true,
                    context.selectedHotbarSlot,
                    packet.func_149175_c(),
                    packet.func_149173_d(),
                    packetItem != null && packetItem.getItem() == Items.prismarine_shard
            );
            if (suppress) event.setCanceled(true);
            return;
        }

        if (event.packet instanceof S30PacketWindowItems) {
            S30PacketWindowItems packet = (S30PacketWindowItems) event.packet;
            ItemStack[] items = packet.getItemStacks();
            int slot = context.selectedHotbarSlot + 36;
            boolean repair = items != null && slot >= 0 && slot < items.length
                    && DrillPacketCorrectionPolicy.shouldRepairWindowItemsUpdate(
                    true,
                    context.attackHeld,
                    true,
                    context.selectedHotbarSlot,
                    items.length,
                    items[slot] != null && items[slot].getItem() == Items.prismarine_shard
            );
            if (repair) {
                // Preserve all other server inventory updates and restore only the selected drill slot.
                // Use a fresh copy so packet processing cannot mutate our cross-thread snapshot.
                items[slot] = context.heldDrillSnapshot.copy();
            }
        }
    }

    private static final class DrillPacketContext {
        private static final DrillPacketContext EMPTY = new DrillPacketContext(false, -1, null);

        private final boolean attackHeld;
        private final int selectedHotbarSlot;
        /** Never exposed or mutated after construction; copied again before insertion into a packet. */
        private final ItemStack heldDrillSnapshot;

        private DrillPacketContext(boolean attackHeld, int selectedHotbarSlot, ItemStack heldDrillSnapshot) {
            this.attackHeld = attackHeld;
            this.selectedHotbarSlot = selectedHotbarSlot;
            this.heldDrillSnapshot = heldDrillSnapshot;
        }
    }

}
