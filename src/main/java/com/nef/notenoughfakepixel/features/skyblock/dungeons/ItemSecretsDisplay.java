/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.MobDisplayTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class ItemSecretsDisplay {

    private static final String ID_TRAINING_WEIGHTS = "32d530e8-2686-3a8c-bc41-ce3650e12bdf";
    private static final String ID_TREASURE_TALISMAN = "9c287464-1a06-3eed-8974-6dcc511d63b2";
    private static final String ID_WITHER_ESSENCE = "96f80a4b-daac-35e7-9c04-1a1544aadeb3";
    private static final Minecraft MC = Minecraft.getMinecraft();
    private final List<Entity> trackedEntities = new ArrayList<>();
    private int refreshTicks;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.feature.dungeons.dungeonsItemSecretsDisplay || !SkyblockData.getCurrentLocation().isDungeon()
                || MC.theWorld == null || MC.thePlayer == null) {
            trackedEntities.clear();
            refreshTicks = 0;
            return;
        }
        if (++refreshTicks < 10) return;
        refreshTicks = 0;
        rebuildCache((WorldClient) MC.theWorld);
    }

    private void rebuildCache(WorldClient world) {
        trackedEntities.clear();
        for (Entity entity : world.loadedEntityList) {
            if (entity == null) continue;
            if (isItemSecret(entity) || isWitherEssence(entity)) trackedEntities.add(entity);
        }
    }

    private boolean isItemSecret(Entity entity) {
        if (entity instanceof EntityItem) {
            String name = entity.getName();
            if ("item.item.shears".equals(name) || "item.item.monsterPlacer".equals(name)) return true;
            if (!"item.item.skull.char".equals(name)) return false;
            ItemStack stack = ((EntityItem) entity).getEntityItem();
            return hasSkullId(stack, ID_TRAINING_WEIGHTS) || hasSkullId(stack, ID_TREASURE_TALISMAN);
        }
        return false;
    }

    private boolean isWitherEssence(Entity entity) {
        if (!(entity instanceof EntityArmorStand)) return false;
        ItemStack head = ((EntityArmorStand) entity).getEquipmentInSlot(4);
        return hasSkullId(head, ID_WITHER_ESSENCE);
    }

    private boolean hasSkullId(ItemStack stack, String id) {
        if (stack == null || stack.getItem() != Items.skull) return false;
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null || !nbt.hasKey("SkullOwner")) return false;
        NBTTagCompound owner = nbt.getCompoundTag("SkullOwner");
        return owner.hasKey("Id") && id.equals(owner.getString("Id"));
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!Config.feature.dungeons.dungeonsItemSecretsDisplay) return;
        if (MC.thePlayer == null || MC.theWorld == null || !SkyblockData.getCurrentLocation().isDungeon()) return;

        Color color = ColorUtils.getColor(Config.feature.dungeons.dungeonsItemSecretsColor);
        MobDisplayTypes hitboxType = Config.feature.dungeons.dungeonsItemSecretsBig ? MobDisplayTypes.ITEMBIG : MobDisplayTypes.ITEM;

        for (Entity entity : trackedEntities) {
            if (entity == null || entity.isDead) continue;
            if (isWitherEssence(entity)) {
                render(entity, event, color, MobDisplayTypes.WITHERESSENCE);
            } else if (isItemSecret(entity)) {
                render(entity, event, color, hitboxType);
            }
        }
    }

    private void render(Entity entity, RenderWorldLastEvent event, Color color, MobDisplayTypes type) {
        GlStateManager.disableDepth();
        try {
            RenderUtils.renderEntityHitbox(entity, event.partialTicks, color, type);
        } finally {
            GlStateManager.enableDepth();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            trackedEntities.clear();
            refreshTicks = 0;
        }
    }
}
