/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.map.DungeonMapRenderer;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.map.DungeonPlayerTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;

@RegisterEvents
public final class DungeonsMap {
    private static DungeonsMap instance;
    private final Minecraft mc = Minecraft.getMinecraft();

    private static final int MAP_POLL_TICKS = 2;

    private MapData cachedMapData;
    private World cachedWorld;
    private int cachedMapId = Integer.MIN_VALUE;
    private long lastDecorationSignature = Long.MIN_VALUE;
    private boolean mapContextActive;
    private int tickCounter;

    public DungeonsMap() {
        instance = this;
    }

    public static int getCachedMapId() {
        return instance == null ? Integer.MIN_VALUE : instance.cachedMapId;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        mapContextActive = Config.feature.dungeons.dungeonsMap
                && DungeonManager.checkEssentials()
                && mc.thePlayer != null
                && mc.theWorld != null;
        if (!mapContextActive) {
            clearMapState();
            return;
        }

        if (++tickCounter < MAP_POLL_TICKS) return;
        tickCounter = 0;

        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(8);
        if (stack == null || !(stack.getItem() instanceof ItemMap)) {
            clearMapState();
            return;
        }

        MapData data = getMapData(stack);
        if (data == null) return;

        if (shouldUpdatePlayerNames() && data.mapDecorations != null && !data.mapDecorations.isEmpty()) {
            long signature = decorationSignature(data.mapDecorations);
            if (signature != lastDecorationSignature) {
                DungeonPlayerTracker.updateMapDecorations(data.mapDecorations);
                lastDecorationSignature = signature;
            }
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!Config.feature.dungeons.dungeonsMap || !mapContextActive
                || mc.thePlayer == null || mc.theWorld == null) return;

        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(8);
        if (stack == null || !(stack.getItem() instanceof ItemMap)) return;

        MapData data = getMapData(stack);
        if (data == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float scale = getMapScale();
        float size = 128F * scale;
        float x = Config.feature.dungeons.dungeonsMapPos.getAbsX(sr, Math.round(size));
        float y = Config.feature.dungeons.dungeonsMapPos.getAbsY(sr, Math.round(size));
        if (Config.feature.dungeons.dungeonsMapPos.isCenterX()) x -= size / 2F;
        if (Config.feature.dungeons.dungeonsMapPos.isCenterY()) y -= size / 2F;

        DungeonMapRenderer.render(data, x, y, scale);
    }

    private MapData getMapData(ItemStack stack) {
        World world = mc.theWorld;
        MapData data = ((ItemMap) stack.getItem()).getMapData(stack, world);
        if (data != cachedMapData || cachedWorld != world) {
            cachedMapData = data;
            cachedWorld = world;
            lastDecorationSignature = Long.MIN_VALUE;
            if (data == null) {
                DungeonPlayerTracker.clearStatic();
            }
        }
        cachedMapId = stack.getItemDamage();
        return data;
    }

    private boolean shouldUpdatePlayerNames() {
        return DungeonPlayerTracker.shouldShowPlayerNames();
    }

    private static long decorationSignature(Map<String, net.minecraft.util.Vec4b> decorations) {
        long hash = 1125899906842597L;
        for (Map.Entry<String, net.minecraft.util.Vec4b> entry : decorations.entrySet()) {
            net.minecraft.util.Vec4b value = entry.getValue();
            if (value == null) continue;
            hash = 31L * hash + entry.getKey().hashCode();
            hash = 31L * hash + value.func_176110_a();
            hash = 31L * hash + value.func_176112_b();
            hash = 31L * hash + value.func_176113_c();
            hash = 31L * hash + value.func_176111_d();
        }
        return hash;
    }

    private void clearMapState() {
        cachedMapData = null;
        cachedWorld = null;
        cachedMapId = Integer.MIN_VALUE;
        lastDecorationSignature = Long.MIN_VALUE;
        mapContextActive = false;
        tickCounter = 0;
        DungeonPlayerTracker.clearStatic();
        DungeonMapRenderer.reset();
    }

    private static float getMapScale() {
        float scale = Config.feature.dungeons.dungeonsMapScale;
        return Float.isFinite(scale) && scale > 0F ? scale : 1F;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == null || !event.world.isRemote) return;
        clearMapState();
    }
}
