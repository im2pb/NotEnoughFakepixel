/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.fishing;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.ParticlePacketEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.SoundUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Beta-42 wake-chain fishing notifier. The feature is opt-in so it does not double the existing
 * standard fishing countdown sound unless the user deliberately enables this advanced notifier.
 */
@RegisterEvents
public class FishingAlert {
    private static final long SLUG_DELAY_MS = 20_000L;
    private static final long SPLASH_WINDOW_MS = 500L;
    private static final long BITE_ALERT_DELAY_MS = 50L;
    private static final float ZERO_PITCH = 1.0f;
    private static final float MAX_PITCH = 0.1f;
    private static final float MAX_DIST = 5.0f;
    private static final int MAX_PING_SAMPLES = 5;
    private static final int MAX_HOOKS = 128;

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Integer, EntityFishHook> hookEntities = new ConcurrentHashMap<>();
    private final Map<WakeChain, List<Integer>> chains = new ConcurrentHashMap<>();
    private final List<Integer> pingDelayList = new ArrayList<>();

    private long lastCastRodMillis;
    private int pingDelayTicks;
    private int buildupSoundDelay;
    private int hookedStateTicks;
    private int tickCounter;
    private S2APacketParticles lastProcessedPacket;
    private long countdownEtaMs;
    private long slugLavaEntryMs;
    private long awaitingSplashUntilMs;
    private long splashConfirmedMs;
    private boolean biteAlertFired;
    private WarningState warningState = WarningState.NOTHING;

    private static boolean isSkyblock() {
        return SkyblockData.getCurrentGamemode() != null
                && SkyblockData.getCurrentGamemode().isSkyblock();
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (!Config.feature.fishing.fishingAlertEnabled || mc.thePlayer == null
                || event == null || event.world == null || !event.world.isRemote
                || !(event.entity instanceof EntityFishHook)) return;
        EntityFishHook hook = (EntityFishHook) event.entity;
        hookEntities.put(hook.getEntityId(), hook);
        if (hook.angler == mc.thePlayer) {
            long now = System.currentTimeMillis();
            long delay = now - lastCastRodMillis;
            if (delay > 0L && delay < 500L) {
                pingDelayList.add(0, (int) Math.min(delay, 300L));
                while (pingDelayList.size() > MAX_PING_SAMPLES) pingDelayList.remove(pingDelayList.size() - 1);
            }
        }
        // The world list can briefly include stale hooks during reconnects; keep this tracker bounded.
        if (hookEntities.size() > MAX_HOOKS) {
            for (Integer id : new ArrayList<>(hookEntities.keySet())) {
                EntityFishHook candidate = hookEntities.get(id);
                if (candidate == null || candidate.isDead || hookEntities.size() > MAX_HOOKS) hookEntities.remove(id);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!Config.feature.fishing.fishingAlertEnabled || event == null
                || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR
                || event.entityPlayer != mc.thePlayer || event.entityPlayer == null) return;
        ItemStack held = event.entityPlayer.getHeldItem();
        if (held == null || held.getItem() != Items.fishing_rod) return;
        long now = System.currentTimeMillis();
        if (now - lastCastRodMillis > 500L) lastCastRodMillis = now;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.feature.fishing.fishingAlertEnabled || mc.thePlayer == null || mc.theWorld == null) {
            resetTransient();
            return;
        }

        if (buildupSoundDelay > 0) buildupSoundDelay--;
        EntityFishHook ownHook = mc.thePlayer.fishEntity;
        if (ownHook == null || ownHook.isDead) {
            countdownEtaMs = 0L;
            slugLavaEntryMs = 0L;
            awaitingSplashUntilMs = 0L;
            splashConfirmedMs = 0L;
            biteAlertFired = false;
            hookedStateTicks = 0;
            warningState = WarningState.NOTHING;
        }

        if (Config.feature.fishing.fishingSlugMode && ownHook != null && !ownHook.isDead) {
            BlockPos bobberPos = new BlockPos(ownHook.posX, ownHook.posY, ownHook.posZ);
            Block block = mc.theWorld.getBlockState(bobberPos).getBlock();
            boolean inLava = block == Blocks.lava || block == Blocks.flowing_lava;
            if (inLava) {
                if (slugLavaEntryMs == 0L) slugLavaEntryMs = System.currentTimeMillis();
            } else {
                slugLavaEntryMs = 0L;
            }
        } else if (!Config.feature.fishing.fishingSlugMode) {
            slugLavaEntryMs = 0L;
        }

        if (ownHook != null && !pingDelayList.isEmpty()) {
            while (pingDelayList.size() > MAX_PING_SAMPLES) pingDelayList.remove(pingDelayList.size() - 1);
            int total = 0;
            for (int delay : pingDelayList) total += delay;
            pingDelayTicks = (int) Math.floor((total / (double) pingDelayList.size()) / 50.0);
        }

        long now = System.currentTimeMillis();
        if (splashConfirmedMs > 0L && !biteAlertFired && now >= splashConfirmedMs && !isSlugWaiting()) {
            SoundUtils.playGlobalSound("note.pling", 1.0f, 2.0f);
            biteAlertFired = true;
            splashConfirmedMs = 0L;
        }

        if (hookedStateTicks > 0) {
            hookedStateTicks--;
            warningState = WarningState.HOOKED;
        } else {
            awaitingSplashUntilMs = 0L;
            warningState = WarningState.NOTHING;
            if (ownHook != null && !ownHook.isDead) {
                int ownId = ownHook.getEntityId();
                for (Map.Entry<WakeChain, List<Integer>> entry : chains.entrySet()) {
                    if (entry.getKey().particleNum >= 3 && entry.getValue().contains(ownId)) {
                        warningState = WarningState.INCOMING;
                        break;
                    }
                }
            }
        }

        // Cleanup dead hooks and short-lived particle chains at a low fixed frequency.
        if (tickCounter++ >= 20) {
            tickCounter = 0;
            hookEntities.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isDead);
            chains.entrySet().removeIf(entry -> now - entry.getKey().lastUpdate > 200L
                    || entry.getValue().isEmpty() || Collections.disjoint(entry.getValue(), hookEntities.keySet()));
        }
    }

    @SubscribeEvent
    public void onParticlePacket(ParticlePacketEvent event) {
        if (!Config.feature.fishing.fishingAlertEnabled || !isSkyblock()) return;
        if (event == null || event.getPacket() == null || hookEntities.isEmpty()) return;
        S2APacketParticles packet = event.getPacket();
        // Defensive duplicate guard: the same packet must never advance a wake chain twice.
        if (packet == lastProcessedPacket) return;
        lastProcessedPacket = packet;
        EnumParticleTypes type = packet.getParticleType();
        long now = System.currentTimeMillis();

        // Confirm the actual splash after the approach phase; approach particles alone do not fire the bite sound.
        EntityFishHook ownHook = mc.thePlayer == null ? null : mc.thePlayer.fishEntity;
        if (awaitingSplashUntilMs > 0L && !biteAlertFired
                && (type == EnumParticleTypes.WATER_BUBBLE || type == EnumParticleTypes.LAVA) && ownHook != null) {
            if (now <= awaitingSplashUntilMs) {
                double dx = packet.getXCoordinate() - ownHook.posX;
                double dy = packet.getYCoordinate() - ownHook.posY;
                double dz = packet.getZCoordinate() - ownHook.posZ;
                if (dx * dx + dy * dy + dz * dz <= 1.0) {
                    splashConfirmedMs = now + BITE_ALERT_DELAY_MS;
                    awaitingSplashUntilMs = 0L;
                }
            } else {
                awaitingSplashUntilMs = 0L;
            }
        }

        if (type != EnumParticleTypes.WATER_WAKE && type != EnumParticleTypes.SMOKE_NORMAL
                && type != EnumParticleTypes.FLAME) return;
        if (Math.abs(packet.getYOffset() - 0.01f) > 0.001f) return;

        double x = packet.getXCoordinate();
        double y = packet.getYCoordinate();
        double z = packet.getZCoordinate();
        double xOffset = packet.getXOffset();
        double zOffset = packet.getZOffset();
        double angle1 = calcAngle(xOffset, -zOffset);
        double angle2 = calcAngle(-xOffset, zOffset);
        List<Integer> possible1 = new ArrayList<>();
        List<Integer> possible2 = new ArrayList<>();

        for (EntityFishHook hook : hookEntities.values()) {
            if (hook == null || hook.isDead) continue;
            HookResult result = classifyHook(hook, x, y, z, angle1, angle2);
            if (result == HookResult.ANGLE1 || result == HookResult.EITHER) possible1.add(hook.getEntityId());
            if (result == HookResult.ANGLE2 || result == HookResult.EITHER) possible2.add(hook.getEntityId());
        }
        if (possible1.isEmpty() && possible2.isEmpty()) return;

        boolean foundChain = false;
        for (Map.Entry<WakeChain, List<Integer>> entry : chains.entrySet()) {
            WakeChain chain = entry.getKey();
            if (now - chain.lastUpdate > 200L) continue;
            List<Integer> possible;
            double updateAngle;
            if (angleWithinRange(chain.currentAngle, angle1, 16.0)) {
                possible = possible1;
                updateAngle = angle1;
            } else if (angleWithinRange(chain.currentAngle, angle2, 16.0)) {
                possible = possible2;
                updateAngle = angle2;
            } else continue;
            if (Collections.disjoint(entry.getValue(), possible)) continue;

            Set<Integer> kept = new HashSet<>();
            for (int hookId : possible) {
                if (!entry.getValue().contains(hookId) || !chain.distances.containsKey(hookId)) continue;
                EntityFishHook hook = hookEntities.get(hookId);
                if (hook == null || hook.isDead) continue;
                double oldDistance = chain.distances.get(hookId);
                double dx = hook.posX - x;
                double dz = hook.posZ - z;
                double newDistance = Math.sqrt(dx * dx + dz * dz);
                double delta = oldDistance - newDistance;
                if (newDistance >= 0.2 && (delta <= -0.1 || delta >= 0.3)) continue;

                if (ownHook != null && ownHook.getEntityId() == hookId && chain.particleNum > 3) {
                    float lavaOffset = type == EnumParticleTypes.SMOKE_NORMAL ? 0.03f : 0.1f;
                    if (newDistance <= 0.2f + lavaOffset * pingDelayTicks) {
                        if (hookedStateTicks <= 0 && !isSlugWaiting()) {
                            awaitingSplashUntilMs = now + SPLASH_WINDOW_MS;
                            biteAlertFired = false;
                        }
                        hookedStateTicks = 12;
                    } else if (newDistance >= 0.4f + 0.1f * pingDelayTicks && buildupSoundDelay <= 0 && !isSlugWaiting()) {
                        SoundUtils.playGlobalSound("note.pling", 0.5f,
                                calcPitch((float) newDistance - (0.3f + 0.1f * pingDelayTicks)));
                        buildupSoundDelay = 4;
                    }
                }

                if (ownHook != null && ownHook.getEntityId() == hookId) {
                    long timeDiff = now - chain.lastUpdate;
                    double distanceDecrease = oldDistance - newDistance;
                    if (timeDiff > 0L && distanceDecrease > 0.0) {
                        double speedBlocksPerMs = distanceDecrease / timeDiff;
                        countdownEtaMs = now + (long) (newDistance / speedBlocksPerMs);
                    }
                }
                chain.distances.put(hookId, newDistance);
                kept.add(hookId);
            }

            if (kept.isEmpty()) continue;
            entry.getValue().retainAll(kept);
            chain.distances.keySet().retainAll(kept);
            chain.lastUpdate = now;
            chain.particleNum++;
            chain.currentAngle = updateAngle;
            foundChain = true;
        }

        if (!foundChain) {
            possible1.removeAll(possible2);
            List<Integer> hooks = !possible1.isEmpty() ? possible1 : possible2;
            double useAngle = !possible1.isEmpty() ? angle1 : angle2;
            if (hooks.isEmpty()) return;
            WakeChain chain = new WakeChain(now, useAngle);
            List<Integer> validHooks = new ArrayList<>();
            for (int hookId : hooks) {
                EntityFishHook hook = hookEntities.get(hookId);
                if (hook == null || hook.isDead) continue;
                double dx = hook.posX - x;
                double dz = hook.posZ - z;
                chain.distances.put(hookId, Math.sqrt(dx * dx + dz * dz));
                validHooks.add(hookId);
            }
            if (!validHooks.isEmpty()) chains.put(chain, validHooks);
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !isSkyblock()
                || !Config.feature.fishing.fishingAlertEnabled || warningState == WarningState.NOTHING || isSlugWaiting()) return;
        String text;
        if (warningState == WarningState.HOOKED) {
            if (!biteAlertFired) return;
            text = "§c§l!!!";
        } else {
            long remaining = countdownEtaMs - System.currentTimeMillis();
            boolean etaValid = remaining > 100L && remaining < 10_000L;
            if (Config.feature.fishing.fishingBiteEta && etaValid) {
                text = "§e§l" + String.format(java.util.Locale.ROOT, "%.1f", remaining / 1000.0) + "s";
            } else if (Config.feature.fishing.fishingIncomingMessage) {
                text = "§e§lINCOMING";
            } else return;
        }
        FontRenderer font = mc.fontRendererObj;
        if (font == null) return;
        ScaledResolution resolution = new ScaledResolution(mc);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(2.0, 2.0, 2.0);
            int x = resolution.getScaledWidth() / 4 - font.getStringWidth(text) / 2;
            int y = resolution.getScaledHeight() / 4 + 10;
            font.drawStringWithShadow(text, x, y, 0xFFFFFF);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        hookEntities.clear();
        chains.clear();
        pingDelayList.clear();
        warningState = WarningState.NOTHING;
        hookedStateTicks = 0;
        countdownEtaMs = 0L;
        slugLavaEntryMs = 0L;
        awaitingSplashUntilMs = 0L;
        splashConfirmedMs = 0L;
        biteAlertFired = false;
        lastProcessedPacket = null;
        pingDelayTicks = 0;
        buildupSoundDelay = 0;
        tickCounter = 0;
        lastCastRodMillis = 0L;
    }

    private void resetTransient() {
        hookEntities.clear();
        chains.clear();
        pingDelayList.clear();
        warningState = WarningState.NOTHING;
        hookedStateTicks = 0;
        countdownEtaMs = 0L;
        slugLavaEntryMs = 0L;
        awaitingSplashUntilMs = 0L;
        splashConfirmedMs = 0L;
        biteAlertFired = false;
        lastProcessedPacket = null;
        pingDelayTicks = 0;
        buildupSoundDelay = 0;
        tickCounter = 0;
    }

    private boolean isSlugWaiting() {
        return Config.feature.fishing.fishingSlugMode && slugLavaEntryMs > 0L
                && System.currentTimeMillis() - slugLavaEntryMs < SLUG_DELAY_MS;
    }

    private double calcAngle(double xOffset, double zOffset) {
        double angleX = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, xOffset / 0.04))));
        double angleZ = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, zOffset / 0.04))));
        if (xOffset < 0.0) angleZ = 180.0 - angleZ;
        if (zOffset < 0.0) angleX = 360.0 - angleX;
        angleX = ((angleX % 360.0) + 360.0) % 360.0;
        angleZ = ((angleZ % 360.0) + 360.0) % 360.0;
        double difference = angleX - angleZ;
        if (difference < -180.0) difference += 360.0;
        if (difference > 180.0) difference -= 360.0;
        return angleZ + difference / 2.0;
    }

    private boolean angleWithinRange(double a, double b, double range) {
        double difference = Math.abs(a - b);
        if (difference > 180.0) difference = 360.0 - difference;
        return difference <= range;
    }

    private HookResult classifyHook(EntityFishHook hook, double particleX, double particleY, double particleZ,
                                    double angle1, double angle2) {
        double deltaY = particleY - hook.posY;
        double tolerance = 0.5;
        if (mc.theWorld != null) {
            for (int offset = -2; offset < 2; offset++) {
                Block block = mc.theWorld.getBlockState(new BlockPos(particleX, particleY + offset, particleZ)).getBlock();
                if (block == Blocks.flowing_lava || block == Blocks.flowing_water || block == Blocks.lava) {
                    tolerance = 2.0;
                    break;
                }
            }
        }
        if (Math.abs(deltaY) > tolerance) return HookResult.NOT_POSSIBLE;
        double deltaX = particleX - hook.posX;
        double deltaZ = particleZ - hook.posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distance < 0.2) return HookResult.EITHER;
        float allowance = (float) Math.toDegrees(Math.atan2(0.03125, distance)) * 1.5f;
        float hookAngle = (float) Math.toDegrees(Math.atan2(deltaX, deltaZ));
        hookAngle = ((hookAngle % 360.0f) + 360.0f) % 360.0f;
        if (angleWithinRange(angle1, hookAngle, allowance)) return HookResult.ANGLE1;
        if (angleWithinRange(angle2, hookAngle, allowance)) return HookResult.ANGLE2;
        return HookResult.NOT_POSSIBLE;
    }

    private float calcPitch(float distance) {
        distance = Math.max(0.1f, Math.min(distance, MAX_DIST));
        return 1.0f / (distance + (1.0f / (ZERO_PITCH - MAX_PITCH))) * (1.0f - distance / MAX_DIST) + MAX_PITCH;
    }

    public enum WarningState { NOTHING, INCOMING, HOOKED }
    private enum HookResult { NOT_POSSIBLE, EITHER, ANGLE1, ANGLE2 }

    private static final class WakeChain {
        final Map<Integer, Double> distances = new HashMap<>();
        int particleNum;
        long lastUpdate;
        double currentAngle;
        WakeChain(long lastUpdate, double angle) { this.lastUpdate = lastUpdate; this.currentAngle = angle; }
    }
}
