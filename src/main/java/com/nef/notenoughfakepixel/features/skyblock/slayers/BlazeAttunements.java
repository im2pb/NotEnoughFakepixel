/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.RenderEntityModelEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.EntityHighlightUtils;
import com.nef.notenoughfakepixel.utils.OutlineUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;

@RegisterEvents
public class BlazeAttunements implements GatedListener {
    private static final long CACHE_TTL_NANOS = 250_000_000L;
    private static final Map<EntityLivingBase, CacheEntry> ATTUNEMENT_CACHE = new WeakHashMap<>();
    private static final Map<String, Color> ATTUNEMENT_COLORS;
    private static volatile boolean outlineEnabledCached;
    private static volatile boolean fillEnabledCached;
    private static volatile boolean crimsonLocationCached;
    private static World cachedWorld;

    static {
        Map<String, Color> colors = new HashMap<>();
        colors.put("ASHEN", Color.DARK_GRAY);
        colors.put("SPIRIT", Color.WHITE);
        colors.put("CRYSTAL", Color.CYAN);
        colors.put("AURIC", Color.YELLOW);
        ATTUNEMENT_COLORS = Collections.unmodifiableMap(colors);
    }

    @Override
    public boolean isListenerActive() {
        refreshFeatureSettings();
        return outlineEnabledCached || fillEnabledCached;
    }

    @Override
    public void onListenerDeactivated() {
        outlineEnabledCached = false;
        fillEnabledCached = false;
        crimsonLocationCached = false;
        clearCache();
    }

    /** Config and location state are refreshed on client ticks, not once per rendered entity. */
    private static void refreshFeatureSettings() {
        boolean master = Config.feature != null
                && Config.feature.slayer != null
                && Config.feature.slayer.slayerBlazeAttunements;
        outlineEnabledCached = master && Config.feature.slayer.slayerBlazeAttunementOutline;
        fillEnabledCached = master && Config.feature.slayer.slayerBlazeAttunementFill;
    }

    public static boolean isOutlineEnabled() {
        return outlineEnabledCached;
    }

    public static boolean isFillEnabled() {
        return fillEnabledCached;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world != cachedWorld) {
            clearCache();
            cachedWorld = world;
        }

        boolean inCrimson = world != null && mc.thePlayer != null && isInCrimsonSkyBlock();
        if (!inCrimson && crimsonLocationCached) {
            clearCacheForCurrentWorld();
        }
        crimsonLocationCached = inCrimson;
    }

    @SubscribeEvent
    public void onRenderEntity(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!crimsonLocationCached) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        EntityLivingBase entity = event.entity;
        if (entity == null || entity == mc.thePlayer || entity.isInvisible() || !isValidMinionType(entity)) {
            return;
        }

        long now = System.nanoTime();
        synchronized (ATTUNEMENT_CACHE) {
            CacheEntry cached = ATTUNEMENT_CACHE.get(entity);
            if (cached != null && now < cached.expiresAtNanos) return;
        }

        String attunement = findAttunement(mc.theWorld, entity);
        synchronized (ATTUNEMENT_CACHE) {
            ATTUNEMENT_CACHE.put(entity, new CacheEntry(attunement, now + CACHE_TTL_NANOS));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (!outlineEnabledCached || !crimsonLocationCached) return;

        EntityLivingBase entity = event.getEntity();
        Color color = getAttunementColor(entity);
        if (color == null) return;
        if (Configuration.isPojav()) {
            EntityHighlightUtils.renderEntityOutline(event, color);
        } else {
            OutlineUtils.outlineEntity(event, 6.0f, color, true);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world == cachedWorld) {
            clearCache();
            crimsonLocationCached = false;
        }
    }

    public static String getAttunement(EntityLivingBase entity) {
        if (entity == null) return null;
        long now = System.nanoTime();
        synchronized (ATTUNEMENT_CACHE) {
            CacheEntry entry = ATTUNEMENT_CACHE.get(entity);
            if (entry == null) return null;
            if (now >= entry.expiresAtNanos) {
                ATTUNEMENT_CACHE.remove(entity);
                return null;
            }
            return entry.attunement;
        }
    }

    public static boolean hasAttunement(EntityLivingBase entity) {
        return getAttunement(entity) != null;
    }

    public static Color getAttunementColor(EntityLivingBase entity) {
        String attunement = getAttunement(entity);
        return attunement == null ? null : ATTUNEMENT_COLORS.get(attunement);
    }

    private static boolean isInCrimsonSkyBlock() {
        return SkyblockData.getCurrentGamemode() != null
                && SkyblockData.getCurrentGamemode().isSkyblock()
                && SkyblockData.getCurrentLocation() != null
                && SkyblockData.getCurrentLocation().isCrimson();
    }

    private static boolean isValidMinionType(EntityLivingBase entity) {
        return entity instanceof EntityBlaze
                || entity instanceof EntityPigZombie
                || (entity instanceof EntitySkeleton && ((EntitySkeleton) entity).getSkeletonType() == 1);
    }

    private static String findAttunement(World world, EntityLivingBase entity) {
        List<Entity> armorStands = world.getEntitiesWithinAABB(
                EntityArmorStand.class,
                entity.getEntityBoundingBox().offset(0, 2.0, 0).expand(1.0, 1.0, 1.0)
        );

        for (Entity armorStand : armorStands) {
            if (!(armorStand instanceof EntityArmorStand) || !armorStand.hasCustomName()) continue;
            String displayName = armorStand.getDisplayName().getUnformattedText();
            Matcher matcher = COLOR_PATTERN.matcher(displayName);
            if (!matcher.find()) continue;

            String attunement = matcher.group().toUpperCase(Locale.ROOT);
            if (isAllowedAttunement(entity, attunement)) return attunement;
        }
        return null;
    }

    private static boolean isAllowedAttunement(EntityLivingBase entity, String attunement) {
        if (entity instanceof EntitySkeleton && ((EntitySkeleton) entity).getSkeletonType() == 1) {
            return "SPIRIT".equals(attunement) || "CRYSTAL".equals(attunement);
        }
        if (entity instanceof EntityPigZombie) {
            return "ASHEN".equals(attunement) || "AURIC".equals(attunement);
        }
        return entity instanceof EntityBlaze;
    }

    private static void clearCacheForCurrentWorld() {
        synchronized (ATTUNEMENT_CACHE) {
            if (!ATTUNEMENT_CACHE.isEmpty()) ATTUNEMENT_CACHE.clear();
        }
    }

    private static void clearCache() {
        synchronized (ATTUNEMENT_CACHE) {
            ATTUNEMENT_CACHE.clear();
            cachedWorld = null;
        }
    }

    private static final class CacheEntry {
        final String attunement;
        final long expiresAtNanos;

        CacheEntry(String attunement, long expiresAtNanos) {
            this.attunement = attunement;
            this.expiresAtNanos = expiresAtNanos;
        }
    }

    public static final Pattern COLOR_PATTERN = Pattern.compile("ASHEN|SPIRIT|CRYSTAL|AURIC");

    public static int getColorForAttunement(String attunement) {
        if (attunement == null) return -1;
        Color color = ATTUNEMENT_COLORS.get(attunement);
        return color == null ? -1 : color.getRGB();
    }
}
