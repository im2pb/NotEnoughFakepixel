/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.mobs;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.MobDisplayTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityBat;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.List;

@RegisterEvents
public class BatMobDisplay implements GatedListener {

    @Override
    public boolean isListenerActive() {
        return Config.feature.dungeons.dungeonsBatMobs && SkyblockData.getCurrentLocation().isDungeon();
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!Config.feature.dungeons.dungeonsBatMobs) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;
        if (Minecraft.getMinecraft().theWorld == null) return;
        if (!SkyblockData.getCurrentLocation().isDungeon()) return;

        WorldClient world = Minecraft.getMinecraft().theWorld;
        List<Entity> entities = world.loadedEntityList;

        Color color = null;
        for (int i = 0, n = entities.size(); i < n; i++) {
            Entity entity = entities.get(i);
            if (!(entity instanceof EntityBat)) continue;
            if (entity.getName() == null) continue;

            if (color == null) color = new Color(ColorUtils.getColor(Config.feature.dungeons.dungeonsBatColor).getRGB()); 
            RenderUtils.renderEntityHitbox(entity, event.partialTicks, color, MobDisplayTypes.BAT);
        }
    }

}
