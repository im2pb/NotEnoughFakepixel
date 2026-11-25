package com.nef.notenoughfakepixel.events.handlers;

import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.NEFClientConnectedToServerEvent;
import com.nef.notenoughfakepixel.utils.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@RegisterEvents
public class ConnectionHandler {

    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    private static volatile boolean decoderErrorOccurred = false;
    private static volatile ServerData lastServerData = null;
    private static volatile long reconnectAt = -1;
    private static int reconnectAttempts = 0;

    @SubscribeEvent
    public void onServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        NEFClientConnectedToServerEvent nefEvent = NEFClientConnectedToServerEvent.create(event);
        nefEvent.manager.channel().pipeline().addBefore("packet_handler", "nef_packet_handler", new PacketHandler());
        Logger.logConsole("Added packet handler to channel pipeline.");

        lastServerData = Minecraft.getMinecraft().getCurrentServerData();
        decoderErrorOccurred = false;
        reconnectAttempts = 0;
        reconnectAt = -1;
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        ApiHandler.init();
    }

    @SubscribeEvent
    public void onServerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (!decoderErrorOccurred || lastServerData == null) {
            decoderErrorOccurred = false;
            return;
        }
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            decoderErrorOccurred = false;
            Logger.logConsole("Auto-reconnect: max attempts reached, giving up.");
            return;
        }
        reconnectAttempts++;
        reconnectAt = System.currentTimeMillis() + 1500;
        decoderErrorOccurred = false;
        Logger.logConsole("Auto-reconnect: scheduling attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (reconnectAt < 0 || System.currentTimeMillis() < reconnectAt) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != null) {
            reconnectAt = -1;
            return;
        }

        reconnectAt = -1;
        mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, lastServerData));
    }

    public static void onDecoderError() {
        decoderErrorOccurred = true;
    }

}