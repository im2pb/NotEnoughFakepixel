package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.google.gson.JsonElement;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

    static void writeAtomically(Path path, String contents) throws IOException {
        Path directory = path.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "fairy-souls-", ".tmp");
        try {
            Files.writeString(temporary, contents);
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
