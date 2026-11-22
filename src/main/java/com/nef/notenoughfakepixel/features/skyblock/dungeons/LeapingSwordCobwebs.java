/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

@RegisterEvents
public final class LeapingSwordCobwebs {

    private static final Item COBWEB_ITEM = Item.getItemFromBlock(Blocks.web);
    private static final long DEBUG_WINDOW_MS = 3500L;
    private static final Minecraft MC = Minecraft.getMinecraft();

    private static boolean debugEnabled;
    
    private static final double DEBUG_RADIUS = 12.0D;

    private static long debugUntil;
    private static final Map<Integer, String> debugStandStates = new HashMap<>();

    public LeapingSwordCobwebs() {
    }

    public static boolean toggleDebug() {
        debugEnabled = !debugEnabled;
        debugUntil = 0L;
        debugStandStates.clear();
        return debugEnabled;
    }

    public static boolean shouldSuppressEntity(Entity entity) {
        if (!Config.feature.dungeons.dungeonsDisableLeapingSwordCobwebs) return false;
        if (entity instanceof EntityFallingBlock) return isCobwebFallingBlock((EntityFallingBlock) entity);
        if (!(entity instanceof EntityArmorStand)) return false;

        EntityArmorStand stand = (EntityArmorStand) entity;
        if (stand.worldObj == null || !stand.worldObj.isRemote) return false;

        return isLeapingSwordCobwebStand(stand);
    }

    private static boolean isCobwebFallingBlock(EntityFallingBlock block) {
        if (block.worldObj == null || !block.worldObj.isRemote) return false;
        IBlockState state = block.getBlock();
        return state != null && state.getBlock() == Blocks.web;
    }

    private static boolean isLeapingSwordCobwebStand(EntityArmorStand stand) {
        return hasCobwebEquipment(stand);
    }

    private static boolean hasCobwebEquipment(EntityArmorStand stand) {
        for (int slot = 0; slot <= 4; slot++) {
            ItemStack stack = stand.getEquipmentInSlot(slot);
            if (stack != null && stack.getItem() == COBWEB_ITEM) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.world == null || !event.world.isRemote) return;
        if (event.entityPlayer != MC.thePlayer) return;
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR &&
                event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;

        if (!debugEnabled) return;
        ItemStack held = event.entityPlayer.getHeldItem();
        if (!isLeapSword(held)) return;

        debugUntil = System.currentTimeMillis() + DEBUG_WINDOW_MS;
        debugStandStates.clear();

        debug("===== LEAP DEBUG START =====");
        debug("item.internal=" + ItemUtils.getInternalName(held));
        debug("item.display=" + clean(held.getDisplayName()));
        debug("player=" + clean(event.entityPlayer.getName()) + " entityId=" + event.entityPlayer.getEntityId());
        debug("playerPos=" + format(event.entityPlayer.posX, event.entityPlayer.posY, event.entityPlayer.posZ));
        debug("world=" + (MC.theWorld == null ? "null" : MC.theWorld.getWorldInfo().getWorldName()));
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (!debugEnabled || !isDebugWindowActive() || event.world == null || !event.world.isRemote || event.entity == null) return;

        Entity entity = event.entity;
        if (MC.thePlayer != null && entity.getDistanceSqToEntity(MC.thePlayer) > DEBUG_RADIUS * DEBUG_RADIUS) return;
        if (entity instanceof EntityFallingBlock) {
            debug("ENTITY_JOIN FallingBlock id=" + entity.getEntityId() +
                    " pos=" + format(entity.posX, entity.posY, entity.posZ) +
                    " block=" + ((EntityFallingBlock) entity).getBlock());
        } else if (entity instanceof EntityArmorStand) {
            EntityArmorStand stand = (EntityArmorStand) entity;
            debug("ENTITY_JOIN ArmorStand id=" + stand.getEntityId() +
                    " pos=" + format(stand.posX, stand.posY, stand.posZ) +
                    " invisible=" + stand.isInvisible() +
                    " alive=" + stand.isEntityAlive());
            dumpArmorStand(stand, "join");
        } else {
            debug("ENTITY_JOIN class=" + entity.getClass().getName() +
                    " id=" + entity.getEntityId() +
                    " pos=" + format(entity.posX, entity.posY, entity.posZ));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!debugEnabled) return;
        if (!isDebugWindowActive()) {
            if (!debugStandStates.isEmpty()) {
                debug("===== LEAP DEBUG END =====");
                debugStandStates.clear();
            }
            return;
        }

        if (MC.theWorld == null || MC.thePlayer == null) return;

        for (Entity entity : MC.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityArmorStand)) continue;

            EntityArmorStand stand = (EntityArmorStand) entity;
            if (stand.getDistanceSqToEntity(MC.thePlayer) > DEBUG_RADIUS * DEBUG_RADIUS) continue;
            String state = buildArmorStandState(stand);
            String previous = debugStandStates.put(stand.getEntityId(), state);
            if (previous == null) {
                dumpArmorStand(stand, "tick-new");
            } else if (!previous.equals(state)) {
                debug("ARMOR_STAND_CHANGED id=" + stand.getEntityId());
                dumpArmorStand(stand, "tick-changed");
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            debugUntil = 0L;
            debugStandStates.clear();
        }
    }

    private static boolean isLeapSword(ItemStack stack) {
        if (stack == null) return false;

        String internal = ItemUtils.getInternalName(stack);
        if ("LEAPING_SWORD".equals(internal) || "SILK_EDGE_SWORD".equals(internal)) return true;

        String display = clean(stack.getDisplayName()).toUpperCase();
        return display.contains("LEAPING SWORD") || display.contains("SILK EDGE SWORD");
    }

    private static boolean isDebugWindowActive() {
        return System.currentTimeMillis() < debugUntil;
    }

    private static void dumpArmorStand(EntityArmorStand stand, String source) {
        debug("ARMOR_STAND source=" + source +
                " id=" + stand.getEntityId() +
                " uuid=" + stand.getUniqueID() +
                " pos=" + format(stand.posX, stand.posY, stand.posZ) +
                " yaw=" + stand.rotationYaw +
                " invisible=" + stand.isInvisible() +
                " customName=" + quote(clean(stand.getCustomNameTag())) +
                " displayName=" + quote(clean(stand.getDisplayName().getUnformattedText())) +
                " bbox=" + stand.getEntityBoundingBox());

        for (int slot = 0; slot <= 4; slot++) {
            ItemStack stack = stand.getEquipmentInSlot(slot);
            debug("  slot" + slot + "=" + describeStack(stack));
        }
    }

    private static String buildArmorStandState(EntityArmorStand stand) {
        StringBuilder state = new StringBuilder();
        state.append(stand.isInvisible()).append('|')
                .append(clean(stand.getCustomNameTag())).append('|');
        for (int slot = 0; slot <= 4; slot++) {
            state.append(describeStack(stand.getEquipmentInSlot(slot))).append('|');
        }
        return state.toString();
    }

    private static String describeStack(ItemStack stack) {
        if (stack == null) return "null";

        return "item=" + String.valueOf(stack.getItem()) +
                ",internal=" + quote(ItemUtils.getInternalName(stack)) +
                ",display=" + quote(clean(stack.getDisplayName())) +
                ",size=" + stack.stackSize +
                ",damage=" + stack.getItemDamage();
    }

    private static String clean(String text) {
        if (text == null) return "null";
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(text);
        return stripped == null ? text : stripped;
    }

    private static String quote(String text) {
        return '"' + String.valueOf(text).replace("\"", "'") + '"';
    }

    private static String format(double x, double y, double z) {
        return String.format("%.3f, %.3f, %.3f", x, y, z);
    }

    private static void debug(String message) {
        System.out.println("[NEF][LeapDebug] " + message);
    }
}
