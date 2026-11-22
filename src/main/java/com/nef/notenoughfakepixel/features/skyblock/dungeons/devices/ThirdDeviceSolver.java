/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.devices;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketWriteEvent;
import com.nef.notenoughfakepixel.mixin.accesors.C02PacketUseEntityAccessor;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonManager;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import net.minecraft.block.BlockSeaLantern;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RegisterEvents
public class ThirdDeviceSolver {

    private final Minecraft mc = Minecraft.getMinecraft();

    private static final Map<BlockPos, Integer> itemFramesRotations;

    static {
        Map<BlockPos, Integer> rotations = new HashMap<>();
        rotations.put(new BlockPos(6, 122, 85), 3);
        rotations.put(new BlockPos(6, 121, 85), 1);
        rotations.put(new BlockPos(6, 121, 84), 1);
        rotations.put(new BlockPos(6, 121, 83), 7);
        rotations.put(new BlockPos(6, 122, 83), 7);
        rotations.put(new BlockPos(6, 123, 83), 7);
        rotations.put(new BlockPos(6, 124, 83), 7);
        rotations.put(new BlockPos(6, 125, 83), 5);
        rotations.put(new BlockPos(6, 125, 84), 5);
        rotations.put(new BlockPos(6, 125, 85), 5);
        rotations.put(new BlockPos(6, 125, 86), 5);
        rotations.put(new BlockPos(6, 125, 87), 3);
        rotations.put(new BlockPos(6, 124, 87), 3);
        rotations.put(new BlockPos(6, 123, 87), 3);
        rotations.put(new BlockPos(6, 122, 87), 3);
        itemFramesRotations = Collections.unmodifiableMap(rotations);
    }

    // Client-thread-owned lookup for rendering/resync.
    private final Map<Integer, FrameClickState> frameStatesByEntityId = new HashMap<>();
    private final Map<BlockPos, FrameClickState> frameStatesByPosition = new HashMap<>();

    // Immutable map reference published to the Netty thread. FrameClickState itself synchronizes debits.
    private volatile Map<Integer, FrameClickState> packetFrameSnapshot = Collections.emptyMap();
    private volatile boolean packetGuardEnabled;
    private volatile long clientTickCounter;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPacket(PacketWriteEvent event) {
        if (!(event.packet instanceof C02PacketUseEntity) || !packetGuardEnabled) return;

        // Never resolve a live Entity or touch mc.theWorld from Netty.
        C02PacketUseEntity packet = (C02PacketUseEntity) event.packet;
        int entityId = ((C02PacketUseEntityAccessor) packet).getEntityId();
        FrameClickState state = packetFrameSnapshot.get(entityId);
        if (state != null && state.shouldCancelPacket(clientTickCounter)) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        clientTickCounter++;

        if (!isFeatureActive()) {
            clearFrameStates();
            return;
        }

        if (clientTickCounter % 5L == 0L) refreshFrameStates();
    }

    private boolean isFeatureActive() {
        return Config.feature.dungeons.dungeonsThirdDeviceSolver
                && mc.theWorld != null
                && mc.thePlayer != null
                && DungeonManager.checkEssentialsF7();
    }

    /** Runs only on the client thread and publishes entity-id -> click-budget snapshots. */
    private void refreshFrameStates() {
        Map<Integer, FrameClickState> nextById = new HashMap<>();
        Map<BlockPos, FrameClickState> nextByPosition = new HashMap<>();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityItemFrame)) continue;

            EntityItemFrame frame = (EntityItemFrame) entity;
            ItemStack displayed = frame.getDisplayedItem();
            if (displayed == null || displayed.getItem() != Items.arrow) continue;

            BlockPos position = new BlockPos(frame.getPosition());
            Integer desiredRotation = itemFramesRotations.get(position);
            if (desiredRotation == null) continue;

            int entityId = frame.getEntityId();
            FrameClickState state = frameStatesByEntityId.get(entityId);
            if (state == null || !state.position.equals(position) || state.desiredRotation != desiredRotation) {
                state = new FrameClickState(entityId, position, desiredRotation, frame.getRotation());
            } else {
                state.syncServerRotation(frame.getRotation());
                state.activate();
            }

            nextById.put(entityId, state);
            nextByPosition.put(position, state);
        }

        for (FrameClickState previous : frameStatesByEntityId.values()) {
            if (!nextById.containsKey(previous.entityId)) previous.deactivate();
        }

        frameStatesByEntityId.clear();
        frameStatesByEntityId.putAll(nextById);
        frameStatesByPosition.clear();
        frameStatesByPosition.putAll(nextByPosition);

        // Publish a defensive immutable copy; all contained states have thread-safe click budgets.
        packetFrameSnapshot = Collections.unmodifiableMap(new HashMap<>(nextById));
        packetGuardEnabled = true;
    }

    private void clearFrameStates() {
        for (FrameClickState state : frameStatesByEntityId.values()) state.deactivate();
        frameStatesByEntityId.clear();
        frameStatesByPosition.clear();
        packetFrameSnapshot = Collections.emptyMap();
        packetGuardEnabled = false;
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!Config.feature.dungeons.dungeonsThirdDeviceSolver || !DungeonManager.checkEssentialsF7()) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityItemFrame)) continue;

            EntityItemFrame itemFrame = (EntityItemFrame) entity;
            ItemStack item = itemFrame.getDisplayedItem();
            if (item == null || item.getItem() != Items.arrow) continue;

            BlockPos pos = getBlockUnderItemFrame(itemFrame);
            if (pos == null) continue;

            Color color = mc.theWorld.getBlockState(pos).getBlock() instanceof BlockSeaLantern
                    ? ColorUtils.getColor(Config.feature.dungeons.dungeonsCorrectColor)
                    : ColorUtils.getColor(Config.feature.dungeons.dungeonsAlternativeColor);
            RenderUtils.highlightBlock(pos, color, false, event.partialTicks);

            BlockPos framePos = new BlockPos(itemFrame.getPosition());
            Integer desiredRotation = itemFramesRotations.get(framePos);
            if (desiredRotation == null) continue;

            FrameClickState state = frameStatesByPosition.get(framePos);
            int clicksNeeded = state != null
                    ? state.getRemainingClicks()
                    : (desiredRotation - itemFrame.getRotation() + 8) % 8;

            double[] renderPos = new double[]{framePos.getX() - 0.7, framePos.getY() - 3.3, framePos.getZ()};
            Color textColor = clicksNeeded == 0
                    ? ColorUtils.getColor(Config.feature.dungeons.dungeonsCorrectColor)
                    : ColorUtils.getColor(Config.feature.dungeons.dungeonsAlternativeColor);
            RenderUtils.drawTag(String.valueOf(clicksNeeded), renderPos, textColor, event.partialTicks);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // Do not mutate client-owned maps from the integrated-server unload event.
        if (event.world == null || !event.world.isRemote) return;
        clearFrameStates();
    }

    private static BlockPos getBlockUnderItemFrame(EntityItemFrame itemFrame) {
        switch (itemFrame.facingDirection) {
            case NORTH: return new BlockPos(itemFrame.posX, itemFrame.posY, itemFrame.posZ + 1);
            case EAST: return new BlockPos(itemFrame.posX - 1, itemFrame.posY, itemFrame.posZ);
            case SOUTH: return new BlockPos(itemFrame.posX, itemFrame.posY, itemFrame.posZ - 1);
            case WEST: return new BlockPos(itemFrame.posX + 1, itemFrame.posY, itemFrame.posZ);
            default: return null;
        }
    }

    private static final class FrameClickState {
        private final int entityId;
        private final BlockPos position;
        private final int desiredRotation;
        private volatile boolean active = true;
        private volatile int observedServerRotation;
        private volatile int remainingClicks;
        private long lastReservedTick = Long.MIN_VALUE;

        private FrameClickState(int entityId, BlockPos position, int desiredRotation, int serverRotation) {
            this.entityId = entityId;
            this.position = position;
            this.desiredRotation = desiredRotation;
            this.observedServerRotation = serverRotation;
            this.remainingClicks = calculateRemaining(desiredRotation, serverRotation);
        }

        /** Returns true only when this packet must be cancelled. Inactive snapshots are ignored. */
        private synchronized boolean shouldCancelPacket(long clientTick) {
            if (!active) return false;
            // Preserve the original one-interaction-per-frame guard without allocating packet keys.
            if (lastReservedTick == clientTick || remainingClicks <= 0) return true;
            remainingClicks--;
            lastReservedTick = clientTick;
            return false;
        }

        private synchronized void syncServerRotation(int serverRotation) {
            if (serverRotation != observedServerRotation) {
                observedServerRotation = serverRotation;
                remainingClicks = calculateRemaining(desiredRotation, serverRotation);
            }
        }

        private synchronized void activate() {
            active = true;
        }

        private synchronized void deactivate() {
            active = false;
        }

        private int getRemainingClicks() {
            return remainingClicks;
        }

        private static int calculateRemaining(int desiredRotation, int currentRotation) {
            return (desiredRotation - currentRotation + 8) % 8;
        }
    }
}
