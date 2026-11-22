/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.detection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tracks Minos Inquisitors using the same association behavior as NotEnoughFakepixel beta 42.
 * A named Armor Stand is associated with the first eligible living entity in its expanded AABB.
 * Once found, that entity stays tracked until it dies, leaves the world, or the world changes.
 */
public final class InquisitorOutlineTracker {
    private static final double HORIZONTAL_SEARCH = 1.5D;
    private static final double VERTICAL_SEARCH = 3.0D;
    private static final long SCAN_INTERVAL_NS = 250_000_000L;

    /* Entity instances are tracked by identity and are never discarded during a normal rescan. */
    private final Set<EntityLivingBase> trackedEntities = Collections.newSetFromMap(
            new IdentityHashMap<EntityLivingBase, Boolean>());
    private volatile long lastScanNs;
    private WorldClient trackedWorld;
    private boolean observedTrackedDeath;

    /** Run on the client thread. The 250ms throttle mirrors beta 42's throttled render scan. */
    public void scanIfDue(Minecraft mc, boolean enabled) {
        if (mc == null || mc.theWorld == null || mc.thePlayer == null || !enabled) {
            clear();
            return;
        }

        WorldClient world = mc.theWorld;
        if (trackedWorld != world) {
            trackedEntities.clear();
            trackedWorld = world;
            lastScanNs = 0L;
            observedTrackedDeath = false;
        }

        long now = System.nanoTime();
        if (now - lastScanNs < SCAN_INTERVAL_NS) return;
        lastScanNs = now;

        pruneTrackedEntities(world);

        // Keep the same discovery order and AABB association used by beta 42.
        // Do not exclude remote EntityPlayer/EntityOtherPlayerMP instances: some Hypixel
        // player-model mobs are represented by those classes and can also appear in tab.
        List<Entity> worldEntities = new ArrayList<Entity>(world.loadedEntityList);
        for (Entity entity : worldEntities) {
            if (!(entity instanceof EntityLivingBase) || entity instanceof EntityArmorStand
                    || entity == mc.thePlayer || entity.isDead || entity.worldObj != world) {
                continue;
            }

            EntityLivingBase living = (EntityLivingBase) entity;
            if (containsInquisitor(getSafeEntityName(living))) {
                trackedEntities.add(living);
            }
        }

        for (Entity entity : worldEntities) {
            if (!(entity instanceof EntityArmorStand)) continue;
            EntityArmorStand armorStand = (EntityArmorStand) entity;
            if (armorStand.isDead || armorStand.worldObj != world
                    || !containsInquisitor(getSafeEntityName(armorStand))) {
                continue;
            }

            EntityLivingBase associatedMob = findAssociatedMob(armorStand, mc);
            if (associatedMob != null) trackedEntities.add(associatedMob);
        }
    }

    /** Exact beta 42 association: first eligible living entity found in the expanded AABB. */
    private EntityLivingBase findAssociatedMob(EntityArmorStand armorStand, Minecraft mc) {
        if (armorStand == null || armorStand.worldObj == null || mc == null) return null;
        AxisAlignedBB searchBox = armorStand.getEntityBoundingBox().expand(
                HORIZONTAL_SEARCH, VERTICAL_SEARCH, HORIZONTAL_SEARCH);

        List<EntityLivingBase> nearby = armorStand.worldObj.getEntitiesWithinAABB(
                EntityLivingBase.class,
                searchBox,
                entity -> entity != null
                        && !(entity instanceof EntityArmorStand)
                        && entity != mc.thePlayer
        );
        return nearby.isEmpty() ? null : nearby.get(0);
    }

    private void pruneTrackedEntities(WorldClient world) {
        Iterator<EntityLivingBase> iterator = trackedEntities.iterator();
        while (iterator.hasNext()) {
            EntityLivingBase entity = iterator.next();
            if (entity == null || entity.isDead || entity.worldObj == null || entity.worldObj != world) {
                iterator.remove();
                observedTrackedDeath = true;
            }
        }
    }

    public static boolean containsInquisitor(String name) {
        if (name == null || name.isEmpty()) return false;
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        if (clean == null) clean = name;
        clean = clean.replace('\u00A0', ' ').replace('\u2007', ' ').replace('\u202F', ' ');
        clean = clean.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return clean.contains("minos inquisitor");
    }

    /** Reads custom, display, and vanilla names, preferring any value that contains the marker. */
    public static String getSafeEntityName(Entity entity) {
        if (entity == null) return "";
        String fallback = "";

        try {
            String customName = entity.getCustomNameTag();
            if (customName != null && !customName.isEmpty()) {
                if (containsInquisitor(customName)) return stripFormatting(customName);
                fallback = stripFormatting(customName);
            }
        } catch (Throwable ignored) { }

        try {
            if (entity.getDisplayName() != null) {
                String formatted = entity.getDisplayName().getFormattedText();
                if (containsInquisitor(formatted)) return stripFormatting(formatted);
                if (fallback.isEmpty()) fallback = stripFormatting(formatted);

                String unformatted = entity.getDisplayName().getUnformattedText();
                if (containsInquisitor(unformatted)) return stripFormatting(unformatted);
                if (fallback.isEmpty()) fallback = stripFormatting(unformatted);
            }
        } catch (Throwable ignored) { }

        try {
            String name = entity.getName();
            if (name != null && !name.isEmpty()) {
                if (containsInquisitor(name)) return stripFormatting(name);
                if (fallback.isEmpty()) fallback = stripFormatting(name);
            }
        } catch (Throwable ignored) { }

        return fallback;
    }

    private static String stripFormatting(String value) {
        if (value == null) return "";
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(value);
        return clean == null ? value : clean;
    }

    /** Rendering uses membership only, matching beta 42's isInq.contains(entity) behavior. */
    public boolean contains(EntityLivingBase entity) {
        return entity != null && trackedEntities.contains(entity);
    }

    /** Recognizes a tracked entity, a directly named entity, or the entity associated with a named stand. */
    public boolean isInquisitorEntity(EntityLivingBase entity) {
        if (entity == null) return false;
        if (trackedEntities.contains(entity) || containsInquisitor(getSafeEntityName(entity))) return true;
        if (entity.worldObj == null) return false;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return false;
        List<Entity> worldEntities = new ArrayList<Entity>(entity.worldObj.loadedEntityList);
        for (Entity candidate : worldEntities) {
            if (!(candidate instanceof EntityArmorStand)) continue;
            EntityArmorStand armorStand = (EntityArmorStand) candidate;
            if (armorStand.isDead || armorStand.worldObj != entity.worldObj
                    || !containsInquisitor(getSafeEntityName(armorStand))) {
                continue;
            }
            if (findAssociatedMob(armorStand, mc) == entity) return true;
        }
        return false;
    }

    /** Removes a dead target immediately without waiting for the next periodic prune. */
    public void forget(EntityLivingBase entity) {
        if (entity != null) trackedEntities.remove(entity);
    }

    /** Consumes a death observed by the periodic cache prune when Forge's death event was missed. */
    public boolean consumeObservedTrackedDeath() {
        boolean result = observedTrackedDeath;
        observedTrackedDeath = false;
        return result;
    }

    public Set<EntityLivingBase> getTrackedEntities() {
        Set<EntityLivingBase> snapshot = Collections.newSetFromMap(
                new IdentityHashMap<EntityLivingBase, Boolean>());
        snapshot.addAll(trackedEntities);
        return Collections.unmodifiableSet(snapshot);
    }

    public void clear() {
        trackedEntities.clear();
        trackedWorld = null;
        lastScanNs = 0L;
        observedTrackedDeath = false;
    }
}
