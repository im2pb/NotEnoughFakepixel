package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.utils.KeybindHelper;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
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
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public class ChestTracker {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final List<TrackedChest> chests = new ArrayList<>();
    private static final int DETECT_RADIUS = 10;

    @SubscribeEvent
    public void onPacketRead(PacketReadEvent event) {
        if (!Config.feature.mining.crystalHollows.chestTracker) return;
        if (!SkyblockData.getCurrentLocation().isCrystalHollows()) return;

        if (event.packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange p = (S23PacketBlockChange) event.packet;
            if (p.blockState != null && p.blockState.getBlock() instanceof BlockChest)
                trackChest(p.getBlockPosition());
        } else if (event.packet instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange p = (S22PacketMultiBlockChange) event.packet;
            for (S22PacketMultiBlockChange.BlockUpdateData upd : p.getChangedBlocks()) {
                if (upd.getBlockState().getBlock() instanceof BlockChest)
                    trackChest(upd.getPos());
            }
        }
    }

    private void trackChest(BlockPos pos) {
        if (mc.thePlayer == null) return;
        double dx = pos.getX() + 0.5 - mc.thePlayer.posX;
        double dy = pos.getY() + 0.5 - mc.thePlayer.posY;
        double dz = pos.getZ() + 0.5 - mc.thePlayer.posZ;
        if (Math.sqrt(dx * dx + dy * dy + dz * dz) > DETECT_RADIUS) return;
        for (TrackedChest existing : chests) {
            if (existing.pos.equals(pos)) return;
        }
        chests.add(new TrackedChest(pos));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.thePlayer == null) return;
        if (!Config.feature.mining.crystalHollows.chestTracker) return;

        if (!SkyblockData.getCurrentLocation().isCrystalHollows()
                || KeybindHelper.isKeyDown(Config.feature.mining.crystalHollows.chestTrackerClearKey)) {
            chests.clear();
            return;
        }

        if (mc.theWorld == null) return;
        List<TrackedChest> toRemove = new ArrayList<>();
        for (TrackedChest chest : chests) {
            if (!mc.theWorld.isBlockLoaded(chest.pos)) continue;
            if (!(mc.theWorld.getBlockState(chest.pos).getBlock() instanceof BlockChest))
                toRemove.add(chest);
        }
        chests.removeAll(toRemove);
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!Config.feature.mining.crystalHollows.chestTracker) return;
        if (!SkyblockData.getCurrentLocation().isCrystalHollows()) return;
        if (chests.isEmpty()) return;
        if (mc.thePlayer == null) return;

        double px = mc.getRenderManager().viewerPosX;
        double py = mc.getRenderManager().viewerPosY;
        double pz = mc.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();

        Tessellator tess = Tessellator.getInstance();
        WorldRenderer wr = tess.getWorldRenderer();

        boolean drawing = false;
        try {
            wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            drawing = true;

            // Line from player to first chest
            TrackedChest first = chests.get(0);
            float[] fc = getColor(first.getLifeRatio());
            double fx = first.pos.getX() + 0.5 - px;
            double fy = first.pos.getY() + 0.5 - py;
            double fz = first.pos.getZ() + 0.5 - pz;
            wr.pos(0, 0, 0).color(fc[0], fc[1], 0f, 1f).endVertex();
            wr.pos(fx, fy, fz).color(fc[0], fc[1], 0f, 1f).endVertex();

            // Chain lines between chests
            for (int i = 0; i < chests.size() - 1; i++) {
                TrackedChest a = chests.get(i);
                TrackedChest b = chests.get(i + 1);
                float[] ca = getColor(a.getLifeRatio());
                float[] cb = getColor(b.getLifeRatio());
                double ax = a.pos.getX() + 0.5 - px, ay = a.pos.getY() + 0.5 - py, az = a.pos.getZ() + 0.5 - pz;
                double bx = b.pos.getX() + 0.5 - px, by = b.pos.getY() + 0.5 - py, bz = b.pos.getZ() + 0.5 - pz;
                wr.pos(ax, ay, az).color(ca[0], ca[1], 0f, 1f).endVertex();
                wr.pos(bx, by, bz).color(cb[0], cb[1], 0f, 1f).endVertex();
            }

            // Wireframe box around each chest
            for (TrackedChest chest : chests) {
                float[] c = getColor(chest.getLifeRatio());
                double x = chest.pos.getX() - px, y = chest.pos.getY() - py, z = chest.pos.getZ() - pz;
                double x2 = x + 1, y2 = y + 1, z2 = z + 1;
                float r = c[0], g = c[1];

                wr.pos(x,  y,  z ).color(r,g,0f,1f).endVertex(); wr.pos(x2, y,  z ).color(r,g,0f,1f).endVertex();
                wr.pos(x2, y,  z ).color(r,g,0f,1f).endVertex(); wr.pos(x2, y,  z2).color(r,g,0f,1f).endVertex();
                wr.pos(x2, y,  z2).color(r,g,0f,1f).endVertex(); wr.pos(x,  y,  z2).color(r,g,0f,1f).endVertex();
                wr.pos(x,  y,  z2).color(r,g,0f,1f).endVertex(); wr.pos(x,  y,  z ).color(r,g,0f,1f).endVertex();

                wr.pos(x,  y2, z ).color(r,g,0f,1f).endVertex(); wr.pos(x2, y2, z ).color(r,g,0f,1f).endVertex();
                wr.pos(x2, y2, z ).color(r,g,0f,1f).endVertex(); wr.pos(x2, y2, z2).color(r,g,0f,1f).endVertex();
                wr.pos(x2, y2, z2).color(r,g,0f,1f).endVertex(); wr.pos(x,  y2, z2).color(r,g,0f,1f).endVertex();
                wr.pos(x,  y2, z2).color(r,g,0f,1f).endVertex(); wr.pos(x,  y2, z ).color(r,g,0f,1f).endVertex();

                wr.pos(x,  y,  z ).color(r,g,0f,1f).endVertex(); wr.pos(x,  y2, z ).color(r,g,0f,1f).endVertex();
                wr.pos(x2, y,  z ).color(r,g,0f,1f).endVertex(); wr.pos(x2, y2, z ).color(r,g,0f,1f).endVertex();
                wr.pos(x2, y,  z2).color(r,g,0f,1f).endVertex(); wr.pos(x2, y2, z2).color(r,g,0f,1f).endVertex();
                wr.pos(x,  y,  z2).color(r,g,0f,1f).endVertex(); wr.pos(x,  y2, z2).color(r,g,0f,1f).endVertex();
            }

            tess.draw();
            drawing = false;
        } finally {
            if (drawing) {
                try { tess.draw(); } catch (Exception ignored) {}
            }
            GlStateManager.enableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        chests.clear();
    }

    private float[] getColor(float lifeRatio) {
        return new float[]{ 1.0f - lifeRatio, lifeRatio };
    }
}

