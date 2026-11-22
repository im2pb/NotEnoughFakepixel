/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.mobs.gaia;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.diana.burrows.ParticleProcessor;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.MobDisplayTypes;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGolem;

import java.awt.Color;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Owns Gaia discovery, hit-state lookup, pruning and hitbox rendering. Client-thread only. */
public final class GaiaTracker {
    private final Queue<GaiaConstruct> alive = new ConcurrentLinkedQueue<>();
    private final Map<UUID, GaiaConstruct> byId = new ConcurrentHashMap<>();

    /** Tracks one entity so Diana can share a single loaded-entity traversal across mob systems. */
    public void trackEntity(Entity entity, boolean enabled) {
        if (!enabled || entity == null || entity.isDead || !(entity instanceof EntityGolem)) return;
        UUID id = entity.getUniqueID();
        if (byId.containsKey(id)) return;
        if (entity.getName() == null) return;
        byId.computeIfAbsent(id, key -> {
            GaiaConstruct gaia = new GaiaConstruct(entity);
            alive.add(gaia);
            return gaia;
        });
    }

    public void prune(EntityPlayerSP player, ParticleProcessor processor) {
        if (player == null || processor == null) return;
        final int playerX = (int) Math.floor(player.posX);
        final int playerY = (int) Math.floor(player.posY);
        final int playerZ = (int) Math.floor(player.posZ);
        final int distanceRenderHitbox = 64;

        alive.removeIf(gaia -> {
            Entity entity = gaia.getEntity();
            if (entity == null || entity.isDead || !(entity instanceof EntityLivingBase)) {
                if (entity != null) byId.remove(entity.getUniqueID(), gaia);
                return true;
            }
            int dx = (int) Math.floor(entity.posX) - playerX;
            int dy = (int) Math.floor(entity.posY) - playerY;
            int dz = (int) Math.floor(entity.posZ) - playerZ;
            if (dx * dx + dy * dy + dz * dz >= distanceRenderHitbox * distanceRenderHitbox) {
                byId.remove(entity.getUniqueID(), gaia);
                return true;
            }
            return false;
        });
    }

    public GaiaConstruct getClosest(int[] coords) {
        if (coords == null) return null;
        GaiaConstruct closest = null;
        double closestDistanceSquared = Double.MAX_VALUE;
        for (GaiaConstruct gaia : alive) {
            Entity entity = gaia.getEntity();
            if (entity == null || entity.isDead) continue;
            double dx = coords[0] - entity.posX;
            double dy = coords[1] - entity.posY;
            double dz = coords[2] - entity.posZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
                closest = gaia;
            }
        }
        return closest;
    }

    public void remove(GaiaConstruct gaia) {
        if (gaia == null) return;
        alive.remove(gaia);
        Entity entity = gaia.getEntity();
        if (entity != null) byId.remove(entity.getUniqueID(), gaia);
    }

    /** Draws tracked Gaia constructs directly, without visiting every loaded entity each frame. */
    public void renderTracked(float partialTicks) {
        for (GaiaConstruct gaia : alive) {
            Entity entity = gaia.getEntity();
            if (entity == null || entity.isDead || !(entity instanceof EntityGolem)) continue;

            String configuredColor = gaia.canBeHit()
                    ? Config.feature.diana.dianaGaiaHittableColor
                    : Config.feature.diana.dianaGaiaUnhittableColor;
            Color base = ColorUtils.getColor(configuredColor);
            Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), 150);
            RenderUtils.renderEntityHitbox(entity, partialTicks, color, MobDisplayTypes.GAIA);
        }
    }

    /** Compatibility entry point for integrations that still render one entity at a time. */
    public void renderEntity(Entity entity, float partialTicks) {
        if (entity == null || entity.getName() == null || !(entity instanceof EntityGolem)) return;
        GaiaConstruct gaia = byId.get(entity.getUniqueID());
        if (gaia == null || gaia.getEntity() == null || gaia.getEntity().isDead) return;
        String configuredColor = gaia.canBeHit()
                ? Config.feature.diana.dianaGaiaHittableColor
                : Config.feature.diana.dianaGaiaUnhittableColor;
        Color base = ColorUtils.getColor(configuredColor);
        Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), 150);
        RenderUtils.renderEntityHitbox(gaia.getEntity(), partialTicks, color, MobDisplayTypes.GAIA);
    }

    public void clear() {
        alive.clear();
        byId.clear();
    }

    public Queue<GaiaConstruct> getAlive() {
        return alive;
    }
}
