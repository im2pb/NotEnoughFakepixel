package com.nef.notenoughfakepixel.features.skyblock.fishing;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.ParticlePacketEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.SoundUtils;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RegisterEvents
public class FishingAlert {

    private static final long SLUG_DELAY_MS = 20_000;
    private static final long SPLASH_WINDOW_MS = 500;
    private static final long BITE_ALERT_DELAY_MS = 50;
    private static final float ZERO_PITCH = 1.0f, MAX_PITCH = 0.1f, MAX_DIST = 5f;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Map<Integer, EntityFishHook> hookEntities = new ConcurrentHashMap<>();
    private final Map<WakeChain, List<Integer>> chains = new ConcurrentHashMap<>();
    private final List<Integer> pingDelayList = new ArrayList<>();
    private long lastCastRodMillis = 0;
    private int pingDelayTicks = 0;
    private int buildupSoundDelay = 0;
    private int hookedStateTicks = 0;
    private int tickCounter = 0;
    // Used to deduplicate the double-fire from MixinNetHandlerPlayClient
    private S2APacketParticles lastProcessedPacket = null;
    // Absolute timestamp (ms) when the wake chain is predicted to reach the bobber
    private long countdownEtaMs = 0;
    // Slug mode: timestamp when the bobber first entered lava (0 = not in lava)
    private long slugLavaEntryMs = 0;
    // Splash confirmation: non-zero while waiting for a bite splash near the bobber
    private long awaitingSplashUntilMs = 0;
    private long splashConfirmedMs = 0;
    private boolean biteAlertFired = false;
    private WarningState warningState = WarningState.NOTHING;

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (!event.world.isRemote) return;
        Entity e = event.entity;
        if (!(e instanceof EntityFishHook)) return;

        EntityFishHook hook = (EntityFishHook) e;
        hookEntities.put(hook.getEntityId(), hook);

        if (hook.angler == mc.thePlayer) {
            long now = System.currentTimeMillis();
            long delay = now - lastCastRodMillis;
            if (delay > 0 && delay < 500) {
                pingDelayList.add(0, (int) Math.min(delay, 300));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR) return;
        if (event.entityPlayer != mc.thePlayer) return;
        ItemStack held = event.entityPlayer.getHeldItem();
        if (held == null || held.getItem() != Items.fishing_rod) return;
        long now = System.currentTimeMillis();
        if (now - lastCastRodMillis > 500) lastCastRodMillis = now;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        if (!Config.feature.fishing.alert.enabled) return;

        if (buildupSoundDelay > 0) buildupSoundDelay--;

        if (mc.thePlayer.fishEntity == null) {
            countdownEtaMs = 0;
            slugLavaEntryMs = 0;
        }

        // Slug mode: track whether the bobber is sitting in lava
        if (Config.feature.fishing.alert.slugMode && mc.thePlayer.fishEntity != null && mc.theWorld != null) {
            BlockPos bobberPos = new BlockPos(mc.thePlayer.fishEntity.posX, mc.thePlayer.fishEntity.posY, mc.thePlayer.fishEntity.posZ);
            net.minecraft.block.Block block = mc.theWorld.getBlockState(bobberPos).getBlock();
            boolean inLava = block == Blocks.lava || block == Blocks.flowing_lava;
            if (inLava) {
                if (slugLavaEntryMs == 0) slugLavaEntryMs = System.currentTimeMillis();
            } else {
                slugLavaEntryMs = 0;
            }
        }

        // Refine ping estimate while bobber is out
        if (mc.thePlayer.fishEntity != null && !pingDelayList.isEmpty()) {
            while (pingDelayList.size() > 5) pingDelayList.remove(pingDelayList.size() - 1);
            int total = 0;
            for (int d : pingDelayList) total += d;
            pingDelayTicks = (int) Math.floor((total / (double) pingDelayList.size()) / 50.0);
        }

        // Fire bite alert after splash confirmation delay
        if (splashConfirmedMs > 0 && !biteAlertFired && System.currentTimeMillis() >= splashConfirmedMs && !isSlugWaiting()) {
            SoundUtils.playGlobalSound("note.pling", 1.0f, 2.0f);
            biteAlertFired = true;
            splashConfirmedMs = 0;
        }

        // Advance warning state
        if (hookedStateTicks > 0) {
            hookedStateTicks--;
            warningState = WarningState.HOOKED;
        } else {
            awaitingSplashUntilMs = 0;
            warningState = WarningState.NOTHING;
            if (mc.thePlayer.fishEntity != null) {
                int myId = mc.thePlayer.fishEntity.getEntityId();
                for (Map.Entry<WakeChain, List<Integer>> entry : chains.entrySet()) {
                    if (entry.getKey().particleNum >= 3 && entry.getValue().contains(myId)) {
                        warningState = WarningState.INCOMING;
                        break;
                    }
                }
            }
        }

        // Cleanup dead hooks and stale chains every second
        if (tickCounter++ >= 20) {
            tickCounter = 0;
            long now = System.currentTimeMillis();
            hookEntities.entrySet().removeIf(e -> e.getValue().isDead);
            chains.entrySet().removeIf(entry -> now - entry.getKey().lastUpdate > 200 || entry.getValue().isEmpty() || Collections.disjoint(entry.getValue(), hookEntities.keySet()));
        }
    }

    @SubscribeEvent
    public void onParticlePacket(ParticlePacketEvent event) {
        if (!Config.feature.fishing.alert.enabled) return;
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;
        if (hookEntities.isEmpty()) return;

        S2APacketParticles p = event.getPacket();
        // MixinNetHandlerPlayClient posts the event twice — skip the duplicate
        if (p == lastProcessedPacket) return;
        lastProcessedPacket = p;

        EnumParticleTypes type = p.getParticleType();

        // Splash confirmation: after the approach condition fires, wait for a bite splash near our bobber.
        // Water bite: WATER_BUBBLE. Lava bite: LAVA only (FLAME is also an approach particle and fires too early).
        if (awaitingSplashUntilMs > 0 && !biteAlertFired && (type == EnumParticleTypes.WATER_BUBBLE || type == EnumParticleTypes.LAVA) && mc.thePlayer != null && mc.thePlayer.fishEntity != null) {
            if (System.currentTimeMillis() <= awaitingSplashUntilMs) {
                double dx = p.getXCoordinate() - mc.thePlayer.fishEntity.posX;
                double dy = p.getYCoordinate() - mc.thePlayer.fishEntity.posY;
                double dz = p.getZCoordinate() - mc.thePlayer.fishEntity.posZ;
                if (dx * dx + dy * dy + dz * dz <= 1.0) {
                    splashConfirmedMs = System.currentTimeMillis() + BITE_ALERT_DELAY_MS;
                    awaitingSplashUntilMs = 0;
                }
            } else {
                awaitingSplashUntilMs = 0;
            }
        }
        if (type != EnumParticleTypes.WATER_WAKE && type != EnumParticleTypes.SMOKE_NORMAL && type != EnumParticleTypes.FLAME)
            return;
        if (Math.abs(p.getYOffset() - 0.01f) > 0.001f) return;

        double x = p.getXCoordinate(), y = p.getYCoordinate(), z = p.getZCoordinate();
        double xOff = p.getXOffset(), zOff = p.getZOffset();

        double angle1 = calcAngle(xOff, -zOff);
        double angle2 = calcAngle(-xOff, zOff);

        List<Integer> possible1 = new ArrayList<>();
        List<Integer> possible2 = new ArrayList<>();

        for (EntityFishHook hook : hookEntities.values()) {
            if (hook.isDead) continue;
            HookResult ret = classifyHook(hook, x, y, z, angle1, angle2);
            switch (ret) {
                case ANGLE1:
                    possible1.add(hook.getEntityId());
                    break;
                case ANGLE2:
                    possible2.add(hook.getEntityId());
                    break;
                case EITHER:
                    possible1.add(hook.getEntityId());
                    possible2.add(hook.getEntityId());
                    break;
                default:
                    break;
            }
        }

        if (possible1.isEmpty() && possible2.isEmpty()) return;

        long now = System.currentTimeMillis();
        boolean foundChain = false;

        for (Map.Entry<WakeChain, List<Integer>> entry : chains.entrySet()) {
            WakeChain chain = entry.getKey();
            if (now - chain.lastUpdate > 200) continue;

            List<Integer> possible;
            double updateAngle;
            if (angleWithinRange(chain.currentAngle, angle1, 16)) {
                possible = possible1;
                updateAngle = angle1;
            } else if (angleWithinRange(chain.currentAngle, angle2, 16)) {
                possible = possible2;
                updateAngle = angle2;
            } else continue;

            if (Collections.disjoint(entry.getValue(), possible)) continue;

            Set<Integer> kept = new HashSet<>();
            for (int hookId : possible) {
                if (!entry.getValue().contains(hookId) || !chain.distances.containsKey(hookId)) continue;
                EntityFishHook hook = hookEntities.get(hookId);
                if (hook == null || hook.isDead) continue;

                double oldDist = chain.distances.get(hookId);
                double dx = hook.posX - x, dz = hook.posZ - z;
                double newDist = Math.sqrt(dx * dx + dz * dz);
                double delta = oldDist - newDist;

                if (newDist >= 0.2 && (delta <= -0.1 || delta >= 0.3)) continue;

                // Sound + state for the player's own hook
                if (mc.thePlayer.fishEntity != null && mc.thePlayer.fishEntity.getEntityId() == hookId && chain.particleNum > 3) {
                    float lavaOff = (type == EnumParticleTypes.SMOKE_NORMAL) ? 0.03f : 0.1f;
                    if (newDist <= 0.2f + lavaOff * pingDelayTicks) {
                        if (hookedStateTicks <= 0 && !isSlugWaiting()) {
                            awaitingSplashUntilMs = now + SPLASH_WINDOW_MS;
                            biteAlertFired = false;
                        }
                        hookedStateTicks = 12;
                    } else if (newDist >= 0.4f + 0.1f * pingDelayTicks && buildupSoundDelay <= 0 && !isSlugWaiting()) {
                        SoundUtils.playGlobalSound("note.pling", 0.5f, calcPitch((float) newDist - (0.3f + 0.1f * pingDelayTicks)));
                        buildupSoundDelay = 4;
                    }
                }

                // Countdown ETA: speed = distDecrease / timeSinceLastParticle
                if (mc.thePlayer.fishEntity != null && mc.thePlayer.fishEntity.getEntityId() == hookId) {
                    long timeDiff = now - chain.lastUpdate;
                    double distDecrease = oldDist - newDist;
                    if (timeDiff > 0 && distDecrease > 0) {
                        double speedBlocksPerMs = distDecrease / timeDiff;
                        countdownEtaMs = now + (long) (newDist / speedBlocksPerMs);
                    }
                }

                chain.distances.put(hookId, newDist);
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
            List<Integer> toUse = !possible1.isEmpty() ? possible1 : possible2;
            double useAngle = !possible1.isEmpty() ? angle1 : angle2;
            if (toUse.isEmpty()) return;

            WakeChain chain = new WakeChain(now, useAngle);
            for (int hookId : toUse) {
                EntityFishHook hook = hookEntities.get(hookId);
                if (hook == null || hook.isDead) continue;
                double dx = hook.posX - x, dz = hook.posZ - z;
                chain.distances.put(hookId, Math.sqrt(dx * dx + dz * dz));
            }
            chains.put(chain, new java.util.concurrent.CopyOnWriteArrayList<>(toUse));
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;
        if (!Config.feature.fishing.alert.enabled) return;
        if (warningState == WarningState.NOTHING) return;
        if (isSlugWaiting()) return;

        String text;
        if (warningState == WarningState.HOOKED) {
            if (!biteAlertFired) return;
            text = "§c§l!!!";
        } else {
            // INCOMING state: ETA takes priority if enabled and valid, else fall back to text
            long remaining = countdownEtaMs - System.currentTimeMillis();
            boolean etaValid = remaining > 100 && remaining < 10_000;
            if (Config.feature.fishing.alert.biteEta && etaValid) {
                text = "§e§l" + String.format("%.1f", remaining / 1000.0) + "s";
            } else if (Config.feature.fishing.alert.incomingMessage) {
                text = "§e§lINCOMING";
            } else {
                return; // neither sub-option is enabled, nothing to show
            }
        }

        FontRenderer fr = mc.fontRendererObj;
        ScaledResolution res = new ScaledResolution(mc);
        GlStateManager.pushMatrix();
        GlStateManager.scale(2.0, 2.0, 2.0);
        int x = (res.getScaledWidth() / 4) - (fr.getStringWidth(text) / 2);
        int y = (res.getScaledHeight() / 4) + 10;
        fr.drawStringWithShadow(text, x, y, 0xFFFFFF);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        hookEntities.clear();
        chains.clear();
        warningState = WarningState.NOTHING;
        hookedStateTicks = 0;
        countdownEtaMs = 0;
        slugLavaEntryMs = 0;
        awaitingSplashUntilMs = 0;
        splashConfirmedMs = 0;
        biteAlertFired = false;
        lastProcessedPacket = null;
    }

    private boolean isSlugWaiting() {
        return Config.feature.fishing.alert.slugMode && slugLavaEntryMs > 0 && System.currentTimeMillis() - slugLavaEntryMs < SLUG_DELAY_MS;
    }

    private double calcAngle(double xOff, double zOff) {
        double aX = Math.toDegrees(Math.acos(xOff / 0.04));
        double aZ = Math.toDegrees(Math.asin(zOff / 0.04));
        if (xOff < 0) aZ = 180 - aZ;
        if (zOff < 0) aX = 360 - aX;
        aX = ((aX % 360) + 360) % 360;
        aZ = ((aZ % 360) + 360) % 360;
        double d = aX - aZ;
        if (d < -180) d += 360;
        if (d > 180) d -= 360;
        return aZ + d / 2.0;
    }

    private boolean angleWithinRange(double a, double b, double range) {
        double d = Math.abs(a - b);
        if (d > 180) d = 360 - d;
        return d <= range;
    }

    private HookResult classifyHook(EntityFishHook hook, double px, double py, double pz, double a1, double a2) {
        double dY = py - hook.posY;
        double tolerance = 0.5;
        if (mc.theWorld != null) {
            for (int i = -2; i < 2; i++) {
                net.minecraft.block.Block b = mc.theWorld.getBlockState(new BlockPos(px, py + i, pz)).getBlock();
                if (b == Blocks.flowing_lava || b == Blocks.flowing_water || b == Blocks.lava) {
                    tolerance = 2.0;
                    break;
                }
            }
        }
        if (Math.abs(dY) > tolerance) return HookResult.NOT_POSSIBLE;

        double dX = px - hook.posX, dZ = pz - hook.posZ;
        double dist = Math.sqrt(dX * dX + dZ * dZ);
        if (dist < 0.2) return HookResult.EITHER;

        float allowance = (float) Math.toDegrees(Math.atan2(0.03125, dist)) * 1.5f;
        float hookAngle = (float) Math.toDegrees(Math.atan2(dX, dZ));
        hookAngle = ((hookAngle % 360) + 360) % 360;

        if (angleWithinRange(a1, hookAngle, allowance)) return HookResult.ANGLE1;
        if (angleWithinRange(a2, hookAngle, allowance)) return HookResult.ANGLE2;
        return HookResult.NOT_POSSIBLE;
    }

    private float calcPitch(float d) {
        d = Math.max(0.1f, Math.min(d, MAX_DIST));
        return 1f / (d + (1f / (ZERO_PITCH - MAX_PITCH))) * (1f - d / MAX_DIST) + MAX_PITCH;
    }

    public enum WarningState {NOTHING, INCOMING, HOOKED}

    private enum HookResult {NOT_POSSIBLE, EITHER, ANGLE1, ANGLE2}

    private static class WakeChain {
        final HashMap<Integer, Double> distances = new HashMap<>();
        int particleNum = 0;
        long lastUpdate;
        double currentAngle;

        WakeChain(long lastUpdate, double angle) {
            this.lastUpdate = lastUpdate;
            this.currentAngle = angle;
        }
    }
}
