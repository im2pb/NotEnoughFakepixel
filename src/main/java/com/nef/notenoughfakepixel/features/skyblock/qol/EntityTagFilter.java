/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

@RegisterEvents
public final class EntityTagFilter implements GatedListener {

    private static final Pattern DAMAGE_TAG = Pattern.compile(
            "^[\\u2727\\u272F\\u2726\\u2694\\u2741\\u270E\\u2620\\u265E\\s]*[0-9][0-9,.]*[kKmMbBtT]?[\\u2727\\u272F\\u2726\\u2694\\u2741\\u270E\\u2620\\u265E\\s]*$");

    private static final Pattern LIST_SEPARATOR = Pattern.compile("[,;\\n]");
    private static final int MAX_DEBUG_NAMES = 200;

    private static volatile Boolean damageTagsOverride;

    private static String parsedSource = "";
    private static String[] containsEntries = new String[0];
    private static Set<String> exactEntries = new HashSet<>();
    private static int listVersion;

    private static final class TagInfo {
        String name;
        int listVersion = -1;
        boolean damageTag;
        boolean listed;
    }

    private static final WeakHashMap<Entity, TagInfo> CACHE = new WeakHashMap<>();
    private static final Set<String> DEBUG_SEEN = new HashSet<>();

    public static boolean isHidingDamageTags() {
        Boolean override = damageTagsOverride;
        return override != null ? override : Config.feature.qol.qolHideDamageTags;
    }

    public static boolean toggleDamageTags() {
        boolean next = !isHidingDamageTags();
        damageTagsOverride = next;
        return next;
    }

    /**
     * True when the invisible armor stand is only a floating damage-number tag.
     * The render-manager hook uses this to skip the complete stand render (model,
     * layers, shadow and name), rather than merely cancelling its label.
     */
    public static boolean shouldSkipDamageTagModel(Entity entity) {
        if (!(entity instanceof EntityArmorStand) || Config.feature == null || Config.feature.qol == null
                || !isHidingDamageTags()) return false;

        if (SkyblockData.getCurrentGamemode() == null || !SkyblockData.getCurrentGamemode().isSkyblock()) return false;

        EntityArmorStand stand = (EntityArmorStand) entity;
        if (!stand.isInvisible() || !stand.hasCustomName()) return false;

        String name = stand.getCustomNameTag();
        if (name == null || name.isEmpty()) return false;

        TagInfo info = CACHE.get(entity);
        if (info != null && name.equals(info.name)) return info.damageTag;

        String plain = StringUtils.stripControlCodes(name);
        boolean match = plain != null && plain.length() <= 20 && DAMAGE_TAG.matcher(plain.trim()).matches();
        if (info == null) {
            info = new TagInfo();
            CACHE.put(entity, info);
        }
        info.name = name;
        info.damageTag = match;
        return match;
    }

    @Override
    public boolean isListenerActive() {
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return false;
        if (Config.feature.qol.qolHideNameTags || isHidingDamageTags()) return true;
        return Config.feature.qol.qolHideListedNames && refreshList();
    }

    @Override
    public void onListenerDeactivated() {
        CACHE.clear();
        DEBUG_SEEN.clear();
    }

    private static boolean refreshList() {
        String source = Config.feature.qol.qolHiddenNamesList;
        if (source == null) source = "";
        if (!source.equals(parsedSource)) {
            parsedSource = source;
            List<String> contains = new ArrayList<>();
            Set<String> exact = new HashSet<>();
            for (String raw : LIST_SEPARATOR.split(source)) {
                String entry = StringUtils.stripControlCodes(raw).trim();
                boolean whole = entry.startsWith("=");
                if (whole) entry = entry.substring(1).trim();
                if (entry.isEmpty()) continue;
                entry = entry.toLowerCase(Locale.ROOT);
                if (whole) exact.add(entry);
                else contains.add(entry);
            }
            containsEntries = contains.toArray(new String[0]);
            exactEntries = exact;
            listVersion++;
        }
        return containsEntries.length > 0 || !exactEntries.isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderSpecials(RenderLivingEvent.Specials.Pre<EntityLivingBase> event) {
        EntityLivingBase entity = event.entity;
        if (entity == null || entity == Minecraft.getMinecraft().thePlayer) return;

        boolean player = entity instanceof EntityPlayer;
        
        if (!player && !entity.hasCustomName()) return;

        boolean wantDamage = isHidingDamageTags() && entity instanceof EntityArmorStand;
        boolean wantList = Config.feature.qol.qolHideListedNames && refreshList();
        if (wantDamage || wantList) {
            TagInfo info = classify(entity, player, wantDamage, wantList);
            if ((wantDamage && info.damageTag) || (wantList && info.listed)) {
                event.setCanceled(true);
                return;
            }
        }

        if (Config.feature.qol.qolHideNameTags) {
            if (player && Config.feature.qol.qolKeepPlayerNameTags) return;
            event.setCanceled(true);
        }
    }

    private static TagInfo classify(EntityLivingBase entity, boolean player, boolean wantDamage, boolean wantList) {
        String name = player ? entity.getName() : entity.getCustomNameTag();
        TagInfo info = CACHE.get(entity);
        if (info == null) {
            info = new TagInfo();
            CACHE.put(entity, info);
        }
        if ((name == null ? info.name == null : name.equals(info.name)) && info.listVersion == listVersion) return info;

        info.name = name;
        info.listVersion = listVersion;
        info.damageTag = false;
        info.listed = false;
        if (name == null || name.isEmpty()) return info;

        String plain = StringUtils.stripControlCodes(name);
        if (plain == null) return info;
        plain = plain.trim();
        if (plain.isEmpty()) return info;

        if (entity instanceof EntityArmorStand) info.damageTag = isDamageTag((EntityArmorStand) entity, plain);
        info.listed = matchesList(plain);
        return info;
    }

    private static boolean matchesList(String plain) {
        String lower = plain.toLowerCase(Locale.ROOT);
        if (exactEntries.contains(lower)) return true;
        for (String entry : containsEntries) {
            if (lower.contains(entry)) return true;
        }
        return false;
    }

    private static boolean isDamageTag(EntityArmorStand stand, String plain) {
        if (!stand.isInvisible() || plain.length() > 20) return false;

        boolean match = DAMAGE_TAG.matcher(plain).matches();
        if (!match && plain.length() <= 12 && DEBUG_SEEN.size() < MAX_DEBUG_NAMES && DEBUG_SEEN.add(plain)) {
            
            Logger.logConsole("[NEF Tags] short armor stand name not treated as damage tag: \"" + plain + "\"");
        }
        return match;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == null || !event.world.isRemote) return;
        CACHE.clear();
        DEBUG_SEEN.clear();
    }

    static void onWorldChanged() {
        if (Config.feature != null && Config.feature.qol.qolDamageTagsResetOnWorldChange) {
            damageTagsOverride = null;
        }
    }
}
