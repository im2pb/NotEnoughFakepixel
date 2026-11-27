package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class FairySoulCatalog {
    private static final Logger LOGGER = LoggerFactory.getLogger("notenoughfakepixel.fairy_souls");
    private static final long MAX_AGE = TimeUnit.HOURS.toMillis(24);
    private static final long RETRY_DELAY = TimeUnit.MINUTES.toMillis(1);
    private static final int MAX_BYTES = 256 * 1024;
    private static final int TIMEOUT = 5_000;
    private static final Map<Location, String> ISLAND_KEYS = Map.of(
            Location.HUB, "hub",
            Location.SPIDERS_DEN, "spider",
            Location.CRIMSON_ISLE, "crimson",
            Location.THE_END, "end",
            Location.PARK, "park",
            Location.BARN, "farming",
            Location.GOLD_MINE, "gold",
            Location.DUNGEON_HUB, "dungeon_hub",
            Location.JERRY, "winter",
            Location.DWARVEN, "dwarven");

    private final Path cacheFile;
    private final URI source;
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Map<Location, List<BlockPos>> locations = Map.of();
    private volatile long nextRefresh;
    private boolean readCache;

    FairySoulCatalog(Path cacheFile, URI source) {
        this.cacheFile = cacheFile;
        this.source = source;
    }

    static String islandKey(Location location) {
        return ISLAND_KEYS.get(location);
    }

    List<BlockPos> coordinates(Location location) {
        return locations.getOrDefault(location, List.of());
    }

    void refreshIfDue() {
        if (System.currentTimeMillis() < nextRefresh || !refreshing.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(this::refresh);
    }

    private void refresh() {
        try {
            if (!readCache) {
                readCache = true;
                long cachedAt = loadCache();
                long now = System.currentTimeMillis();
                if (cachedAt > 0 && now - cachedAt < MAX_AGE) {
                    nextRefresh = Math.min(now, cachedAt) + MAX_AGE;
                    return;
                }
            }

            String json = download();
            locations = parse(json);
            nextRefresh = System.currentTimeMillis() + MAX_AGE;
            try {
                FairySoulFiles.writeAtomically(cacheFile, json);
            } catch (IOException exception) {
                LOGGER.warn("Could not save the Fairy Soul catalog cache at {}", cacheFile, exception);
            }
        } catch (IOException | JsonParseException exception) {
            nextRefresh = System.currentTimeMillis() + RETRY_DELAY;
            LOGGER.warn("Could not refresh the Fairy Soul catalog. Keeping the last valid coordinates.", exception);
        } finally {
            refreshing.set(false);
        }
    }

    private long loadCache() {
        try {
            long cachedAt = Files.getLastModifiedTime(cacheFile).toMillis();
            if (Files.size(cacheFile) > MAX_BYTES) throw new IOException("Catalog cache exceeds size limit");
            locations = parse(Files.readString(cacheFile));
            return cachedAt;
        } catch (NoSuchFileException ignored) {
            return 0;
        } catch (IOException | JsonParseException exception) {
            LOGGER.warn("Could not read the Fairy Soul catalog cache at {}", cacheFile, exception);
            return 0;
        }
    }

    private String download() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) source.toURL().openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + status);

            try (var input = connection.getInputStream()) {
                byte[] bytes = input.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) throw new IOException("Catalog response exceeds size limit");
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    static Map<Location, List<BlockPos>> parse(String json) {
        var document = JsonParser.parseString(json);
        if (!document.isJsonObject()) throw new JsonParseException("Expected a Fairy Soul catalog object");
        var entries = document.getAsJsonObject().get("locations");
        if (entries == null || !entries.isJsonObject()) throw new JsonParseException("Missing Fairy Soul locations");

        Map<Location, List<BlockPos>> result = new EnumMap<>(Location.class);
        var islands = entries.getAsJsonObject();
        for (var island : ISLAND_KEYS.entrySet()) {
            var rows = islands.get(island.getValue());
            if (rows == null) continue;
            if (!rows.isJsonArray()) throw new JsonParseException("Expected coordinates for " + island.getValue());

            var positions = new LinkedHashSet<BlockPos>();
            for (var row : rows.getAsJsonArray()) {
                BlockPos position = FairySoulFiles.parsePosition(row);
                if (position != null) positions.add(position);
            }
            if (!positions.isEmpty()) result.put(island.getKey(), List.copyOf(positions));
        }
        if (result.isEmpty()) throw new JsonParseException("Catalog contains no valid Fairy Soul coordinates");
        return Map.copyOf(result);
    }
}
