package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.nef.notenoughfakepixel.variables.Location;
import net.minecraft.util.math.BlockPos;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class FairySoulCatalog {
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

    private FairySoulCatalog() {
    }

    static String islandKey(Location location) {
        return ISLAND_KEYS.get(location);
    }

    public static Map<Location, List<BlockPos>> parse(String json) {
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
