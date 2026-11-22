/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.mobs;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.RenderEntityModelEvent;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.OutlineUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.utils.SkyblockGate;
import com.nef.notenoughfakepixel.variables.MobDisplayTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RegisterEvents
public final class DungeonBloodMobHighlight {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Set<String> BLOOD_NAMES = new HashSet<>(Arrays.asList(
            "angry archaeologist",
            "lost adventurer",
            "frozen adventurer",
            "diamond guy",
            "shadow assassin",
            "king midas"
    ));
    private final List<EntityLivingBase> cachedMobs = new ArrayList<>();
    private int tickCounter;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.feature.dungeons.dungeonsBloodMobHighlight || !SkyblockGate.isFakepixelDungeon() || MC.theWorld == null) {
            cachedMobs.clear();
            tickCounter = 0;
            return;
        }
        if (++tickCounter < 5) return;
        tickCounter = 0;
        rebuildCache();
    }

    private void rebuildCache() {
        cachedMobs.clear();
        for (Object object : MC.theWorld.loadedEntityList) {
            if (!(object instanceof EntityLivingBase)) continue;
            EntityLivingBase entity = (EntityLivingBase) object;
            if (entity == MC.thePlayer || entity.isDead) continue;
            if (isBloodMob(entity.getName())) cachedMobs.add(entity);
        }

        for (Object object : MC.theWorld.loadedEntityList) {
            if (!(object instanceof EntityArmorStand)) continue;
            EntityArmorStand armorStand = (EntityArmorStand) object;
            String label = StringUtils.stripControlCodes(armorStand.getName() == null ? "" : armorStand.getName()).trim().toLowerCase(Locale.ROOT);
            if (!isBloodMob(label)) continue;
            EntityLivingBase nearest = null;
            double nearestDistance = 0.8D * 0.8D;
            for (Object candidate : MC.theWorld.loadedEntityList) {
                if (!(candidate instanceof EntityLivingBase) || candidate instanceof EntityArmorStand) continue;
                EntityLivingBase living = (EntityLivingBase) candidate;
                if (living == MC.thePlayer || living.isDead) continue;
                double distance = living.getDistanceSqToEntity(armorStand);
                if (distance <= nearestDistance) {
                    nearestDistance = distance;
                    nearest = living;
                }
            }
            if (nearest != null && !cachedMobs.contains(nearest)) cachedMobs.add(nearest);
        }
    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event) {
        if (!Config.feature.dungeons.dungeonsBloodMobHighlight || !SkyblockGate.isFakepixelDungeon()) return;
        Color color = ColorUtils.getColor(Config.feature.dungeons.dungeonsBloodMobHighlightColor);
        for (EntityLivingBase entity : cachedMobs) {
            if (entity == null || entity.isDead) continue;
            if (Config.feature.dungeons.dungeonsBloodMobHighlightRender == 0) {
                RenderUtils.renderEntityHitbox(entity, event.partialTicks, color, MobDisplayTypes.NONE);
            }
        }
    }

    @SubscribeEvent
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        if (!Config.feature.dungeons.dungeonsBloodMobHighlight || Config.feature.dungeons.dungeonsBloodMobHighlightRender != 1) return;
        EntityLivingBase entity = event.getEntity();
        if (entity == null || !cachedMobs.contains(entity)) return;
        OutlineUtils.outlineEntity(event, 3.0f, ColorUtils.getColor(Config.feature.dungeons.dungeonsBloodMobHighlightColor), true);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            cachedMobs.clear();
            tickCounter = 0;
        }
    }

    public List<EntityLivingBase> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(cachedMobs));
    }

    private static boolean isBloodMob(String name) {
        String clean = StringUtils.stripControlCodes(name == null ? "" : name).trim().toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) return false;
        for (String known : BLOOD_NAMES) {
            if (clean.equals(known) || clean.startsWith(known + " ")) return true;
        }
        return false;
    }
}
