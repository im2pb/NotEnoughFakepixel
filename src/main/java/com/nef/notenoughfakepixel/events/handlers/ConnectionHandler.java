/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.events.handlers;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.NEFClientConnectedToServerEvent;
import com.nef.notenoughfakepixel.utils.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@RegisterEvents
public class ConnectionHandler {

    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY_MS = 1500L;
    private static final long DECODER_ERROR_VALID_MS = 15000L;
    private static final long STABLE_WORLD_RESET_MS = 30000L;
    private static final long AUTO_RECONNECT_CONNECT_WINDOW_MS = 15000L;

    /** Written on the Netty thread and consumed by Forge's client thread. */
    private static volatile long decoderErrorAt = -1L;
    private static volatile ServerData lastServerData;
    private static volatile long reconnectAt = -1L;
    /** Non-negative only while a scheduled auto-reconnect launch is waiting for connect event. */
    private static volatile long autoReconnectLaunchDeadline = -1L;

    /** Client-thread state. Attempts survive automatic retries and reset on a manual connect or stable world. */
    private static int reconnectAttempts;
    private static long worldAvailableSince = -1L;
    private static String lastServerAddress;

    @SubscribeEvent
    public void onServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        NEFClientConnectedToServerEvent nefEvent = NEFClientConnectedToServerEvent.create(event);
        if (nefEvent.manager != null && nefEvent.manager.channel() != null) {
            if (nefEvent.manager.channel().pipeline().get("nef_packet_handler") == null) {
                try {
                    nefEvent.manager.channel().pipeline().addBefore(
                            "packet_handler", "nef_packet_handler", new PacketHandler()
                    );
                    Logger.logConsole("Added packet handler to channel pipeline.");
                } catch (Throwable t) {
                    Logger.logErrorConsole("Could not install packet handler: " + t.getMessage());
                }
            }
        }

        ServerData currentServer = Minecraft.getMinecraft().getCurrentServerData();
        String address = currentServer == null ? null : normalizeAddress(currentServer.serverIP);
        long now = System.currentTimeMillis();
        boolean autoReconnectConnect = autoReconnectLaunchDeadline >= now;
        autoReconnectLaunchDeadline = -1L;
        // A user-initiated connect starts a fresh retry budget, even when reconnecting
        // to the same server after an earlier sequence exhausted the automatic attempts.
        if (!autoReconnectConnect || (lastServerAddress != null && address != null
                && !lastServerAddress.equals(address))) {
            reconnectAttempts = 0;
        }
        lastServerAddress = address;
        // Clear stale multiplayer data when the new connection is not tied to a saved server.
        lastServerData = currentServer;

        decoderErrorAt = -1L;
        reconnectAt = -1L;
        worldAvailableSince = -1L;
    }

    @SubscribeEvent
    public void onServerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        long now = System.currentTimeMillis();
        long errorAt = decoderErrorAt;
        decoderErrorAt = -1L;

        // A stale decoder flag must not trigger a reconnect after an unrelated/manual disconnect.
        if (errorAt < 0L || now - errorAt > DECODER_ERROR_VALID_MS || lastServerData == null) {
            return;
        }
        if (reconnectAt >= 0L) return;

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            reconnectAt = -1L;
            Logger.logConsole("Auto-reconnect: max attempts reached, giving up.");
            return;
        }

        reconnectAttempts++;
        reconnectAt = now + RECONNECT_DELAY_MS;
        Logger.logConsole("Auto-reconnect: scheduling attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        long now = System.currentTimeMillis();

        // Do not reset the retry budget at channel-connect time: unstable reconnect loops
        // otherwise reset the counter before every failed login. Require 30 seconds in-world.
        if (mc.theWorld == null) {
            worldAvailableSince = -1L;
        } else if (worldAvailableSince < 0L) {
            worldAvailableSince = now;
        } else if (reconnectAttempts > 0 && now - worldAvailableSince >= STABLE_WORLD_RESET_MS) {
            reconnectAttempts = 0;
            worldAvailableSince = now;
            Logger.logConsole("Auto-reconnect: connection stable; retry counter reset.");
        }

        if (autoReconnectLaunchDeadline >= 0L && now > autoReconnectLaunchDeadline) {
            autoReconnectLaunchDeadline = -1L;
        }

        long scheduledAt = reconnectAt;
        if (scheduledAt < 0L || now < scheduledAt) return;

        // Consume the schedule before opening a screen so a tick or event cannot open duplicates.
        reconnectAt = -1L;
        if (mc.theWorld != null || mc.currentScreen instanceof GuiConnecting || lastServerData == null) return;

        // Respect a user who has already returned to the main menu instead of forcing a reconnect.
        if (mc.currentScreen instanceof GuiMainMenu) return;

        // The Forge connected event is raised shortly after channel activation; a short
        // window avoids mistaking a later user-initiated connect for this auto-retry.
        autoReconnectLaunchDeadline = now + AUTO_RECONNECT_CONNECT_WINDOW_MS;
        mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, lastServerData));
    }

    public static void onDecoderError() {
        decoderErrorAt = System.currentTimeMillis();
    }

    private static String normalizeAddress(String address) {
        return address == null ? null : address.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
