/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Separates real remote players from entities that only use a player model.
 * A remote entity is hidden only when it has a live player-list entry. NPCs and
 * player-model mobs normally have no such entry, so they remain renderable.
 */
public final class PlayerVisibility {
    private static final Minecraft MC = Minecraft.getMinecraft();
    // Treat the opening "[Lv" marker as sufficient; the closing bracket is not required.
    // Keep player-model mobs with names such as "[Lv100]" or even "[Lv200" visible.
    private static final Pattern LEVELLED_MOB_NAME = Pattern.compile("(?i)\\[Lv");
    private static final double LEVEL_TAG_HORIZONTAL_RADIUS_SQ = 1.0D;
    private static final double LEVEL_TAG_MIN_VERTICAL_OFFSET = -0.75D;
    private static final double LEVEL_TAG_MAX_VERTICAL_EXTRA = 2.75D;

    /* Armor-stand labels are cached once per world tick; render calls must not rescan the world. */
    private static World cachedLevelTagWorld;
    private static long cachedLevelTagTick = Long.MIN_VALUE;
    private static List<LevelTagMarker> cachedLevelTagMarkers = Collections.emptyList();

    private PlayerVisibility() {
    }

    public static boolean shouldHidePlayer(Entity entity) {
        if (Config.feature == null || Config.feature.qol == null
                || !Config.feature.qol.qolHidePlayers) return false;
        return isRealOnlinePlayer(entity);
    }

    public static boolean isRealOnlinePlayer(Entity entity) {
        boolean remotePlayerEntity = entity instanceof EntityOtherPlayerMP && entity != MC.thePlayer;
        if (!remotePlayerEntity || hasLevelledMobName(entity) || MC.getNetHandler() == null) return false;

        // Explicitly preserve the NPC format already recognized by the existing
        // Hide Players near NPCs feature, even if a server exposes a player-info entry.
        boolean recognizedNpc = HidePlayersNearNpcs.isNpc(entity);
        try {
            NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(entity.getUniqueID());
            return PlayerVisibilityPolicy.isRealOnlinePlayer(remotePlayerEntity, info != null, recognizedNpc);
        } catch (Throwable ignored) {
            // Failing closed is important: an entity we cannot classify is left visible.
            return false;
        }
    }

    /**
     * Level tags are part of the displayed mob name, not a player nickname.
     * Check both the custom name and rendered name because servers may expose
     * the same label through different Entity name methods.
     */
    private static boolean hasLevelledMobName(Entity entity) {
        if (entity == null) return false;

        // First check names attached directly to the player-model entity.
        if (hasLevelledLabel(entity)) return true;

        // Hypixel often puts the visible [Lv...] label on a separate Armor Stand
        // above the player-model mob. Read that marker and associate it by X/Z
        // alignment and height instead of assuming the mob itself owns the name.
        return hasNearbyLevelledArmorStand(entity);
    }

    private static boolean hasNearbyLevelledArmorStand(Entity entity) {
        final World world = entity.worldObj;
        if (world == null) return false;

        final List<LevelTagMarker> markers = getLevelTagMarkers(world);
        if (markers.isEmpty()) return false;

        final double maxVerticalOffset = Math.max(3.5D, entity.height + LEVEL_TAG_MAX_VERTICAL_EXTRA);
        for (LevelTagMarker marker : markers) {
            final double dx = marker.x - entity.posX;
            final double dz = marker.z - entity.posZ;
            if (dx * dx + dz * dz > LEVEL_TAG_HORIZONTAL_RADIUS_SQ) continue;

            final double verticalOffset = marker.y - entity.posY;
            if (verticalOffset < LEVEL_TAG_MIN_VERTICAL_OFFSET || verticalOffset > maxVerticalOffset) continue;

            return true;
        }
        return false;
    }

    /** Builds the marker snapshot at most once per world tick, not once per rendered entity. */
    private static List<LevelTagMarker> getLevelTagMarkers(World world) {
        final long worldTick = world.getTotalWorldTime();
        if (world == cachedLevelTagWorld && worldTick == cachedLevelTagTick) {
            return cachedLevelTagMarkers;
        }

        final List<LevelTagMarker> markers = new ArrayList<LevelTagMarker>();
        for (Object loaded : world.loadedEntityList) {
            if (!(loaded instanceof EntityArmorStand)) continue;
            final EntityArmorStand stand = (EntityArmorStand) loaded;
            if (stand.isDead || stand.worldObj != world || !hasLevelledLabel(stand)) continue;
            markers.add(new LevelTagMarker(stand.posX, stand.posY, stand.posZ));
        }

        cachedLevelTagWorld = world;
        cachedLevelTagTick = worldTick;
        cachedLevelTagMarkers = markers.isEmpty()
                ? Collections.<LevelTagMarker>emptyList() : markers;
        return cachedLevelTagMarkers;
    }

    /** Checks custom, entity, and rendered display names without relying on formatting codes. */
    private static boolean hasLevelledLabel(Entity entity) {
        if (entity == null) return false;
        try {
            if (containsLevelTag(entity.getCustomNameTag())) return true;
            if (containsLevelTag(entity.getName())) return true;
            if (entity.getDisplayName() != null) {
                if (containsLevelTag(entity.getDisplayName().getFormattedText())) return true;
                if (containsLevelTag(entity.getDisplayName().getUnformattedText())) return true;
            }
        } catch (Throwable ignored) {
            // Keep visibility conservative if a custom display-name implementation fails.
        }
        return false;
    }

    private static boolean containsLevelTag(String name) {
        if (name == null || name.isEmpty()) return false;
        String plainName = net.minecraft.util.StringUtils.stripControlCodes(name);
        return plainName != null && LEVELLED_MOB_NAME.matcher(plainName).find();
    }

    private static final class LevelTagMarker {
        final double x;
        final double y;
        final double z;

        LevelTagMarker(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
