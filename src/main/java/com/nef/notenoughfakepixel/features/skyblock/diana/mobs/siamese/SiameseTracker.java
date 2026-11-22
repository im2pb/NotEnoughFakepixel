/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.mobs.siamese;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.diana.burrows.ParticleProcessor;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.MobDisplayTypes;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;

import java.awt.Color;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Owns Siamese-pair discovery, target selection, pruning and hitbox rendering. Client-thread only. */
public final class SiameseTracker {
    private final Queue<SiameseLynx> alive = new ConcurrentLinkedQueue<>();
    /** Avoids walking every tracked pair for every armor stand on each scan. */
    private final Map<UUID, SiameseLynx> byEntityId = new ConcurrentHashMap<>();

    /** Tracks one armor stand. Only newly discovered Siamese members need a pair lookup. */
    public void trackEntity(Entity entity, boolean enabled) {
        if (!enabled || !(entity instanceof EntityArmorStand) || entity.isDead) return;
        UUID entityId = entity.getUniqueID();
        if (byEntityId.containsKey(entityId) || entity.getName() == null) return;
        EntityArmorStand armorStand = (EntityArmorStand) entity;
        String displayName = armorStand.getDisplayName().getUnformattedText();
        if (!displayName.contains("Bagheera") && !displayName.contains("Azrael")) return;

        for (SiameseLynx pair : alive) {
            if (pair.getEntity2() != null || pair.getEntity1() == null) continue;
            if (pair.getEntity1().getUniqueID().equals(entityId)) return;
            pair.setEntity2(entity);
            byEntityId.put(entityId, pair);
            return;
        }

        SiameseLynx pair = new SiameseLynx(entity);
        alive.add(pair);
        byEntityId.put(entityId, pair);
    }

    public Entity getClosest(int[] coords) {
        if (coords == null || coords.length < 3) return null;
        Entity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (SiameseLynx pair : alive) {
            Entity first = pair.getEntity1();
            if (isUsable(first)) {
                double distance = squaredDistance(coords, first);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = first;
                }
            }
            Entity second = pair.getEntity2();
            if (isUsable(second)) {
                double distance = squaredDistance(coords, second);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = second;
                }
            }
        }
        return closest;
    }

    public void markClosestHittable(int[] coords) {
        Entity closest = getClosest(coords);
        if (closest == null) return;
        SiameseLynx pair = byEntityId.get(closest.getUniqueID());
        if (pair != null) pair.setHittable(closest);
    }

    public void prune(EntityPlayerSP player, ParticleProcessor processor) {
        if (player == null || processor == null) return;
        final int playerX = (int) Math.floor(player.posX);
        final int playerY = (int) Math.floor(player.posY);
        final int playerZ = (int) Math.floor(player.posZ);
        final int maxDistanceSquared = 64 * 64;

        alive.removeIf(pair -> {
            Entity first = pair.getEntity1();
            if (first != null && !isInRange(first, playerX, playerY, playerZ, maxDistanceSquared)) {
                byEntityId.remove(first.getUniqueID(), pair);
                pair.setEntity1(null);
            }

            Entity second = pair.getEntity2();
            if (second != null && !isInRange(second, playerX, playerY, playerZ, maxDistanceSquared)) {
                byEntityId.remove(second.getUniqueID(), pair);
                pair.setEntity2(null);
            }
            return pair.getEntity1() == null && pair.getEntity2() == null;
        });
    }

    /** Renders once per frame, rather than once for every loaded ocelot entity. */
    public void renderTracked(float partialTicks) {
        Color base = null;
        for (SiameseLynx pair : alive) {
            Entity target = pair.getHittable();
            if (!isUsable(target)) continue;
            if (base == null) base = ColorUtils.getColor(Config.feature.diana.dianaSiameseHittableColor);
            Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), 150);
            RenderUtils.renderEntityHitbox(target, partialTicks, color, MobDisplayTypes.SIAMESE);
        }
    }

    /** Compatibility shim for callers from older integrations. */
    public void renderForOcelot(Entity entity, float partialTicks) {
        if (entity != null) renderTracked(partialTicks);
    }

    public void clear() {
        alive.clear();
        byEntityId.clear();
    }

    public Queue<SiameseLynx> getAlive() {
        return alive;
    }

    private static boolean isUsable(Entity entity) {
        return entity != null && !entity.isDead && entity.worldObj != null;
    }

    private static boolean isInRange(Entity entity, int px, int py, int pz, int maxDistanceSquared) {
        if (!isUsable(entity)) return false;
        int dx = (int) Math.floor(entity.posX) - px;
        int dy = (int) Math.floor(entity.posY) - py;
        int dz = (int) Math.floor(entity.posZ) - pz;
        return dx * dx + dy * dy + dz * dz < maxDistanceSquared;
    }

    private static double squaredDistance(int[] coords, Entity entity) {
        double dx = coords[0] - entity.posX;
        double dy = coords[1] - entity.posY;
        double dz = coords[2] - entity.posZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
