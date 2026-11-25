package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows.treasure;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.Logger;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.Area;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.List;

@RegisterEvents
public class CrystalHollowsTreasureModule {

    private static final GradientSolver GRAD_SOLVER = new GradientSolver();
    private static final double         LERP        = 0.15;

    private double lastSeenDist = -1.0;
    private int    postResetSkip = 0;

    private BlockPos confirmedChest = null;

    private double smoothX0 = Double.NaN, smoothZ0 = Double.NaN;
    private double smoothX1 = Double.NaN, smoothZ1 = Double.NaN;

    @SubscribeEvent
    public void onPacketRead(PacketReadEvent event) {
        if (!Config.feature.mining.crystalHollows.crystalMetalDetector) return;
        if (SkyblockData.getCurrentArea() != Area.CH_MINES_OF_DIVAN) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // detect if our confirmed chest was covered by a gold block
        if (confirmedChest != null) {
            boolean covered = false;
            if (event.packet instanceof S23PacketBlockChange) {
                S23PacketBlockChange p = (S23PacketBlockChange) event.packet;
                if (p.getBlockPosition().equals(confirmedChest)
                        && mc.theWorld.getBlockState(confirmedChest).getBlock() instanceof BlockChest
                        && !(p.blockState != null && p.blockState.getBlock() instanceof BlockChest)) {
                    covered = true;
                }
            } else if (event.packet instanceof S22PacketMultiBlockChange) {
                S22PacketMultiBlockChange p = (S22PacketMultiBlockChange) event.packet;
                for (S22PacketMultiBlockChange.BlockUpdateData upd : p.getChangedBlocks()) {
                    if (upd.getPos().equals(confirmedChest)
                            && mc.theWorld.getBlockState(confirmedChest).getBlock() instanceof BlockChest
                            && !(upd.getBlockState().getBlock() instanceof BlockChest)) {
                        covered = true;
                        break;
                    }
                }
            }
            if (covered) {
                Logger.log("§c[MD] Confirmed chest was covered — resetting");
                confirmedChest = null;
                resetAll();
                return;
            }
        }

        // detect a new chest spawning from a gold block nearby
        BlockPos newChest = null;
        if (event.packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange p = (S23PacketBlockChange) event.packet;
            if (p.blockState != null && p.blockState.getBlock() instanceof BlockChest)
                newChest = p.getBlockPosition();
        } else if (event.packet instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange p = (S22PacketMultiBlockChange) event.packet;
            for (S22PacketMultiBlockChange.BlockUpdateData upd : p.getChangedBlocks()) {
                if (upd.getBlockState().getBlock() instanceof BlockChest) {
                    newChest = upd.getPos();
                    break;
                }
            }
        }
        if (newChest == null) return;

        Block oldBlock = mc.theWorld.getBlockState(newChest).getBlock();
        if (oldBlock != Blocks.gold_block && oldBlock != Blocks.air) return;

        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        double cx = newChest.getX() + 0.5, cy = newChest.getY() + 0.5, cz = newChest.getZ() + 0.5;
        double distToPlayer = Math.sqrt((cx-px)*(cx-px) + (cy-py)*(cy-py) + (cz-pz)*(cz-pz));
        if (distToPlayer > 5) return;

        confirmedChest = newChest;
        resetSmooth();
        Logger.log("§a[MD] Chest confirmed at " + newChest.getX() + "," + newChest.getY() + "," + newChest.getZ()
            + " §a(dist=" + String.format("%.1f", distToPlayer) + ")");
    }

    @SubscribeEvent
    public void onChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent e) {
        String msg = e.message.getFormattedText();

        if (msg.contains("with your Metal Detector!")) {
            resetAll();
            confirmedChest = null;
            lastSeenDist = -1.0;
        }

        if (!Config.feature.mining.crystalHollows.crystalMetalDetector) return;
        if (SkyblockData.getCurrentArea() != Area.CH_MINES_OF_DIVAN) return;
        if (!msg.contains("TREASURE: ")) return;

        if (postResetSkip > 0) {
            postResetSkip--;
            Logger.log("§7[MD] Skipping stale post-chest reading (" + postResetSkip + " remaining)");
            return;
        }

        try {
            String distStr = msg.split("TREASURE: ")[1].split("m")[0].replaceAll("[^0-9.]", "");
            double distance = Double.parseDouble(distStr);
            EntityPlayerSP p = Minecraft.getMinecraft().thePlayer;

            boolean changed = Math.abs(distance - lastSeenDist) >= 0.05;
            if (!changed) return;
            lastSeenDist = distance;

            if (confirmedChest != null) {
                double dx = confirmedChest.getX() + 0.5 - p.posX;
                double dy = confirmedChest.getY() + 0.5 - p.posY;
                double dz = confirmedChest.getZ() + 0.5 - p.posZ;
                double actualDist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (Math.abs(actualDist - distance) > 3.0) {
                    Logger.log("§c[MD] Chest mismatch (chest=" + String.format("%.1f", actualDist)
                        + "m, MD=" + String.format("%.1f", distance) + "m) — resetting");
                    confirmedChest = null;
                    resetAll();
                }
            }

            boolean gradAccepted = GRAD_SOLVER.addReading(p.posX, p.posY, p.posZ, distance);
            if (gradAccepted) {
                Logger.log("§b[MD] grad_cands=§f" + GRAD_SOLVER.getCandidateCount()
                    + " §b| grad_readings=§f" + GRAD_SOLVER.getReadingCount());
            }

        } catch (Exception ex) {
            Logger.log("§c[MD] parse error: " + ex.getMessage());
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload e) {
        resetAll();
        confirmedChest = null;
        lastSeenDist = -1.0;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load e) {
        if (SkyblockData.getCurrentLocation().equals(Location.CRYSTAL_HOLLOWS)) {
            resetAll();
            confirmedChest = null;
        }
    }

    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent e) {
        if (!Config.feature.mining.crystalHollows.crystalMetalDetector) return;
        if (!SkyblockData.getCurrentLocation().equals(Location.CRYSTAL_HOLLOWS)) return;
        if (e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK &&
                e.world.getBlockState(e.pos).getBlock() instanceof BlockChest) {
            resetAll();
            confirmedChest = null;
            postResetSkip = 7;
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!Config.feature.mining.crystalHollows.crystalMetalDetector) return;
        if (SkyblockData.getCurrentArea() != Area.CH_MINES_OF_DIVAN) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Color waypointColor = ColorUtils.getColor(Config.feature.mining.metalDetector.crystalDivanWaypointColor);
        int playerY = (int) mc.thePlayer.posY;

        if (confirmedChest != null) {
            int beamY = findFloorY(confirmedChest.getX(), confirmedChest.getZ(), confirmedChest.getY());
            BlockPos beamPos = new BlockPos(confirmedChest.getX(), beamY, confirmedChest.getZ());
            BlockPos textPos = new BlockPos(confirmedChest.getX(), playerY, confirmedChest.getZ());
            RenderUtils.renderBeaconBeam(beamPos, waypointColor.getRGB(), 1, event.partialTicks);
            RenderUtils.renderWaypointText("TREASURE!", textPos.add(0, 3, 0), event.partialTicks);
            RenderUtils.draw3DLine(
                mc.thePlayer.getPositionEyes(event.partialTicks),
                new Vec3(confirmedChest.getX() + 0.5, confirmedChest.getY() + 0.5, confirmedChest.getZ() + 0.5),
                waypointColor, 2, true, event.partialTicks
            );
            return;
        }

        List<BlockPos> gradCands = GRAD_SOLVER.getCandidatePositions();
        if (!gradCands.isEmpty()) {
            BlockPos c0 = gradCands.get(0);
            double tx0 = c0.getX() + 0.5, tz0 = c0.getZ() + 0.5;
            if (Double.isNaN(smoothX0)) { smoothX0 = tx0; smoothZ0 = tz0; }
            else { smoothX0 += (tx0 - smoothX0) * LERP; smoothZ0 += (tz0 - smoothZ0) * LERP; }
            drawCandidate(smoothX0, playerY, smoothZ0,
                gradCands.size() == 1 ? "Treasure?" : "Guess A", waypointColor, event.partialTicks);
            if (gradCands.size() >= 2) {
                BlockPos c1 = gradCands.get(1);
                double tx1 = c1.getX() + 0.5, tz1 = c1.getZ() + 0.5;
                if (Double.isNaN(smoothX1)) { smoothX1 = tx1; smoothZ1 = tz1; }
                else { smoothX1 += (tx1 - smoothX1) * LERP; smoothZ1 += (tz1 - smoothZ1) * LERP; }
                drawCandidate(smoothX1, playerY, smoothZ1, "Guess B", waypointColor, event.partialTicks);
            } else {
                smoothX1 = smoothZ1 = Double.NaN;
            }
        }
    }

    private void drawCandidate(double x, int playerY, double z, String label, Color color, float partialTicks) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int beamY = findFloorY(bx, bz, playerY);
        RenderUtils.renderBeaconBeam(new BlockPos(bx, beamY, bz), color.getRGB(), 1, partialTicks);
        RenderUtils.renderWaypointText(label, new BlockPos(bx, playerY, bz).add(0, 3, 0), partialTicks);
    }

    private int findFloorY(int x, int z, int fromY) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return Math.max(1, fromY - 20);
        int limit = Math.max(1, fromY - 50);
        for (int y = fromY; y >= limit; y--) {
            try {
                if (mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock() != Blocks.air) {
                    return y + 1;
                }
            } catch (Exception ignored) {
                return Math.max(1, fromY - 20);
            }
        }
        return limit;
    }

    private void resetAll() {
        GRAD_SOLVER.reset();
        postResetSkip = 0;
        resetSmooth();
    }

    private void resetSmooth() {
        smoothX0 = smoothZ0 = smoothX1 = smoothZ1 = Double.NaN;
    }
}

