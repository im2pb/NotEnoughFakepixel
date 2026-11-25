package com.nef.notenoughfakepixel.features.skyblock.dungeons.terminals;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonManager;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@RegisterEvents
public class TerminalWaypoints {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final Color WAYPOINT_COLOR = new Color(0, 191, 255, 150);

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!Config.feature.dungeons.terminals.dungeonsTerminalWaypoints) return;
        if (!DungeonManager.checkEssentialsF7()) return;
        if (MC.thePlayer == null || MC.theWorld == null) return;

        for (Entity entity : MC.theWorld.loadedEntityList) {
            String name = entity.getDisplayName().getUnformattedText();
            if (!name.contains("Inactive Device")
                    && !name.contains("Not Activated")
                    && !name.contains("Inactive Terminal")) {
                continue;
            }

            RenderUtils.renderFilledBoundingBox(getAxisAlignedBB(event, entity, MC), WAYPOINT_COLOR, true);
        }
    }

    private static @NotNull AxisAlignedBB getAxisAlignedBB(RenderWorldLastEvent event, Entity entity, Minecraft mc) {
        double px = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
        double py = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks;
        double pz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;

        // Determine the block position for the entity (using floor to align with block boundaries)
        double blockX = Math.floor(entity.posX);
        double blockY = Math.floor(entity.posY);
        double blockZ = Math.floor(entity.posZ);

        // Create a block-sized bounding box (1x1x1) in world space relative to the camera
        return new AxisAlignedBB(
                blockX - px, blockY - py, blockZ - pz,
                blockX - px + 1, blockY - py + 1, blockZ - pz + 1
        );
    }
}

