package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.google.gson.JsonElement;
import net.minecraft.util.math.BlockPos;

final class FairySoulFiles {
    private FairySoulFiles() {
    }

    static BlockPos parsePosition(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return null;
        String[] coordinates = value.getAsString().split(",", -1);
        if (coordinates.length != 3) return null;
        try {
            return new BlockPos(Integer.parseInt(coordinates[0].strip()), Integer.parseInt(coordinates[1].strip()),
                    Integer.parseInt(coordinates[2].strip()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
