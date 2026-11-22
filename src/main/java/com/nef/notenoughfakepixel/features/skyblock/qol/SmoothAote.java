/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.util.vector.Vector3f;

import java.util.Arrays;
import java.util.List;

/** Camera interpolation helper for Aspect of the End/Void and Shadow Warp. */
@RegisterEvents
public class SmoothAote {
    private static final List<String> TELEPORT_ITEMS = Arrays.asList("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID");
    private static volatile SmoothAote instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private Vector3f renderPosition;
    private int remainingMs;
    private long lastFrameMs;
    private long lastTeleportMs;
    private final int transitionMs = 125;

    public SmoothAote() { instance = this; }
    public static SmoothAote getInstance() { return instance; }

    public Vector3f getCurrentPosition() {
        if (!Config.feature.qol.enableSmoothAote || remainingMs <= 0 || renderPosition == null) return null;
        return renderPosition;
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!Config.feature.qol.enableSmoothAote || event == null || event.entityPlayer == null
                || event.entityPlayer != mc.thePlayer) return;
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_AIR && event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        if (mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) return;
        ItemStack held = event.entityPlayer.getHeldItem();
        if (held == null) return;
        String internal = ItemUtils.getInternalName(held);
        if (internal == null) return;
        boolean shadowWarp = false;
        NBTTagCompound tag = held.getTagCompound();
        if (tag != null && tag.hasKey("ExtraAttributes", 10)) {
            NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
            shadowWarp = extra.hasKey("shadow_warp");
        }
        if (!TELEPORT_ITEMS.contains(internal) && !shadowWarp) return;
        lastTeleportMs = System.currentTimeMillis();
        if (renderPosition == null) {
            renderPosition = new Vector3f((float) mc.thePlayer.posX, (float) mc.thePlayer.posY, (float) mc.thePlayer.posZ);
        }
    }

    /** Called by the local-player position packet redirect after the server confirms the teleport. */
    public void onTeleportApplied() {
        if (!Config.feature.qol.enableSmoothAote || renderPosition == null) return;
        remainingMs = Math.min(transitionMs * 2, remainingMs + transitionMs);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.feature.qol.enableSmoothAote || mc.thePlayer == null || mc.theWorld == null) {
            reset(); return;
        }
        long now = System.currentTimeMillis();
        int delta = (int) Math.max(0L, Math.min(100L, now - (lastFrameMs == 0L ? now : lastFrameMs)));
        lastFrameMs = now;
        if (remainingMs > 0 && renderPosition != null) {
            int step = Math.min(delta, remainingMs);
            float factor = remainingMs <= 0 ? 1.0f : step / (float) remainingMs;
            renderPosition.x += ((float) mc.thePlayer.posX - renderPosition.x) * factor;
            renderPosition.y += ((float) mc.thePlayer.posY - renderPosition.y) * factor;
            renderPosition.z += ((float) mc.thePlayer.posZ - renderPosition.z) * factor;
            BlockPos pos = new BlockPos(renderPosition.x, renderPosition.y, renderPosition.z);
            if (mc.theWorld.getBlockState(pos).getBlock().getMaterial() != Material.air) {
                renderPosition.y = (float) Math.ceil(renderPosition.y);
            }
            remainingMs = Math.max(0, remainingMs - step);
        } else if (now - lastTeleportMs > 1000L) {
            renderPosition = null;
            remainingMs = 0;
        } else if (renderPosition != null) {
            renderPosition.set((float) mc.thePlayer.posX, (float) mc.thePlayer.posY, (float) mc.thePlayer.posZ);
        }
    }

    private void reset() {
        renderPosition = null; remainingMs = 0; lastFrameMs = 0L; lastTeleportMs = 0L;
    }
}
