package com.nef.notenoughfakepixel.features.skyblock.dungeons.terminals;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonManager;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class TerminalTracker {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final AxisAlignedBB GLOBAL_BOUNDS = new AxisAlignedBB(3, 100, 29, 120, 159, 150);
    private static final int LEVER_MAX = 2;
    private static final int DEVICE_MAX = 1;
    private static final int[] TERMINAL_MAX = {0, 4, 5, 4, 4};
    private static final Pattern DEVICE_PATTERN = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w+) completed a device! \\(\\d+/\\d+\\)$");
    private static final Pattern TERMINAL_PATTERN = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w+) activated a terminal! \\(\\d+/\\d+\\)$");
    private static final Pattern LEVER_PATTERN = Pattern.compile("^(?:\\[[^\\]]+\\] )?(\\w+) activated a lever! \\(\\d+/\\d+\\)$");
    private static final Pattern GATE_PATTERN = Pattern.compile("The gate will open in 5 seconds!");
    private static final String CORE_OPENING_MESSAGE = "The Core entrance is opening!";

    private final int[] terminalCounts = new int[5];
    private final int[] deviceCounts = new int[5];
    private final int[] leverCounts = new int[5];
    private final Map<String, Integer> playerTerminals = new HashMap<>();
    private final Map<String, Integer> playerDevices = new HashMap<>();
    private final Map<String, Integer> playerLevers = new HashMap<>();
    private int currentPhase = 1;

    public TerminalTracker() {
        resetCounts();
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!Config.feature.dungeons.terminals.dungeonsTerminalTracker) return;
        if (!DungeonManager.checkEssentialsF7()) return;

        String message = event.message.getUnformattedText();
        EntityPlayerSP player = MC.thePlayer;

        if (CORE_OPENING_MESSAGE.equals(message)) {
            sendSummaryToChat();
        }

        if (player == null || !SkyblockData.getCurrentLocation().isDungeon() || !isPlayerInBounds(player)) {
            return;
        }

        if (GATE_PATTERN.matcher(message).matches()) {
            if (currentPhase < 4) {
                currentPhase++;
            }
            return;
        }

        Matcher deviceMatcher = DEVICE_PATTERN.matcher(message);
        if (deviceMatcher.matches()) {
            if (deviceCounts[currentPhase] < DEVICE_MAX) {
                String name = deviceMatcher.group(1);
                deviceCounts[currentPhase]++;
                incrementPlayerCount(playerDevices, name);
            }
            return;
        }

        Matcher terminalMatcher = TERMINAL_PATTERN.matcher(message);
        if (terminalMatcher.matches()) {
            if (terminalCounts[currentPhase] < TERMINAL_MAX[currentPhase]) {
                String name = terminalMatcher.group(1);
                terminalCounts[currentPhase]++;
                incrementPlayerCount(playerTerminals, name);
            }
            return;
        }

        Matcher leverMatcher = LEVER_PATTERN.matcher(message);
        if (leverMatcher.matches() && leverCounts[currentPhase] < LEVER_MAX) {
            String name = leverMatcher.group(1);
            leverCounts[currentPhase]++;
            incrementPlayerCount(playerLevers, name);
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Text event) {
        if (!Config.feature.dungeons.terminals.dungeonsTerminalTracker) return;
        if (!DungeonManager.checkEssentialsF7()) return;

        EntityPlayerSP player = MC.thePlayer;
        if (player == null || !SkyblockData.getCurrentLocation().isDungeon() || !isPlayerInBounds(player)) {
            return;
        }

        float scale = Config.feature.dungeons.terminals.dungeonsTerminalTrackerScale;
        ScaledResolution sr = new ScaledResolution(MC);
        int x = Config.feature.dungeons.terminals.terminalTrackerPos.getAbsX(sr, 1);
        int y = Config.feature.dungeons.terminals.terminalTrackerPos.getAbsY(sr, 1);
        float scaledX = x / scale;
        float scaledY = y / scale;
        float lineStep = 14.0F / scale;
        int color = ColorUtils.getColor(Config.feature.dungeons.terminals.dungeonsTerminalTrackerColor).getRGB();

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        MC.fontRendererObj.drawStringWithShadow("Phase " + currentPhase, scaledX, scaledY, color);
        MC.fontRendererObj.drawStringWithShadow("Terminals: " + terminalCounts[currentPhase] + "/" + TERMINAL_MAX[currentPhase], scaledX, scaledY + lineStep, color);
        MC.fontRendererObj.drawStringWithShadow("Devices: " + deviceCounts[currentPhase] + "/" + DEVICE_MAX, scaledX, scaledY + (lineStep * 2.0F), color);
        MC.fontRendererObj.drawStringWithShadow("Levers: " + leverCounts[currentPhase] + "/" + LEVER_MAX, scaledX, scaledY + (lineStep * 3.0F), color);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        resetCounts();
        playerTerminals.clear();
        playerDevices.clear();
        playerLevers.clear();
    }

    private void sendSummaryToChat() {
        if (MC.thePlayer == null || playerTerminals.isEmpty()) return;

        for (Map.Entry<String, Integer> entry : playerTerminals.entrySet()) {
            String name = entry.getKey();
            int terminals = entry.getValue();
            int devices = getPlayerCount(playerDevices, name);
            int levers = getPlayerCount(playerLevers, name);
            MC.thePlayer.addChatMessage(new ChatComponentText(
                    "§b[§aNEF§b] §6" + name + "§7, Terminals: §f" + terminals
                            + "§7, Devices: §f" + devices
                            + "§7, Levers: §f" + levers + "§7."
            ));
        }
    }

    private void resetCounts() {
        for (int phase = 1; phase <= 4; phase++) {
            terminalCounts[phase] = 0;
            deviceCounts[phase] = 0;
            leverCounts[phase] = 0;
        }
        currentPhase = 1;
    }

    private static void incrementPlayerCount(Map<String, Integer> counts, String name) {
        counts.put(name, getPlayerCount(counts, name) + 1);
    }

    private static int getPlayerCount(Map<String, Integer> counts, String name) {
        Integer value = counts.get(name);
        return value == null ? 0 : value;
    }

    private static boolean isPlayerInBounds(EntityPlayerSP player) {
        double x = player.posX;
        double y = player.posY;
        double z = player.posZ;
        return x >= GLOBAL_BOUNDS.minX && x <= GLOBAL_BOUNDS.maxX
                && y >= GLOBAL_BOUNDS.minY && y <= GLOBAL_BOUNDS.maxY
                && z >= GLOBAL_BOUNDS.minZ && z <= GLOBAL_BOUNDS.maxZ;
    }
}
