/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.events.PacketWriteEvent;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.SoundUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.*;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@RegisterEvents
public class MiscFeatures {

    private static final Pattern DYING_MOB_PATTERN_1 = Pattern.compile("^§.\\[§.Lv\\d+§.\\] §.+ (?:§.)+0§f/.+§c❤$");
    private static final Pattern DYING_MOB_PATTERN_2 = Pattern.compile("^.+ (?:§.)+0§c❤$");

    ArrayList<Block> flowerPlaceable = new ArrayList<>(Arrays.asList(
            Blocks.grass,
            Blocks.dirt,
            Blocks.flower_pot,
            Blocks.tallgrass,
            Blocks.double_plant
    ));

    private final ItemStack[] hiddenArmor = new ItemStack[4];
    private EntityPlayer hiddenArmorOwner = null;

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent event) {
        if (!SkyblockData.getCurrentGamemode().isSkyblock() || mc.thePlayer != event.entityPlayer) return;
        ItemStack item = event.entityPlayer.getHeldItem();
        if (item == null) return;

        if (event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            Block block = mc.theWorld.getBlockState(event.pos).getBlock();

            if (flowerPlaceable.contains(block)) {
                if (Config.feature.qol.qolBlockPlacingItems && item.getDisplayName().contains("Flower of Truth")) {
                    event.setCanceled(true);
                }
                if (Config.feature.qol.qolBlockPlacingItems && item.getDisplayName().contains("Spirit Sceptre")) {
                    event.setCanceled(true);
                }
                if (Config.feature.qol.qolBlockPlacingItems && item.getDisplayName().contains("Bouquet of Lies")) {
                    event.setCanceled(true);
                }
            }

            if (Config.feature.qol.qolBlockPlacingItems && item.getDisplayName().contains("Superboom TNT")) {
                event.setCanceled(true);
            }

            if (Config.feature.qol.qolBlockPlacingItems && item.getDisplayName().contains("Infinityboom TNT")) {
                event.setCanceled(true);
            }

            if (Config.feature.qol.qolBlockPlacingItems && item.getItem() == Item.getItemFromBlock(Blocks.hopper) && item.getDisplayName().contains("Weird Tuba")) {
                event.setCanceled(true);
            }
        }
    }

    private volatile long aotvUseTime = 0L;
    private volatile double aotvUseX;
    private volatile double aotvUseY;
    private volatile double aotvUseZ;
    private volatile double aotvTargetX;
    private volatile double aotvTargetY;
    private volatile double aotvTargetZ;
    private volatile double cachedPlayerX;
    private volatile double cachedPlayerY;
    private volatile double cachedPlayerZ;
    private volatile boolean aotvTargetKnown = false;
    private volatile boolean cachedAotvEtherwarp = false;
    /** Client-thread SkyBlock state snapshot consumed by Netty packet callbacks. */
    private volatile boolean skyblockContextCached;

    private static final long AOTV_USE_WINDOW_MS = 1800L;
    private static final double AOTV_SOUND_RADIUS_SQ = 12.0D * 12.0D;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        skyblockContextCached = mc.thePlayer != null && mc.theWorld != null
                && SkyblockData.getCurrentGamemode() != null
                && SkyblockData.getCurrentGamemode().isSkyblock();

        if (mc.thePlayer != null) {
            cachedPlayerX = mc.thePlayer.posX;
            cachedPlayerY = mc.thePlayer.posY;
            cachedPlayerZ = mc.thePlayer.posZ;
            cachedAotvEtherwarp = isAotvEtherwarpStack(mc.thePlayer.getHeldItem()) && mc.thePlayer.isSneaking();
        } else {
            cachedAotvEtherwarp = false;
        }

        if (aotvUseTime != 0L && System.currentTimeMillis() - aotvUseTime > AOTV_USE_WINDOW_MS) {
            clearAotvTeleport();
        }
    }

    @SubscribeEvent
    public void onPacketWrite(PacketWriteEvent event) {
        // PacketWriteEvent is delivered on Netty; never read mutable SkyblockData from this thread.
        if (!skyblockContextCached) return;
        if (Config.feature.qol.qolEtherwarpSound == 0) return;
        if (!(event.packet instanceof C08PacketPlayerBlockPlacement)) return;
        if (!cachedAotvEtherwarp) return;

        C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement) event.packet;
        if (!isAotvEtherwarpStack(packet.getStack())) return;

        aotvUseTime = System.currentTimeMillis();
        aotvUseX = cachedPlayerX;
        aotvUseY = cachedPlayerY;
        aotvUseZ = cachedPlayerZ;
        aotvTargetX = cachedPlayerX;
        aotvTargetY = cachedPlayerY;
        aotvTargetZ = cachedPlayerZ;
        aotvTargetKnown = false;
    }

    @SubscribeEvent
    public void onPacketRead(PacketReadEvent event) {
        if (event.packet instanceof S0EPacketSpawnObject) {
            S0EPacketSpawnObject spawnPacket = (S0EPacketSpawnObject) event.packet;
            if (spawnPacket.getType() == 70 && Config.feature.qol.qolHideFallingBlocks) {
                event.setCanceled(true);
            }
            return;
        }

        if (event.packet instanceof S08PacketPlayerPosLook) {
            if (aotvUseTime != 0L && System.currentTimeMillis() - aotvUseTime <= AOTV_USE_WINDOW_MS) {
                S08PacketPlayerPosLook packet = (S08PacketPlayerPosLook) event.packet;
                Set<S08PacketPlayerPosLook.EnumFlags> flags = packet.func_179834_f();

                double targetX = packet.getX();
                double targetY = packet.getY();
                double targetZ = packet.getZ();

                if (flags != null) {
                    if (flags.contains(S08PacketPlayerPosLook.EnumFlags.X)) targetX += aotvUseX;
                    if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Y)) targetY += aotvUseY;
                    if (flags.contains(S08PacketPlayerPosLook.EnumFlags.Z)) targetZ += aotvUseZ;
                }

                aotvTargetX = targetX;
                aotvTargetY = targetY;
                aotvTargetZ = targetZ;
                aotvTargetKnown = true;
            }
            return;
        }

        if (!(event.packet instanceof S29PacketSoundEffect)) return;
        if (Config.feature.qol.qolEtherwarpSound == 0) return;

        S29PacketSoundEffect soundPacket = (S29PacketSoundEffect) event.packet;
        if (!"mob.endermen.portal".equals(soundPacket.getSoundName())) return;
        if (aotvUseTime == 0L || System.currentTimeMillis() - aotvUseTime > AOTV_USE_WINDOW_MS) return;

        if (!aotvTargetKnown) return;

        double targetX = aotvTargetX;
        double targetY = aotvTargetY;
        double targetZ = aotvTargetZ;

        double dx = soundPacket.getX() - targetX;
        double dy = soundPacket.getY() - targetY;
        double dz = soundPacket.getZ() - targetZ;
        double targetDistanceSq = dx * dx + dy * dy + dz * dz;

        if (targetDistanceSq > AOTV_SOUND_RADIUS_SQ) return;

        event.setCanceled(true);
        String soundName = getConfiguredSound();
        float volume = soundPacket.getVolume();
        float pitch = soundPacket.getPitch();
        double soundX = soundPacket.getX();
        double soundY = soundPacket.getY();
        double soundZ = soundPacket.getZ();

        mc.addScheduledTask(() -> {
            if (mc.theWorld == null) return;
            mc.theWorld.playSound(soundX, soundY, soundZ, soundName, volume, pitch, false);
        });
        clearAotvTeleport();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        skyblockContextCached = false;
        clearAotvTeleport();
    }

    private void clearAotvTeleport() {
        aotvUseTime = 0L;
        aotvTargetKnown = false;
    }

    private boolean isAotvEtherwarpStack(ItemStack stack) {
        if (stack == null) return false;
        if (!"ASPECT_OF_THE_VOID".equals(ItemUtils.getInternalName(stack))) return false;
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("ExtraAttributes", 10)) return false;

        NBTTagCompound extraAttributes = stack.getTagCompound().getCompoundTag("ExtraAttributes");
        return extraAttributes.hasKey("conduit") && extraAttributes.getInteger("conduit") == 1;
    }

    private String getConfiguredSound() {
        switch (Config.feature.qol.qolEtherwarpSound) {
            case 1:
                return "mob.blaze.hit";
            case 2:
                return "note.pling";
            case 3:
                return "random.orb";
            case 4:
                return "mob.enderdragon.hit";
            case 5:
                return "mob.cat.meow";
            default:
                return "mob.endermen.portal";
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderLivingSpecialsPre(RenderLivingEvent.Specials.Pre<EntityLivingBase> event) {
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;
        if (Config.feature.qol.qolHideDyingMobs) {
            EntityLivingBase entity = event.entity;
            String name = entity.getDisplayName().getUnformattedText();

            if (DYING_MOB_PATTERN_1.matcher(name).matches() || DYING_MOB_PATTERN_2.matcher(name).matches()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;

        restoreHiddenArmor();

        if (Config.feature.qol.qolHideDyingMobs) {
            EntityLivingBase entity = event.entity;

            if (entity != Minecraft.getMinecraft().thePlayer && (entity.getHealth() <= 0 || entity.isDead)) {
                double playerX = Minecraft.getMinecraft().thePlayer.posX;
                double playerZ = Minecraft.getMinecraft().thePlayer.posZ;

                double teleportY = -64.0;
                entity.setPositionAndUpdate(playerX, teleportY, playerZ);

                event.setCanceled(true);
                
                return;
            }
        }

        if (Config.feature.qol.qolHidePlayerArmor && event.entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entity;
            ItemStack[] armorInv = player.inventory.armorInventory;
            int n = Math.min(armorInv.length, hiddenArmor.length);
            for (int i = 0; i < n; i++) {
                hiddenArmor[i] = armorInv[i];
                armorInv[i] = null;
            }
            hiddenArmorOwner = player;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderLivingPreCancelled(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (hiddenArmorOwner != null && event.isCanceled()) {
            restoreHiddenArmor();
        }
    }

    private void restoreHiddenArmor() {
        EntityPlayer owner = hiddenArmorOwner;
        if (owner == null) return;
        hiddenArmorOwner = null;
        ItemStack[] armorInv = owner.inventory.armorInventory;
        int n = Math.min(armorInv.length, hiddenArmor.length);
        for (int i = 0; i < n; i++) {
            armorInv[i] = hiddenArmor[i];
            hiddenArmor[i] = null;
        }
    }

    @SubscribeEvent
    public void onRenderLivingSpecials(RenderLivingEvent.Specials.Pre<EntityLivingBase> event) {
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;

        if (Config.feature.qol.qolHideDyingMobs) {
            EntityLivingBase entity = event.entity;

            if (entity.getHealth() <= 0 || entity.isDead) {
                double playerX = Minecraft.getMinecraft().thePlayer.posX;
                double playerZ = Minecraft.getMinecraft().thePlayer.posZ;

                double teleportY = -64.0;
                entity.setPositionAndUpdate(playerX, teleportY, playerZ);

                event.setCanceled(true);
            }
        }

    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (Config.feature.misc.qolAlwaysSprint) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }
    }

    @SubscribeEvent
    public void onRenderLivingPost(RenderLivingEvent.Post<EntityLivingBase> event) {
        
        restoreHiddenArmor();
    }

}
