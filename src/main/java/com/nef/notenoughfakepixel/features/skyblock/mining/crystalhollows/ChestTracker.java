/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows;

import com.nef.notenoughfakepixel.config.gui.Config;
import org.lwjgl.input.Keyboard;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;


/** Packet-driven, bounded chest tracker for Crystal Hollows. */
@RegisterEvents
public class ChestTracker {
    private static final int DETECT_RADIUS_SQ = 100;
    private static final int MAX_TRACKED = 48;
    private static final long LIFETIME_MS = 30000L;
    private static final long CLEANUP_INTERVAL_MS = 500L;
    private final Minecraft mc = Minecraft.getMinecraft();
    // PacketReadEvent runs on Netty's channel thread. Only immutable block coordinates cross threads;
    // the map and all Minecraft world/player reads are kept on the client tick/render thread.
    private static final int MAX_PENDING = 256;
    private final Object pendingLock = new Object();
    private final ConcurrentLinkedQueue<BlockPos> pending = new ConcurrentLinkedQueue<>();
    private int pendingCount;
    private final LinkedHashMap<BlockPos, Long> chests = new LinkedHashMap<>();
    private long lastCleanupMs;

    @SubscribeEvent
    public void onPacketRead(PacketReadEvent event) {
        if (!Config.feature.mining.crystalChestTracker || event == null || event.packet == null) return;
        if (event.packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange packet = (S23PacketBlockChange) event.packet;
            if (packet.blockState != null && packet.blockState.getBlock() instanceof BlockChest) enqueue(packet.getBlockPosition());
        } else if (event.packet instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange packet = (S22PacketMultiBlockChange) event.packet;
            for (S22PacketMultiBlockChange.BlockUpdateData update : packet.getChangedBlocks()) {
                if (update != null && update.getBlockState() != null && update.getBlockState().getBlock() instanceof BlockChest)
                    enqueue(update.getPos());
            }
        }
    }

    private boolean enabled() {
        return Config.feature.mining.crystalChestTracker && SkyblockData.getCurrentLocation().isCrystalHollows();
    }

    private void enqueue(BlockPos pos) {
        if (pos == null) return;
        synchronized (pendingLock) {
            if (pendingCount >= MAX_PENDING) return;
            pending.offer(pos);
            pendingCount++;
        }
    }

    private void track(BlockPos pos) {
        if (pos == null || mc.thePlayer == null) return;
        double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
        double dy = pos.getY() + 0.5 - mc.thePlayer.posY;
        double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;
        if (dx * dx + dy * dy + dz * dz > DETECT_RADIUS_SQ) return;
        if (chests.containsKey(pos)) return;
        chests.put(pos, System.currentTimeMillis());
        while (chests.size() > MAX_TRACKED) {
            Iterator<BlockPos> iterator = chests.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next(); iterator.remove();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!enabled() || mc.theWorld == null || mc.thePlayer == null) {
            chests.clear();
            clearPending();
            return;
        }
        boolean clearDown = Keyboard.isKeyDown(Config.feature.mining.crystalChestTrackerClearKey);
        if (clearDown) {
            chests.clear();
            clearPending();
            return;
        }
        for (int processed = 0; processed < 64; processed++) {
            BlockPos candidate = pollPending();
            if (candidate == null) break;
            track(candidate);
        }

        long now = System.currentTimeMillis();
        if (now - lastCleanupMs < CLEANUP_INTERVAL_MS) return;
        lastCleanupMs = now;
        Iterator<Map.Entry<BlockPos, Long>> iterator = chests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (now - entry.getValue() > LIFETIME_MS) { iterator.remove(); continue; }
            BlockPos pos = entry.getKey();
            if (mc.theWorld.isBlockLoaded(pos) && !(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockChest)) iterator.remove();
        }
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled() || chests.isEmpty() || mc.thePlayer == null || mc.theWorld == null) return;
        RenderManager renderManager = mc.getRenderManager();
        double px = renderManager.viewerPosX;
        double py = renderManager.viewerPosY;
        double pz = renderManager.viewerPosZ;
        long now = System.currentTimeMillis();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GL11.glLineWidth(1.2f);
        boolean drawing = false;
        try {
            renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            drawing = true;
            BlockPos previous = null;
            for (Map.Entry<BlockPos, Long> entry : chests.entrySet()) {
                BlockPos pos = entry.getKey();
                double age = Math.max(0.0, Math.min(1.0, (now - entry.getValue()) / (double) LIFETIME_MS));
                float red = (float) age;
                float green = (float) (1.0 - age);
                double x = pos.getX() - px, y = pos.getY() - py, z = pos.getZ() - pz;
                double cx = x + 0.5, cy = y + 0.5, cz = z + 0.5;

                if (previous == null) addLine(renderer, 0, 0, 0, cx, cy, cz, red, green);
                else addLine(renderer,
                        previous.getX() + 0.5 - px, previous.getY() + 0.5 - py, previous.getZ() + 0.5 - pz,
                        cx, cy, cz, red, green);

                // Twelve wireframe edges are emitted into the same batch, not one GL draw per chest.
                double x2 = x + 1.0, y2 = y + 1.0, z2 = z + 1.0;
                addLine(renderer, x, y, z, x2, y, z, red, green);
                addLine(renderer, x2, y, z, x2, y, z2, red, green);
                addLine(renderer, x2, y, z2, x, y, z2, red, green);
                addLine(renderer, x, y, z2, x, y, z, red, green);
                addLine(renderer, x, y2, z, x2, y2, z, red, green);
                addLine(renderer, x2, y2, z, x2, y2, z2, red, green);
                addLine(renderer, x2, y2, z2, x, y2, z2, red, green);
                addLine(renderer, x, y2, z2, x, y2, z, red, green);
                addLine(renderer, x, y, z, x, y2, z, red, green);
                addLine(renderer, x2, y, z, x2, y2, z, red, green);
                addLine(renderer, x2, y, z2, x2, y2, z2, red, green);
                addLine(renderer, x, y, z2, x, y2, z2, red, green);
                previous = pos;
            }
            tessellator.draw();
            drawing = false;
        } finally {
            if (drawing) {
                try { tessellator.draw(); } catch (Exception ignored) { }
            }
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
        }
    }

    private void addLine(WorldRenderer renderer, double ax, double ay, double az,
                         double bx, double by, double bz, float red, float green) {
        renderer.pos(ax, ay, az).color(red, green, 0.0f, 1.0f).endVertex();
        renderer.pos(bx, by, bz).color(red, green, 0.0f, 1.0f).endVertex();
    }

    private BlockPos pollPending() {
        synchronized (pendingLock) {
            BlockPos pos = pending.poll();
            if (pos != null) pendingCount = Math.max(0, pendingCount - 1);
            return pos;
        }
    }

    private void clearPending() {
        synchronized (pendingLock) {
            pending.clear();
            pendingCount = 0;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        chests.clear(); clearPending(); lastCleanupMs = 0L;
    }
}
