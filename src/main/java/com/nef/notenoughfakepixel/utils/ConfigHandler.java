/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ConfigHandler {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <T> @Nullable T loadConfig(Class<T> configClass, File file, Gson gson) {
        return loadConfig(configClass, file, gson, false);
    }

    public static <T> @Nullable T loadConfig(Class<T> configClass, File file, Gson gson, boolean useGzip) {
        return loadConfig(configClass, file, gson, useGzip, true);
    }

    public static <T> @Nullable T loadConfig(
            Class<T> configClass,
            File file,
            Gson gson,
            boolean useGzip,
            boolean handleError
    ) {
        if (file == null) return null;

        File tempFile = new File(file.getParent(), file.getName() + ".temp");
        if (tempFile.isFile() && (!file.isFile() || tempFile.lastModified() >= file.lastModified())) {
            try {
                T recovered = loadConfig(configClass, tempFile, gson, useGzip, false);
                if (recovered != null) {
                    try {
                        Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ignored) {
                    }
                    return recovered;
                }
            } catch (Exception ignored) {
            }
        }

        if (!file.exists()) return null;
        try (
                BufferedReader reader = useGzip ?
                        new BufferedReader(new InputStreamReader(
                                new GZIPInputStream(Files.newInputStream(file.toPath())),
                                StandardCharsets.UTF_8
                        )) :
                        new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))
        ) {
            return gson.fromJson(reader, configClass);
        } catch (Exception e) {
            if (!handleError) return null;
            new RuntimeException(
                    "Invalid config file '" + file + "'. This will reset the config to default",
                    e
            ).printStackTrace();
            
            makeBackup(file, ".corrupted");
        }
        return null;
    }

    public static void saveConfig(Object config, File file, Gson gson) {
        saveConfig(config, file, gson, false);
    }

    public static void saveConfig(Object config, File file, Gson gson, boolean useGzip) {
        File tempFile = new File(file.getParent(), file.getName() + ".temp");
        try {
            tempFile.createNewFile();
            try (
                    BufferedWriter writer = useGzip ?
                            new BufferedWriter(new OutputStreamWriter(
                                    new GZIPOutputStream(Files.newOutputStream(tempFile.toPath())),
                                    StandardCharsets.UTF_8
                            )) :
                            new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(tempFile.toPath()), StandardCharsets.UTF_8))
            ) {
                writer.write(gson.toJson(config));
            }

            if (loadConfig(config.getClass(), tempFile, gson, useGzip, false) == null) {
                System.out.println("Config verification failed for " + tempFile + ", could not save config properly.");
                makeBackup(tempFile, ".backup");
                return;
            }

            try {
                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {

                Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            makeBackup(tempFile, ".backup");
            e.printStackTrace();
        }
    }

    private static void makeBackup(File file, String suffix) {
        if (file == null || !file.exists()) return;

        File backupFile = new File(file.getParent(), file.getName() + "-" + System.currentTimeMillis() + suffix);
        try {
            Files.move(file.toPath(), backupFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            return;
        } catch (IOException ignored) {
            try {
                Files.move(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException ignoredAgain) {
                System.out.println("Could not back up config file: " + file);
            }
        }
    }

}
