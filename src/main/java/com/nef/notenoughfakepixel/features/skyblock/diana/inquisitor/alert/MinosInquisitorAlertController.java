/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.alert;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.diana.burrows.ParticleProcessor;
import com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.detection.InquisitorLocationRegistry;
import com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.detection.InquisitorOutlineTracker;
import com.nef.notenoughfakepixel.features.skyblock.diana.services.DianaSchedulerService;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import com.nef.notenoughfakepixel.utils.ScoreboardUtils;
import com.nef.notenoughfakepixel.utils.SoundUtils;
import com.nef.notenoughfakepixel.utils.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

/** Handles Minos Inquisitor party alerts and the lifetime of the shared Inquisitor waypoint. */
public final class MinosInquisitorAlertController {
    /* Match the payload, not the full formatted line: ranks, prefixes and trailing formatting vary. */
    private static final Pattern PARTY_CHAT = Pattern.compile(
            "(?i)Minos\\s+Inquisitor\\s+found\\s+at\\s+.*?\\bx:\\s*(-?\\d+)\\s*,\\s*y:\\s*(-?\\d+)\\s*,\\s*z:\\s*(-?\\d+)\\s+in\\s+HUB-(\\d+)"
    );
    private static final String INQUISITOR_SOUND = "mob.enderdragon.growl";
    private static final String WAYPOINT_TYPE = "MINOS";
    private static final long ALERT_COOLDOWN_SECONDS = 63L;
    private static final long DUPLICATE_ALERT_WINDOW_NS = TimeUnit.SECONDS.toNanos(5L);

    private final InquisitorLocationRegistry locations;
    private final DianaSchedulerService scheduler;
    private final InquisitorOutlineTracker tracker;
    private Instant lastCaptureTime = Instant.EPOCH;
    private EntityLivingBase lastCapturedEntity;
    private long waypointGeneration;
    private String lastHandledAlertKey;
    private long lastHandledAlertNs;

    public MinosInquisitorAlertController(InquisitorLocationRegistry locations, DianaSchedulerService scheduler,
                                          InquisitorOutlineTracker tracker) {
        if (locations == null || scheduler == null || tracker == null) {
            throw new IllegalArgumentException("Inquisitor dependencies cannot be null");
        }
        this.locations = locations;
        this.scheduler = scheduler;
        this.tracker = tracker;
    }

    public void initializeLocations() {
        locations.initialize();
    }

    /** Local discovery starts the timer immediately; it no longer depends on a party-chat echo. */
    public void onEntityRendered(EntityLivingBase entity, ParticleProcessor processor) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || entity == null || processor == null) return;
        if (!Config.feature.diana.dianaMinosInquisitorAlert) return;
        // The name is commonly attached to an Armor Stand, not the living mob rendered here.
        // Use the tracker's verified association so local alert/countdown works in both formats.
        if (!tracker.contains(entity)) return;
        if (entity == lastCapturedEntity) return;

        int x = entity.getPosition().getX();
        int y = entity.getPosition().getY() - 2;
        int z = entity.getPosition().getZ();
        int hubNumber = ScoreboardUtils.getHubNumber();
        if (hubNumber < 0) return;

        // A party announcement can arrive before this client's entity tracker sees the mob.
        // Suppress the local echo before sending another /pc message or title.
        String alertKey = hubNumber + ":" + x + ":" + y + ":" + z;
        long nowNs = System.nanoTime();
        if (alertKey.equals(lastHandledAlertKey)
                && nowNs - lastHandledAlertNs >= 0L
                && nowNs - lastHandledAlertNs < DUPLICATE_ALERT_WINDOW_NS) {
            lastCaptureTime = Instant.now();
            lastCapturedEntity = entity;
            return;
        }

        Instant now = Instant.now();
        if (lastCaptureTime != null && now.isBefore(lastCaptureTime.plusSeconds(ALERT_COOLDOWN_SECONDS))) return;

        mc.ingameGUI.displayTitle("¡Inquisidor detectado!", "", 10, 40, 20);
        String location = locations.findNearest(x, y, z);
        String where = location != null && !location.isEmpty()
                ? " at " + location + ", x:" + x + ", y:" + y + ", z:" + z
                : " at x:" + x + ", y:" + y + ", z:" + z;
        mc.thePlayer.sendChatMessage("/pc Minos Inquisitor found" + where + " in HUB-" + hubNumber);

        lastCaptureTime = now;
        lastCapturedEntity = entity;
        beginWaypointLifecycle(x, y, z, hubNumber, processor);
    }

    public void onPartyChat(ClientChatReceivedEvent event, ParticleProcessor processor) {
        if (event == null || event.message == null || processor == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc == null ? null : mc.thePlayer;
        if (player == null || !SkyblockData.getCurrentLocation().isHub()) return;
        if (!Config.feature.diana.dianaMinosInquisitorAlert) return;
        if (ChatUtils.middleBar.matcher(event.message.getFormattedText()).matches()) return;

        String plainText = event.message.getUnformattedText();
        if (plainText == null || !plainText.toLowerCase(java.util.Locale.ROOT).contains("party")) return;
        Matcher matcher = PARTY_CHAT.matcher(plainText);
        if (!matcher.find()) return;

        int x;
        int y;
        int z;
        int hubNumber;
        try {
            x = Integer.parseInt(matcher.group(1));
            y = Integer.parseInt(matcher.group(2));
            z = Integer.parseInt(matcher.group(3));
            hubNumber = Integer.parseInt(matcher.group(4));
        } catch (NumberFormatException ignored) {
            return;
        }

        if (ScoreboardUtils.getHubNumber() != hubNumber) return;
        beginWaypointLifecycle(x, y, z, hubNumber, processor);
    }

    /** Starts one 60-second lifecycle and suppresses the duplicate party echo of a local detection. */
    private void beginWaypointLifecycle(int x, int y, int z, int hubNumber, ParticleProcessor processor) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || processor == null) return;
        if (!SkyblockData.getCurrentLocation().isHub() || ScoreboardUtils.getHubNumber() != hubNumber) return;

        String alertKey = hubNumber + ":" + x + ":" + y + ":" + z;
        long nowNs = System.nanoTime();
        if (alertKey.equals(lastHandledAlertKey)
                && nowNs - lastHandledAlertNs >= 0L
                && nowNs - lastHandledAlertNs < DUPLICATE_ALERT_WINDOW_NS) {
            return;
        }
        lastHandledAlertKey = alertKey;
        lastHandledAlertNs = nowNs;

        int[] playerCoords = {
                mc.thePlayer.getPosition().getX(),
                mc.thePlayer.getPosition().getY(),
                mc.thePlayer.getPosition().getZ()
        };
        SoundUtils.playSound(playerCoords, INQUISITOR_SOUND, 3.0f, 0.8f);

        /* A new report replaces only the old Inquisitor marker, never a nearby burrow waypoint. */
        processor.removeWaypointsOfType(WAYPOINT_TYPE);
        Waypoint waypoint = new Waypoint(WAYPOINT_TYPE, new int[]{x, y, z});
        processor.addWaypoint(waypoint);
        final long currentGeneration = ++waypointGeneration;

        scheduler.scheduleOnClientThread(() -> {
            if (!isCurrentWaypoint(currentGeneration)) return;
            sendCountdownMessage("Minos Inquisitor despawns in 30 seconds!");
        }, 30, TimeUnit.SECONDS);
        scheduler.scheduleOnClientThread(() -> {
            if (!isCurrentWaypoint(currentGeneration)) return;
            sendCountdownMessage("Minos Inquisitor despawns in 10 seconds!");
        }, 50, TimeUnit.SECONDS);
        scheduler.scheduleOnClientThread(() -> {
            if (!isCurrentWaypoint(currentGeneration)) return;
            processor.deleteWaypoint(waypoint);
            waypointGeneration++;
        }, 60, TimeUnit.SECONDS);
    }

    /** Called only after the dying entity has been verified as this Inquisitor or its named marker. */
    public void onInquisitorDeath(ParticleProcessor processor) {
        waypointGeneration++; // Invalidates old countdown and fallback-expiry callbacks.
        if (processor != null) processor.removeWaypointsOfType(WAYPOINT_TYPE);
    }

    private boolean isCurrentWaypoint(long generation) {
        return generation == waypointGeneration;
    }

    private void sendCountdownMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.YELLOW + message));
    }

    public void reset() {
        waypointGeneration++;
        lastCaptureTime = Instant.EPOCH;
        lastCapturedEntity = null;
        lastHandledAlertKey = null;
        lastHandledAlertNs = 0L;
        locations.clear();
    }
}
