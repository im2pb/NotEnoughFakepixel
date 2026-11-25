package com.nef.notenoughfakepixel.features.debug;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Debug;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.serverdata.SkyblockDiagnostics;
import com.nef.notenoughfakepixel.utils.Logger;
import com.nef.notenoughfakepixel.variables.Area;
import com.nef.notenoughfakepixel.variables.Gamemode;
import com.nef.notenoughfakepixel.variables.Location;
import com.nef.notenoughfakepixel.variables.Slayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

@RegisterEvents
public final class DebugModule implements ClientModInitializer {
    private TraceContext lastContext;
    private boolean copyKeyDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::traceSkyblock);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> client.getSoundManager()
                .registerListener((sound, soundSet, range) -> client.execute(() -> traceSound(client, sound))));
        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof HandledScreen<?> inventory)) return;

            copyKeyDown = false;
            ScreenKeyboardEvents.afterKeyPress(screen).register((current, key, scanCode, modifiers) -> {
                if (key != GLFW.GLFW_KEY_RIGHT_CONTROL || copyKeyDown) return;
                copyKeyDown = true;
                copyHoveredItem(client, inventory);
            });
            ScreenKeyboardEvents.afterKeyRelease(screen).register((current, key, scanCode, modifiers) -> {
                if (key == GLFW.GLFW_KEY_RIGHT_CONTROL) copyKeyDown = false;
            });
        });
    }

    public static void logLocation() {
        Logger.report(SkyblockDiagnostics.location());
    }

    public static void logGamemode() {
        Logger.report(SkyblockDiagnostics.gamemode());
    }

    public static void logScoreboard() {
        Logger.report(SkyblockDiagnostics.scoreboard(MinecraftClient.getInstance()));
    }

    public static void logTabList() {
        Logger.report(SkyblockDiagnostics.tabList(MinecraftClient.getInstance()));
    }

    public static void logHeldItem() {
        Logger.report(SkyblockDiagnostics.heldItem(MinecraftClient.getInstance()));
    }

    public static void logSkyblockData() {
        Logger.report(SkyblockDiagnostics.snapshot());
    }

    public static void copySkyblockData() {
        MinecraftClient.getInstance().keyboard.setClipboard(Formatting.strip(SkyblockDiagnostics.snapshot().getString()));
        Logger.feedback("Copied the SkyBlock data report to the clipboard.");
    }

    public static void logClientEnvironment() {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean forced = ClientConfig.configuration().debug.forcePojav;
        String os = System.getProperty("os.name", "unknown");
        boolean pojav = forced || System.getenv("POJAV_RENDERER") != null || os.toLowerCase(Locale.ROOT).contains("android");
        String connection = client.getNetworkHandler() == null ? "disconnected"
                : client.isInSingleplayer() ? "singleplayer" : "multiplayer";

        String details = "\nConnection %s\nJava %s\nOperating system %s\nPojav %s%s"
                .formatted(connection, System.getProperty("java.version", "unknown"), os, pojav, forced ? " (forced)" : "");
        Logger.report(Text.empty().append(Text.literal("Client environment").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(details));
    }

    private void traceSkyblock(MinecraftClient client) {
        if (client.player == null && lastContext == null) return;

        Debug settings = ClientConfig.configuration().debug;
        if (!settings.debug || !settings.traceSkyblockChanges || (!settings.chatLogs && !settings.consoleLogs)) {
            lastContext = null;
            return;
        }
        if (client.player == null || !traceAllowed(settings)) {
            if (lastContext != null && lastContext.mode().isSkyblock()) Logger.trace("Left SkyBlock.");
            lastContext = null;
            return;
        }

        TraceContext current = new TraceContext(SkyblockData.currentGamemode(), SkyblockData.currentProfile(),
                SkyblockData.currentLocation(), SkyblockData.currentArea(), SkyblockData.sandbox(), SkyblockData.currentSlayer(),
                SkyblockData.slayerActive(), SkyblockData.bossActive());
        if (current.equals(lastContext)) return;

        lastContext = current;
        Logger.trace(SkyblockDiagnostics.context());
    }

    private static void traceSound(MinecraftClient client, SoundInstance sound) {
        if (client.player == null) return;

        Debug settings = ClientConfig.configuration().debug;
        if (!settings.debug || !settings.showSounds || !traceAllowed(settings)) return;

        Logger.trace(Text.literal("Sound ").formatted(Formatting.AQUA)
                .append(Text.literal(sound.getId().toString()).formatted(Formatting.WHITE))
                .append(Text.literal(String.format(Locale.ROOT, " | Category %s | Volume %.2f | Pitch %.2f",
                        sound.getCategory(), sound.getVolume(), sound.getPitch())).formatted(Formatting.GRAY)));
    }

    private static boolean traceAllowed(Debug settings) {
        return settings.enableOutOfFakepixel || SkyblockData.isSkyblock();
    }

    private static void copyHoveredItem(MinecraftClient client, HandledScreen<?> inventory) {
        if (client.world == null || !ClientConfig.configuration().debug.debugCopyNbt) return;

        Slot slot = inventory.focusedSlot;
        if (slot == null || !slot.hasStack()) return;

        try {
            client.keyboard.setClipboard(slot.getStack().encode(client.world.getRegistryManager()).toString());
            Logger.feedback("Copied the hovered item's data to the clipboard.");
        } catch (RuntimeException error) {
            Logger.logErrorConsole("Could not copy hovered item data: " + error.getMessage());
            Logger.feedbackError("Could not copy the hovered item's data.");
        }
    }

    private record TraceContext(Gamemode mode, String profile, Location location, Area area, boolean sandbox,
                                Slayer slayer, boolean questActive, boolean bossActive) {
    }
}
