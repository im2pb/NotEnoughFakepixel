/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.mobs;

import com.nef.notenoughfakepixel.features.skyblock.diana.burrows.ParticleProcessor;
import com.nef.notenoughfakepixel.features.skyblock.diana.mobs.gaia.GaiaTracker;
import com.nef.notenoughfakepixel.features.skyblock.diana.mobs.siamese.SiameseTracker;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGolem;

import java.util.List;

/** Coordinates Diana mob discovery using one bounded-frequency loaded-entity snapshot. Client-thread only. */
public final class DianaMobTrackingService {
    private final GaiaTracker gaiaTracker;
    private final SiameseTracker siameseTracker;

    public DianaMobTrackingService(GaiaTracker gaiaTracker, SiameseTracker siameseTracker) {
        if (gaiaTracker == null || siameseTracker == null) {
            throw new IllegalArgumentException("Diana mob trackers cannot be null");
        }
        this.gaiaTracker = gaiaTracker;
        this.siameseTracker = siameseTracker;
    }

    /**
     * Called every few client ticks, not every rendered frame. Both trackers share one entity-list traversal.
     */
    public void update(WorldClient world, EntityPlayerSP player, ParticleProcessor processor,
                       boolean gaiaEnabled, boolean siameseEnabled) {
        if (world == null || player == null || processor == null) {
            clear();
            return;
        }

        if (!gaiaEnabled && !siameseEnabled) {
            clear();
            return;
        }

        // This callback is client-thread only, so iterate the live world list once without allocating a copy.
        List<Entity> loadedEntities = world.loadedEntityList;
        final double maxDistanceSquared = 64.0D * 64.0D;
        for (Entity entity : loadedEntities) {
            if (entity == null || entity.isDead) continue;
            boolean gaiaCandidate = gaiaEnabled && entity instanceof EntityGolem;
            boolean siameseCandidate = siameseEnabled && entity instanceof EntityArmorStand;
            if (!gaiaCandidate && !siameseCandidate) continue;

            double dx = entity.posX - player.posX;
            double dy = entity.posY - player.posY;
            double dz = entity.posZ - player.posZ;
            if (dx * dx + dy * dy + dz * dz >= maxDistanceSquared) continue;
            if (gaiaCandidate) gaiaTracker.trackEntity(entity, true);
            if (siameseCandidate) siameseTracker.trackEntity(entity, true);
        }

        if (gaiaEnabled) gaiaTracker.prune(player, processor);
        else gaiaTracker.clear();

        if (siameseEnabled) siameseTracker.prune(player, processor);
        else siameseTracker.clear();
    }

    /** Draws only entities already discovered by the periodic scan; never walks the world entity list. */
    public void render(float partialTicks, boolean gaiaEnabled, boolean siameseEnabled) {
        if (gaiaEnabled) gaiaTracker.renderTracked(partialTicks);
        if (siameseEnabled) siameseTracker.renderTracked(partialTicks);
    }

    public void clear() {
        gaiaTracker.clear();
        siameseTracker.clear();
    }
}
