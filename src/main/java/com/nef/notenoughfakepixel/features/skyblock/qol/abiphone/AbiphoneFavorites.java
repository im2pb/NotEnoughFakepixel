/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol.abiphone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nef.notenoughfakepixel.config.gui.Config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AbiphoneFavorites {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_FAVORITES = 200;

    private static final class State {
        boolean favoritesOnly;
        List<String> favorites = new ArrayList<>();
    }

    private static final Set<String> FAVORITES = new LinkedHashSet<>();
    private static boolean favoritesOnly;
    private static boolean loaded;

    private AbiphoneFavorites() {
    }

    static boolean isFavorite(String contact) {
        load();
        return FAVORITES.contains(AbiphoneHeads.normalize(contact));
    }

    static boolean toggleFavorite(String contact) {
        load();
        String key = AbiphoneHeads.normalize(contact);
        if (key.isEmpty()) return false;
        boolean now;
        if (FAVORITES.remove(key)) {
            now = false;
        } else {
            if (FAVORITES.size() >= MAX_FAVORITES) return false;
            FAVORITES.add(key);
            now = true;
        }
        save();
        return now;
    }

    static boolean isFavoritesOnly() {
        load();
        return favoritesOnly;
    }

    static boolean toggleFavoritesOnly() {
        load();
        favoritesOnly = !favoritesOnly;
        save();
        return favoritesOnly;
    }

    private static File file() {
        return new File(Config.configDirectory, "abiphone-favorites.json");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        File file = file();
        if (!file.isFile()) return;
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            State state = GSON.fromJson(text, State.class);
            if (state == null) return;
            favoritesOnly = state.favoritesOnly;
            if (state.favorites != null) {
                for (String name : state.favorites) {
                    String key = AbiphoneHeads.normalize(name);
                    if (!key.isEmpty() && FAVORITES.size() < MAX_FAVORITES) FAVORITES.add(key);
                }
            }
        } catch (Exception ignored) {
            FAVORITES.clear();
            favoritesOnly = false;
        }
    }

    private static void save() {
        State state = new State();
        state.favoritesOnly = favoritesOnly;
        state.favorites = new ArrayList<>(FAVORITES);

        File file = file();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return;
        File temp = new File(file.getPath() + ".tmp");
        try {
            Files.write(temp.toPath(), GSON.toJson(state).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicFailed) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            temp.delete();
        }
    }
}
