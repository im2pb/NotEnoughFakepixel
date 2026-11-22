/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.mining.service.MiningOverlayDataController;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Forge adapter that refreshes mining HUD data once at the end of each client tick. */
@RegisterEvents
public final class MiningOverlayDataUpdater {
    private final MiningOverlayDataController controller = MiningOverlayDataController.getInstance();

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!Config.feature.mining.miningOverlay) {
            controller.reset();
            return;
        }

        // Do not keep a previous world's HUD alive during disconnect/menu gaps.
        // This also covers teardown paths where a remote WorldEvent is not observed.
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.theWorld == null || minecraft.thePlayer == null) {
            controller.reset();
            return;
        }

        controller.refresh();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // Ignore integrated-server world unloads; this controller belongs to the client HUD.
        if (event != null && event.world != null && event.world.isRemote) {
            controller.reset();
        }
    }
}
