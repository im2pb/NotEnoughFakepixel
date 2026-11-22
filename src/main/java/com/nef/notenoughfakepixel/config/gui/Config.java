/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.gui.config.ConfigEditor;
import com.nef.notenoughfakepixel.config.gui.core.GuiScreenElementWrapper;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterKeybind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@RegisterEvents
public class Config {

    public static Configuration feature;
    private static File configFile;
    public static File configDirectory = new File("config/Notenoughfakepixel");

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    public static void init() {
        configFile = new File(configDirectory, "config.json");
        loadConfig();
    }

    private static void loadConfig() {
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(configFile.toPath()), StandardCharsets.UTF_8))) {
                feature = gson.fromJson(reader, Configuration.class);
            } catch (Exception ignored) {
                backupBrokenConfig();
                feature = null;
            }
        }

        if (feature == null) {
            File temp = new File(configFile.getPath() + ".tmp");
            if (temp.isFile()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(temp.toPath()), StandardCharsets.UTF_8))) {
                    Configuration recovered = gson.fromJson(reader, Configuration.class);
                    if (recovered != null) {
                        feature = recovered;
                        try {
                            Files.move(temp.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ignored) {
                        }
                    }
                } catch (Exception ignored) {
                    temp.delete();
                }
            }
        }

        if (feature == null) {
            feature = new Configuration();
        }
        ensureSections();
        migrateLegacyOptions();
        saveConfig();
    }

    private static void migrateLegacyOptions() {
        if (configFile == null || !configFile.isFile()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(configFile.toPath()), StandardCharsets.UTF_8))) {
            JsonElement root = new JsonParser().parse(reader);
            if (root == null || !root.isJsonObject()) return;
            JsonElement performance = root.getAsJsonObject().get("performance");
            if (performance == null || !performance.isJsonObject()) return;
            JsonObject perf = performance.getAsJsonObject();
            JsonElement legacyWeather = perf.get("disableWeatherRendering");
            if (legacyWeather != null && legacyWeather.isJsonPrimitive() && legacyWeather.getAsBoolean()) {
                feature.qol.qolDisableRain = true;
            }
        } catch (Exception ignored) {
            
        }
    }

    public static void saveConfig() {
        if (feature == null || configFile == null) return;
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return;

        File temp = new File(configFile.getPath() + ".tmp");
        try {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(temp.toPath()), StandardCharsets.UTF_8))) {
                writer.write(gson.toJson(feature));
                writer.flush();
            }

            try (BufferedReader verify = new BufferedReader(new InputStreamReader(Files.newInputStream(temp.toPath()), StandardCharsets.UTF_8))) {
                if (gson.fromJson(verify, Configuration.class) == null) {
                    temp.delete();
                    return;
                }
            }

            try {
                Files.move(temp.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                Files.move(temp.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            temp.delete();
        }
    }

    private static void backupBrokenConfig() {
        if (configFile == null || !configFile.isFile()) return;
        File backup = new File(configFile.getPath() + ".corrupted-" + System.currentTimeMillis());
        try {
            Files.move(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private static void ensureSections() {
        if (feature.qol == null) feature.qol = new com.nef.notenoughfakepixel.config.features.QualityOfLife();
        if (feature.dungeons == null) feature.dungeons = new com.nef.notenoughfakepixel.config.features.Dungeons();
        if (feature.diana == null) feature.diana = new com.nef.notenoughfakepixel.config.features.DianaF();
        if (feature.slayer == null) feature.slayer = new com.nef.notenoughfakepixel.config.features.Slayer();
        if (feature.experimentation == null) feature.experimentation = new com.nef.notenoughfakepixel.config.features.Experimentation();
        if (feature.chocolateFactory == null) feature.chocolateFactory = new com.nef.notenoughfakepixel.config.features.ChocolateFactory();
        if (feature.crimson == null) feature.crimson = new com.nef.notenoughfakepixel.config.features.Crimson();
        if (feature.mining == null) feature.mining = new com.nef.notenoughfakepixel.config.features.Mining();
        if (feature.fishing == null) feature.fishing = new com.nef.notenoughfakepixel.config.features.Fishing();
        if (feature.sl == null) feature.sl = new com.nef.notenoughfakepixel.config.features.SlotLocking();
        if (feature.duels == null) feature.duels = new com.nef.notenoughfakepixel.features.duels.Duels();
        if (feature.misc == null) feature.misc = new com.nef.notenoughfakepixel.config.features.Misc();
        if (feature.waypoints == null) feature.waypoints = new com.nef.notenoughfakepixel.config.features.Waypoints();
        if (feature.overlays == null) feature.overlays = new com.nef.notenoughfakepixel.config.features.Overlays();
        if (feature.accessories == null) feature.accessories = new com.nef.notenoughfakepixel.config.features.Accessories();
        if (feature.searchBar == null) feature.searchBar = new com.nef.notenoughfakepixel.config.features.SearchBarConfig();
        if (feature.romanNumerals == null) feature.romanNumerals = new com.nef.notenoughfakepixel.config.features.RomanNumeralConfig();
        if (feature.dungeonSystems == null) feature.dungeonSystems = new com.nef.notenoughfakepixel.config.features.DungeonSystemsConfig();
        if (feature.protection == null) feature.protection = new com.nef.notenoughfakepixel.config.features.ProtectionConfig();
        if (feature.debug == null) feature.debug = new com.nef.notenoughfakepixel.config.features.Debug();
        if (feature.pets == null) feature.pets = new com.nef.notenoughfakepixel.config.features.Pets();
        if (feature.performance == null) feature.performance = new com.nef.notenoughfakepixel.config.features.PerformanceConfig();
    }

    @RegisterKeybind
    public static final KeyBinding openGuiKey = new KeyBinding(
            "Open GUI",
            Keyboard.KEY_P,
            "NotEnoughFakepixel"
    );

    public static GuiScreen screenToOpen = null;
    private static int screenTicks = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;

        if (screenToOpen != null) {
            screenTicks++;
            if (screenTicks == 5) {
                Minecraft.getMinecraft().displayGuiScreen(screenToOpen);
                screenTicks = 0;
                screenToOpen = null;
            }
        }

        if (openGuiKey.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
            screenToOpen = new GuiScreenElementWrapper(new ConfigEditor(feature));
        }
    }

}
