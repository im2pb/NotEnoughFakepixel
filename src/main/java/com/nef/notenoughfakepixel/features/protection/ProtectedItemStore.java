/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.protection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nef.notenoughfakepixel.config.gui.Config;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ProtectedItemStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();
    private final Set<String> ids = new HashSet<>();
    private File file;
    private boolean loaded;

    public void load() {
        if (loaded) return;
        file = new File(Config.configDirectory, "protected-items.json");
        ids.clear();
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        if (temp.isFile() && (!file.isFile() || temp.lastModified() >= file.lastModified())) {
            try (Reader reader = new InputStreamReader(new FileInputStream(temp), StandardCharsets.UTF_8)) {
                Set<String> recovered = GSON.fromJson(reader, SET_TYPE);
                if (recovered != null) {
                    for (String id : recovered) {
                        try { ids.add(UUID.fromString(id).toString()); } catch (IllegalArgumentException ignored) {}
                    }
                    try {
                        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ignored) {
                    }
                    loaded = true;
                    return;
                }
            } catch (Exception ignored) {
                temp.delete();
            }
        }
        if (!file.isFile()) {
            loaded = true;
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Set<String> loadedIds = GSON.fromJson(reader, SET_TYPE);
            if (loadedIds != null) {
                for (String id : loadedIds) {
                    try { ids.add(UUID.fromString(id).toString()); } catch (IllegalArgumentException ignored) {}
                }
            }
            loaded = true;
        } catch (Exception ignored) {
            File backup = new File(file.getPath() + ".corrupted-" + System.currentTimeMillis());
            try {
                Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignoredMove) {
            }
            loaded = true;
        }
    }

    public boolean isProtected(ItemId id) {
        load();
        return id != null && id.uuid != null && ids.contains(id.uuid.toString());
    }

    public boolean contains(UUID uuid) {
        load();
        return uuid != null && ids.contains(uuid.toString());
    }

    public boolean toggle(ItemId id) {
        if (id == null || id.uuid == null) return false;
        load();
        String value = id.uuid.toString();
        if (!ids.add(value)) ids.remove(value);
        save();
        return ids.contains(value);
    }

    public Set<String> snapshot() {
        load();
        return Collections.unmodifiableSet(new HashSet<>(ids));
    }

    public void clear() {
        load();
        ids.clear();
        save();
    }

    public void save() {
        loadIfNeeded();
        if (file == null) file = new File(Config.configDirectory, "protected-items.json");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return;
        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8)) {
            writer.write(GSON.toJson(ids, SET_TYPE));
            writer.flush();
        } catch (IOException ignored) {
            temp.delete();
            return;
        }
        try {
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicFailure) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            temp.delete();
        }
    }

    private void loadIfNeeded() {
        if (!loaded) load();
    }

    public static final class ItemId {
        public final UUID uuid;
        public ItemId(UUID uuid) { this.uuid = uuid; }
        public static ItemId from(ItemStackAccessor stack) {
            return stack == null ? null : new ItemId(stack.getUuid());
        }
    }

    public interface ItemStackAccessor { UUID getUuid(); }
}
