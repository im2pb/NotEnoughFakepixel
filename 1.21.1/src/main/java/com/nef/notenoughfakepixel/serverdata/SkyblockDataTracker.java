package com.nef.notenoughfakepixel.serverdata;

import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockSidebar.SlayerPhase;
import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import com.nef.notenoughfakepixel.variables.Gamemode;
import com.nef.notenoughfakepixel.variables.Slayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public final class SkyblockDataTracker implements ClientModInitializer {
    private static final int TAB_INTERVAL = 20;
    private static final String PROFILE_PREFIX = "You are playing on profile:";
    private static final Pattern SLAYER_LEVEL = Pattern.compile("\\bSlayer LVL\\s+(\\d+)(?=\\s|-|$)");
    private static final Pattern SLAYER_XP = Pattern.compile("\\bNext LVL in\\s+([0-9,]+)(?=\\s|$)");
    private static final Pattern RNG_METER = Pattern.compile("\\bRNGesus Meter:\\s*(\\d+(?:\\.\\d+)?)(?=\\s*%|$)");

    private ClientWorld world;
    private int ticksUntilTab;
    private boolean tabIndicatesSkyblock;
    private SkyblockSidebar lastSidebar;
    private NbtComponent lastHeldItemData;
    private SlayerPhase slayerPhase = SlayerPhase.NONE;
    private Long bossStartedAt;
    private boolean questResolved;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(this::resetConnection));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::resetConnection));
    }

    private void onClientTick(MinecraftClient client) {
        updateWorld(client.world);
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null || client.isInSingleplayer() || client.world == null || client.player == null) return;

        SkyblockSidebar sidebar = SkyblockSidebar.read(client);
        Gamemode mode = sidebar.gamemode();
        if (!mode.isSkyblock() && (mode != Gamemode.LOBBY || sidebar.hasLobbyTitle())) {
            leaveSkyblock(mode);
            return;
        }

        SkyblockTabList tabList = null;
        if (ticksUntilTab-- <= 0 || (mode.isSkyblock() && !SkyblockData.isSkyblock())) {
            tabList = SkyblockTabList.read(client, handler);
            tabIndicatesSkyblock = tabList.indicatesSkyblock();
            ticksUntilTab = TAB_INTERVAL - 1;
        }
        if (tabIndicatesSkyblock) mode = Gamemode.SKYBLOCK;
        if (!mode.isSkyblock()) {
            leaveSkyblock(mode);
            return;
        }

        NbtComponent heldItemData = client.player.getMainHandStack().get(DataComponentTypes.CUSTOM_DATA);
        String itemId = heldItemData == lastHeldItemData ? SkyblockData.lastHandItemId() : SkyblockItemData.id(heldItemData);
        lastHeldItemData = heldItemData;
        SkyblockData.updateContext(mode, itemId);
        if (tabList != null) SkyblockData.applyTabList(tabList);
        if (!sidebar.equals(lastSidebar)) {
            lastSidebar = sidebar;
            sidebar.updateWorldData();
            if (sidebar == SkyblockSidebar.EMPTY) {
                SkyblockData.updateSlayerActivity(false, false);
            } else {
                updateSlayer(sidebar.slayer(), sidebar.slayerPhase());
            }
        }
    }

    private void onGameMessage(Text message, boolean overlay) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (overlay || client.isInSingleplayer() || client.getNetworkHandler() == null) return;
        updateWorld(client.world);

        for (String rawLine : message.getString().lines().toList()) {
            String line = SkyblockText.normalize(rawLine);
            int profileStart = line.indexOf(PROFILE_PREFIX);
            if (profileStart >= 0) {
                String profile = line.substring(profileStart + PROFILE_PREFIX.length()).strip();
                if (!profile.isEmpty() && SkyblockData.changeProfile(profile)) {
                    lastSidebar = null;
                    resetQuest();
                }
            } else if (SkyblockData.isSkyblock()) {
                handleSlayerMessage(line);
            }
        }
    }

    private void updateSlayer(Slayer slayer, SlayerPhase phase) {
        if (SkyblockData.observeSlayer(slayer)) resetQuest();

        if (phase != slayerPhase) {
            if (phase == SlayerPhase.SPAWNING) {
                startQuest();
            } else if (phase == SlayerPhase.BOSS) {
                if (questResolved) startQuest();
                if (bossStartedAt == null) bossStartedAt = System.nanoTime();
            } else if (phase == SlayerPhase.FAILED) {
                finishQuest(false);
            }
            slayerPhase = phase;
        }

        boolean active = !questResolved && phase != SlayerPhase.SLAIN
                && (slayer != Slayer.NONE || phase == SlayerPhase.BOSS || phase == SlayerPhase.SPAWNING);
        SkyblockData.updateSlayerActivity(active, active && phase == SlayerPhase.BOSS);
    }

    private void handleSlayerMessage(String message) {
        if (message.contains("SLAYER QUEST STARTED!")) {
            startQuest();
            SkyblockData.updateSlayerActivity(true, false);
        } else if (message.contains("SLAYER QUEST COMPLETE!")) {
            finishQuest(true);
        } else if (message.contains("SLAYER QUEST FAILED!")) {
            finishQuest(false);
        }

        if (SkyblockData.currentSlayer() == Slayer.NONE) return;
        Matcher meter = RNG_METER.matcher(message);
        if (meter.find()) SkyblockData.recordRngesusMeter(Float.parseFloat(meter.group(1)));
        Matcher levelMatch = SLAYER_LEVEL.matcher(message);
        if (!levelMatch.find()) return;

        int level = SkyblockText.parseNonnegativeInt(levelMatch.group(1));
        Matcher xpMatch = SLAYER_XP.matcher(message);
        int xpRemaining = xpMatch.find() ? SkyblockText.parseNonnegativeInt(xpMatch.group(1)) : -1;
        SkyblockData.updateSlayerProgress(level, xpRemaining);
    }

    private void startQuest() {
        bossStartedAt = null;
        questResolved = false;
    }

    private void finishQuest(boolean success) {
        if (questResolved) return;
        if (success) {
            double seconds = bossStartedAt == null ? 0 : (System.nanoTime() - bossStartedAt) / 1_000_000_000.0;
            SkyblockData.recordSlayerCompletion(seconds);
        }
        bossStartedAt = null;
        questResolved = true;
        SkyblockData.updateSlayerActivity(false, false);
    }

    private void resetQuest() {
        startQuest();
        slayerPhase = SlayerPhase.NONE;
    }

    private void updateWorld(ClientWorld current) {
        if (world == current) return;
        world = current;
        resetWorld();
    }

    private void leaveSkyblock(Gamemode mode) {
        if (SkyblockData.isSkyblock()) resetWorld();
        SkyblockData.updateContext(mode, "");
    }

    private void resetWorld() {
        SkyblockData.clearWorldData();
        ticksUntilTab = 0;
        tabIndicatesSkyblock = false;
        lastSidebar = null;
        lastHeldItemData = null;
        resetQuest();
    }

    private void resetConnection() {
        world = null;
        SkyblockData.clearProfile();
        resetWorld();
    }
}
