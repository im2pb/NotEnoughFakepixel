/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DungeonPBTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();
    private static final Map<String, Long> PBS = new HashMap<>();
    private static boolean loaded;
    private static boolean dirty;

    public static void load() {
        if (loaded) return;
        loaded = true;
        PbsFileHolder.load(PBS);
        dirty = false;
    }

    public static Map<String, Long> snapshot() {
        load();
        return Collections.unmodifiableMap(new HashMap<>(PBS));
    }

    public static long get(String key) {
        load();
        Long value = PBS.get(key);
        return value == null ? -1L : value;
    }

    public static boolean updateIfBetter(String key, long durationMs) {
        if (!Config.feature.dungeonSystems.personalBest || durationMs < 0) return false;
        load();
        Long old = PBS.get(key);
        if (old != null && old <= durationMs) return false;
        PBS.put(key, durationMs);
        dirty = true;
        return true;
    }

    public static boolean remove(String key) {
        load();
        if (PBS.remove(key) == null) return false;
        dirty = true;
        save();
        return true;
    }

    public static boolean isDirty() {
        load();
        return dirty;
    }

    public static void save() {
        load();
        if (!dirty) return;
        PbsFileHolder.save(PBS);
        dirty = false;
    }

    private static String format(long ms) {
        long seconds = ms / 1000L;
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    private static final class PbsFileHolder {
        private static File file() { return new File(Config.configDirectory, "dungeon-pb.json"); }

        static void load(Map<String, Long> target) {
            File file = file();
            if (!file.isFile()) return;
            try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                Map<String, Long> loadedMap = GSON.fromJson(reader, MAP_TYPE);
                if (loadedMap != null) target.putAll(loadedMap);
            } catch (Exception ignored) {
                target.clear();
            }
        }

        static void save(Map<String, Long> source) {
            File file = file();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) return;
            File temp = new File(file.getPath() + ".tmp");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(source, MAP_TYPE));
                writer.flush();
            } catch (IOException ignored) {
                temp.delete();
                return;
            }
            try {
                try {
                    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (Exception ignored) {
                    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ignored) {
                temp.delete();
            }
        }
    }
}
