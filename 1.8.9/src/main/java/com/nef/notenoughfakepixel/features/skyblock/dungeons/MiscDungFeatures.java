package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.RenderEntityModelEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;

@RegisterEvents
public class MiscDungFeatures {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Color SPIRIT_BOW_COLOR = Color.RED;
    private static final String BLOOD_READY_MESSAGE = "[BOSS] The Watcher: That will be enough for now.";
    private static final String SPIRIT_BEAR_MESSAGE = "A Spirit Bear has appeared!";
    private static final String SPIRIT_BOW_NAME = "Spirit Bow";

    @SubscribeEvent(receiveCanceled = true)
    public void onChat(ClientChatReceivedEvent event) {
        String message = StringUtils.stripControlCodes(event.message.getUnformattedText());

        if (!SkyblockData.getCurrentLocation().isDungeon()) return;
        if (MC.thePlayer == null) return;

        if (message.startsWith(BLOOD_READY_MESSAGE)) {
            if (Config.feature.dungeons.general.dungeonsBloodReady) {
                TitleUtils.showTitle(EnumChatFormatting.RED + "BLOOD READY!", 2000);
                if (MC.theWorld != null) {
                    SoundUtils.playSound(MC.thePlayer.getPosition(), "note.pling", 2.0F, 1.0F);
                    MC.thePlayer.sendChatMessage("/pc Blood Ready!");
                }
            }
        }
        if (message.startsWith(SPIRIT_BEAR_MESSAGE)) {
            if (MC.theWorld != null && Config.feature.dungeons.general.dungeonsSpiritBow) {
                SoundUtils.playSound(MC.thePlayer.getPosition(), "mob.enderdragon.growl", 2.0F, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!SkyblockData.getCurrentLocation().isDungeon()) return;
        if (!Config.feature.dungeons.general.dungeonsSpiritBow) return;
        if (MC.theWorld == null || MC.thePlayer == null) return;

        WorldClient world = MC.theWorld;
        final Vec3 eyePos = MC.thePlayer.getPositionEyes(event.partialTicks);
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityArmorStand)) continue;

            EntityArmorStand armorStand = (EntityArmorStand) entity;
            String name = armorStand.getName();
            if (name == null || !name.contains(SPIRIT_BOW_NAME)) continue;

            RenderUtils.draw3DLine(
                    entity.posX, entity.posY + 0.5D, entity.posZ,
                    eyePos.xCoord, eyePos.yCoord, eyePos.zCoord,
                    SPIRIT_BOW_COLOR, 8, true, event.partialTicks
            );
            break;
        }
    }
    @SubscribeEvent
    public void render(RenderEntityModelEvent e) {
        EntityLivingBase entity = e.getEntity();
        if (MC.thePlayer == null || MC.theWorld == null) return;
        if (entity instanceof EntityWither) {
            String name = EnumChatFormatting.getTextWithoutFormattingCodes(entity.getName());
            if ((name.equals("Maxor") || name.equals("Storm") || name.equals("Goldor") || name.equals("Necron"))) {
                if (!Config.feature.dungeons.dungeonsWithersBox) return;

                Color color = ColorUtils.getColor(Config.feature.dungeons.dungeonsWithersBoxColor);
                GlStateManager.disableDepth();
                GlStateManager.disableCull();
                if (Configuration.isPojav()) {
                    EntityHighlightUtils.renderEntityOutline(e, color);
                } else {
                    OutlineUtils.outlineEntity(e, 4.0f, color, true);
                }
                GlStateManager.enableDepth();
                GlStateManager.enableCull();
            }
        }
    }
}


