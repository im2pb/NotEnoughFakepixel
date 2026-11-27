package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.nef.notenoughfakepixel.utils.AtomicFiles;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class FairySoulProgress {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    private Map<String, Set<BlockPos>> found = Map.of();
    private boolean readFailed;

    FairySoulProgress(Path file) {
        this.file = file;
    }

    void load() throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            var document = JsonParser.parseReader(reader);
            var locations = document.isJsonObject() ? document.getAsJsonObject().get("locations") : null;
            if (locations == null || !locations.isJsonObject()) throw new JsonParseException("Missing saved Fairy Soul locations");

            var loaded = new LinkedHashMap<String, Set<BlockPos>>();
            for (var island : locations.getAsJsonObject().entrySet()) {
                if (!island.getValue().isJsonArray()) continue;

                var positions = new LinkedHashSet<BlockPos>();
                for (var row : island.getValue().getAsJsonArray()) {
                    BlockPos position = FairySoulFiles.parsePosition(row);
                    if (position != null) positions.add(position);
                }
                loaded.put(island.getKey(), positions);
            }
            found = loaded;
            readFailed = false;
        } catch (NoSuchFileException ignored) {
            found = Map.of();
            readFailed = false;
        } catch (IOException | JsonParseException exception) {
            readFailed = true;
            throw new IOException("Could not load " + file, exception);
        }
    }

    Set<BlockPos> found(Location island) {
        String key = FairySoulCatalog.islandKey(island);
        return key == null ? Set.of() : found.getOrDefault(key, Set.of());
    }

    boolean record(Location island, BlockPos position) throws IOException {
        String key = FairySoulCatalog.islandKey(island);
        if (key == null || found(island).contains(position)) return false;
        if (readFailed) throw new IOException("Existing Fairy Soul progress could not be read");

        var updated = new LinkedHashMap<>(found);
        var coordinates = new LinkedHashSet<>(found(island));
        coordinates.add(position);
        updated.put(key, coordinates);
        save(updated);
        return true;
    }

    void reset() throws IOException {
        save(Map.of());
        readFailed = false;
    }

    private void save(Map<String, Set<BlockPos>> updated) throws IOException {
        var locations = new LinkedHashMap<String, List<String>>();
        updated.forEach((key, positions) -> locations.put(key, positions.stream()
                .map(pos -> pos.getX() + "," + pos.getY() + "," + pos.getZ())
                .toList()));
        int count = locations.values().stream().mapToInt(List::size).sum();
        var data = new SavedSouls("Locally recorded Fairy Souls", count, locations);
        AtomicFiles.writeString(file, GSON.toJson(data) + "\n");
        found = updated;
    }

    private record SavedSouls(String description, int soulCount, Map<String, List<String>> locations) {
    }
}
