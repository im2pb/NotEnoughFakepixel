/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.google.gson.Gson;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.handlers.RepoHandler;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.ConfigHandler;
import com.nef.notenoughfakepixel.utils.RenderUtils;
import com.nef.notenoughfakepixel.variables.Gamemode;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RegisterEvents
public class FairySouls {

    private String island;
    private String cachedMarkerIsland;
    private List<String> cachedMarkerSource;
    private List<SoulMarker> cachedMarkers = Collections.emptyList();
    private FairySoulData cachedGainedOwner;
    private List<String> cachedGainedSource;
    private int cachedGainedSize = -1;
    private Set<String> cachedGainedSet = Collections.emptySet();
    private static volatile FairySoulData cachedSoulData;

    private static String islandKey(Location location) {
        if (location == null) return null;
        switch (location) {
            case HUB: return "hub";
            case SPIDERS_DEN: return "spider";
            case CRIMSON_ISLE: return "crimson";
            case THE_END: return "end";
            case PARK: return "park";
            case BARN: return "farming";
            case GOLD_MINE: return "gold";
            case DUNGEON_HUB: return "dungeon_hub";
            case JERRY: return "winter";
            case DWARVEN: return "dwarven";
            default: return null;
        }
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        island = null;
        if (SkyblockData.getCurrentGamemode() != Gamemode.SKYBLOCK) return;
        if (!Config.feature.waypoints.fairySoulWaypoints) return;

        // Do not trigger repository warmup/download work while this feature is disabled.
        final FairySoulData all = getAllSouls();
        final Map<String, List<String>> locs = (all != null) ? all.locations : null;
        List<String> souls = Collections.emptyList();
        Location currentIsland = SkyblockData.getCurrentLocation();
        island = islandKey(currentIsland);
        if (locs != null && island != null) souls = locs.getOrDefault(island, Collections.<String>emptyList());

        if (island == null || souls.isEmpty()) return;

        List<SoulMarker> markers = getCachedMarkers(island, souls);
        if (markers.isEmpty()) return;

        FairySoulData localData = getSoulData();
        Set<String> gained = getCachedGainedSet(localData, island);
        Color base = ColorUtils.getColor(Config.feature.waypoints.fairySoulWaypointsColor);
        // Alpha is composed with bit operations so the render loop does not allocate a Color per frame.
        int beamRgb = (102 << 24) | (base.getRGB() & 0x00FFFFFF);

        GlStateManager.pushMatrix();
        try {
            for (SoulMarker marker : markers) {
                // Keep the original behavior: render only uncollected souls; a completed island is clear.
                if (gained.contains(marker.key)) continue;
                GlStateManager.color(1f, 1f, 1f, 1f);
                RenderUtils.highlightBlock(marker.pos, base, true, e.partialTicks);
                GlStateManager.disableCull();
                GlStateManager.disableDepth();
                RenderUtils.renderBeaconBeam(marker.pos, beamRgb, 1.0f, e.partialTicks);
                GlStateManager.enableDepth();
                GlStateManager.enableCull();
                GlStateManager.enableTexture2D();
            }
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e == null || e.message == null) return;
        String msg = StringUtils.stripControlCodes(e.message.getUnformattedText()).trim();
        if (!msg.equalsIgnoreCase("SOUL! You found a Fairy Soul!")
                && !msg.equalsIgnoreCase("You already found that Fairy Soul!")) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (SkyblockData.getCurrentGamemode() != Gamemode.SKYBLOCK) return;
        String currentIsland = islandKey(SkyblockData.getCurrentLocation());
        if (currentIsland == null || mc.thePlayer == null || mc.theWorld == null) return;

        FairySoulData allSouls = getAllSouls();
        FairySoulData localData = getSoulData();
        if (allSouls == null || allSouls.locations == null || localData == null || localData.locations == null) return;

        List<String> source = allSouls.locations.get(currentIsland);
        if (source == null || source.isEmpty()) return;

        String soul = null;
        double closestDistSq = 25.0;
        List<SoulMarker> markers = getCachedMarkers(currentIsland, source);
        for (SoulMarker marker : markers) {
            double distSq = marker.pos.distanceSq(mc.thePlayer.getPosition());
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                soul = marker.key;
            }
        }

        List<String> gainedSouls = localData.locations.computeIfAbsent(currentIsland, k -> new ArrayList<String>());
        if (soul != null && !gainedSouls.contains(soul)) {
            gainedSouls.add(soul);
            localData.locations.put(currentIsland, gainedSouls);
            localData.soulCount++;
            saveSoulData(localData);
        }
    }

    private List<SoulMarker> getCachedMarkers(String islandKey, List<String> source) {
        if (source == cachedMarkerSource && Objects.equals(islandKey, cachedMarkerIsland)) return cachedMarkers;

        List<SoulMarker> parsed = new ArrayList<>(source.size());
        for (String coordinate : source) {
            SoulMarker marker = parseSoulMarker(coordinate);
            if (marker != null) parsed.add(marker);
        }
        cachedMarkerIsland = islandKey;
        cachedMarkerSource = source;
        cachedMarkers = Collections.unmodifiableList(parsed);
        return cachedMarkers;
    }

    private Set<String> getCachedGainedSet(FairySoulData data, String islandKey) {
        List<String> gained = data == null || data.locations == null || islandKey == null
                ? null : data.locations.get(islandKey);
        int size = gained == null ? 0 : gained.size();
        if (data != cachedGainedOwner || gained != cachedGainedSource || size != cachedGainedSize) {
            cachedGainedOwner = data;
            cachedGainedSource = gained;
            cachedGainedSize = size;
            cachedGainedSet = gained == null || gained.isEmpty()
                    ? Collections.<String>emptySet() : new HashSet<>(gained);
        }
        return cachedGainedSet;
    }

    private static SoulMarker parseSoulMarker(String coordinate) {
        if (coordinate == null) return null;
        String[] parts = coordinate.split(",");
        if (parts.length != 3) return null;
        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());
            return new SoulMarker(coordinate, new BlockPos(x, y, z));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static final class SoulMarker {
        final String key;
        final BlockPos pos;

        SoulMarker(String key, BlockPos pos) {
            this.key = key;
            this.pos = pos;
        }
    }

    public static class FairySoulData {

        String description;
        int soulCount;
        Map<String, List<String>> locations;

        public FairySoulData(String desc, int souls, Map<String, List<String>> locs) {
            description = desc;
            soulCount = souls;
            locations = locs;
        }

    }
    public static File SOULS_FILE = new File(Config.configDirectory, "gainedsouls.json");
    public static Gson gson = ConfigHandler.GSON;
    public static Map<String, List<String>> templateMap = new HashMap<>();

    public static synchronized FairySoulData getSoulData() {
        if (cachedSoulData != null) return cachedSoulData;

        try {
            if (!Files.exists(SOULS_FILE.toPath())) {
                FairySoulData data = createEmptySoulData();
                saveSoulData(data);
                return data;
            }

            try (java.io.BufferedReader reader = Files.newBufferedReader(
                    SOULS_FILE.toPath(), StandardCharsets.UTF_8)) {
                FairySoulData data = gson.fromJson(reader, FairySoulData.class);
                if (data != null && data.locations != null) {
                    cachedSoulData = data;
                    return data;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Keep a safe in-memory fallback so a damaged file is not reopened every render frame.
        cachedSoulData = createEmptySoulData();
        return cachedSoulData;
    }

    private static FairySoulData createEmptySoulData() {
        Map<String, List<String>> template = new HashMap<>();
        template.put("hub", new ArrayList<>());
        template.put("spider", new ArrayList<>());
        template.put("crimson", new ArrayList<>());
        template.put("end", new ArrayList<>());
        template.put("park", new ArrayList<>());
        template.put("farming", new ArrayList<>());
        template.put("gold", new ArrayList<>());
        template.put("dungeon_hub", new ArrayList<>());
        template.put("winter", new ArrayList<>());
        template.put("dwarven", new ArrayList<>());
        return new FairySoulData("Do Not Manually Change This File, It will lead to errors", 0, template);
    }

    public static synchronized void saveSoulData(FairySoulData soulData) {
        if (soulData == null) return;
        cachedSoulData = soulData;
        Path target = SOULS_FILE.toPath();
        Path temporary = target.resolveSibling(target.getFileName().toString() + ".tmp");
        try {
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            try (java.io.BufferedWriter writer = Files.newBufferedWriter(
                    temporary, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE)) {
                gson.toJson(soulData, writer);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            e.printStackTrace();
        }
    }

    /**
     * Removes the locally tracked Fairy Soul progress. The next call to getSoulData()
     * recreates the normal empty data file using the existing template.
     *
     * @return true when the progress file was removed or did not exist, false on I/O failure
     */
    public static synchronized boolean resetSoulData() {
        try {
            // Remove an abandoned temp file first. If it cannot be removed, keep the
            // original progress file and in-memory cache untouched.
            Files.deleteIfExists(SOULS_FILE.toPath().resolveSibling(SOULS_FILE.getName() + ".tmp"));
            Files.deleteIfExists(SOULS_FILE.toPath());
            cachedSoulData = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static final FairySoulData STUB_SOULS =
            new FairySoulData("Could not load repository", 247, Collections.emptyMap());

    public static FairySoulData getAllSouls() {
        return RepoHandler.getData("fairysouls", FairySoulData.class, STUB_SOULS);
    }

}
