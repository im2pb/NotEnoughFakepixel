/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.events.handlers;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@RegisterEvents
public class ConnectionEvents {

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        RepoHandler.refreshAllAsync();
    }
}
