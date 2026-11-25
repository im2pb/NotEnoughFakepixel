package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;

@RegisterEvents
public class WitherBloodKeysTracers {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final String WITHER_KEY_NAME = "§8Wither key";
    private static final String BLOOD_KEY_NAME = "§cBlood key";

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!Config.feature.dungeons.general.dungeonsKeyTracers) return;
        if (!SkyblockData.getCurrentLocation().isDungeon()) return;
        if (MC.theWorld == null || MC.thePlayer == null) return;

        final Vec3 eyePos = MC.thePlayer.getPositionEyes(event.partialTicks);
        for (Object obj : MC.theWorld.loadedEntityList) {
            if (!(obj instanceof EntityArmorStand)) continue;

            EntityArmorStand entity = (EntityArmorStand) obj;
            String name = entity.getName();
            if (name == null) continue;

            Color color;
            if (WITHER_KEY_NAME.equals(name)) {
                color = Color.BLACK;
            } else if (BLOOD_KEY_NAME.equals(name)) {
                color = Color.RED;
            } else {
                continue;
            }

            RenderUtils.draw3DLine(
                    entity.posX, entity.posY + 1.75D, entity.posZ,
                    eyePos.xCoord, eyePos.yCoord, eyePos.zCoord,
                    color, 8, true, event.partialTicks
            );
        }
    }
}
