/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nef.notenoughfakepixel.config.gui.Config;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/** Durable, profile-scoped storage for the aggregate collection and each observed Pets page. */
final class PetTrackerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STORE_TYPE = new TypeToken<Map<String, ProfileData>>() { }.getType();
    private static final int MIN_RARITY_POINTS = 1;
    private static final int MAX_RARITY_POINTS = 6;

    private static final Map<String, ProfileData> PROFILES = new LinkedHashMap<>();
    private static boolean loaded;
    private static boolean dirty;

    private PetTrackerStore() { }

    /** Serialized payload for one player UUID + SkyBlock profile. */
    public static final class ProfileData {
        public Map<String, Integer> ownedPets = new LinkedHashMap<>();
        public Map<String, Map<String, Integer>> petsByPage = new LinkedHashMap<>();

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ProfileData)) return false;
            ProfileData that = (ProfileData) other;
            return ownedPets.equals(that.ownedPets) && petsByPage.equals(that.petsByPage);
        }

        @Override
        public int hashCode() {
            return 31 * ownedPets.hashCode() + petsByPage.hashCode();
        }
    }

    static synchronized ProfileData loadProfileData(String profileKey) {
        ensureLoaded();
        ProfileData saved = PROFILES.get(profileKey);
        return saved == null ? new ProfileData() : copyAndSanitize(saved);
    }

    /** Saves only when the aggregate or an individual page snapshot actually changes. */
    static synchronized boolean saveProfileData(String profileKey, Map<String, Integer> ownedPets,
                                                Map<String, Map<String, Integer>> petsByPage) {
        if (profileKey == null || profileKey.isEmpty() || ownedPets == null || petsByPage == null) return false;
        ensureLoaded();

        ProfileData sanitized = new ProfileData();
        sanitized.ownedPets = sanitizePets(ownedPets);
        for (Entry<String, Map<String, Integer>> page : petsByPage.entrySet()) {
            if (page.getKey() == null || page.getKey().trim().isEmpty() || page.getValue() == null) continue;
            sanitized.petsByPage.put(page.getKey(), sanitizePets(page.getValue()));
        }

        ProfileData previous = PROFILES.get(profileKey);
        if (!sanitized.equals(previous)) {
            PROFILES.put(profileKey, sanitized);
            dirty = true;
        }
        return !dirty || writeStore();
    }

    private static ProfileData copyAndSanitize(ProfileData source) {
        ProfileData copy = new ProfileData();
        if (source == null) return copy;
        if (source.ownedPets != null) copy.ownedPets = sanitizePets(source.ownedPets);
        if (source.petsByPage != null) {
            for (Entry<String, Map<String, Integer>> page : source.petsByPage.entrySet()) {
                if (page.getKey() == null || page.getKey().trim().isEmpty() || page.getValue() == null) continue;
                copy.petsByPage.put(page.getKey(), sanitizePets(page.getValue()));
            }
        }
        return copy;
    }

    private static Map<String, Integer> sanitizePets(Map<String, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (source == null) return result;
        for (Entry<String, Integer> entry : source.entrySet()) {
            String name = entry.getKey();
            Integer points = entry.getValue();
            if (name == null || name.trim().isEmpty() || points == null
                    || points < MIN_RARITY_POINTS || points > MAX_RARITY_POINTS) continue;
            result.put(name, points);
        }
        return result;
    }

    private static void ensureLoaded() {
        if (loaded) return;
        File file = getFile();
        if (file == null) return;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        // Mark the in-memory store loaded even if the directory is temporarily unwritable.
        // Otherwise a later retry could overwrite unsaved observations with an empty disk read.
        loaded = true;

        File temp = new File(file.getPath() + ".tmp");
        if (temp.isFile() && (!file.isFile() || temp.lastModified() >= file.lastModified())) {
            Map<String, ProfileData> recovered = readStore(temp);
            if (recovered != null) {
                replaceStore(recovered);
                try {
                    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) { }
                return;
            }
            temp.delete();
        }

        if (!file.isFile()) return;
        Map<String, ProfileData> loadedProfiles = readStore(file);
        if (loadedProfiles != null) {
            replaceStore(loadedProfiles);
            return;
        }

        // Preserve a damaged file for manual recovery instead of silently replacing it.
        File backup = new File(file.getPath() + ".corrupted-" + System.currentTimeMillis());
        try {
            Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) { }
    }

    private static Map<String, ProfileData> readStore(File file) {
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            Map<String, ProfileData> parsed = GSON.fromJson(reader, STORE_TYPE);
            if (parsed == null) return null;
            Map<String, ProfileData> clean = new LinkedHashMap<>();
            for (Entry<String, ProfileData> entry : parsed.entrySet()) {
                if (entry.getKey() == null || entry.getKey().trim().isEmpty() || entry.getValue() == null) continue;
                clean.put(entry.getKey(), copyAndSanitize(entry.getValue()));
            }
            return clean;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void replaceStore(Map<String, ProfileData> source) {
        PROFILES.clear();
        for (Entry<String, ProfileData> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            PROFILES.put(entry.getKey(), copyAndSanitize(entry.getValue()));
        }
    }

    static synchronized void flushIfDirty() {
        ensureLoaded();
        if (dirty) writeStore();
    }

    private static boolean writeStore() {
        File file = getFile();
        if (file == null) return false;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false;

        File temp = new File(file.getPath() + ".tmp");
        try {
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(temp.toPath()), StandardCharsets.UTF_8)) {
                GSON.toJson(PROFILES, STORE_TYPE, writer);
                writer.flush();
            }

            // Verify the complete temporary document before replacing the last good save.
            Map<String, ProfileData> verified = readStore(temp);
            if (verified == null || !verified.equals(PROFILES)) {
                temp.delete();
                return false;
            }

            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicFailure) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
            return true;
        } catch (Exception ignored) {
            temp.delete();
            return false;
        }
    }

    private static File getFile() {
        File directory = Config.configDirectory;
        if (directory == null) return null;
        return new File(directory, "pet-tracker.json");
    }
}
