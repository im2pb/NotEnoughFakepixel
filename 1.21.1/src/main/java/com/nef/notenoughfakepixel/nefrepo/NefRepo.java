package com.nef.notenoughfakepixel.nefrepo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.FairySoulCatalog;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Catalog;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

import java.net.URI;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;

@RegisterEvents
public final class NefRepo implements ClientModInitializer {
    private static final URI BASE = URI.create(
            "https://raw.githubusercontent.com/davidbelesp/NotEnoughFakepixel-REPO/refs/heads/main/data/");
    private static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve("Notenoughfakepixel");
    private static final Pattern PETS_TRAILING_COMMA = Pattern.compile(",(?=\\s*}\\s*}\\s*$)");
    private static final RepoSource<Catalog> ACCESSORIES = source("accessories", AccessoriesData::parse);
    private static final RepoSource<Map<Location, List<BlockPos>>> FAIRY_SOULS = source("fairysouls", FairySoulCatalog::parse);
    private static final RepoSource<Map<String, String>> PETS = source("pets", NefRepo::parsePets);
    private static final RepoSource<Update> UPDATE = source("update", NefRepo::parseUpdate);
    private static final List<RepoSource<?>> SOURCES = List.of(ACCESSORIES, FAIRY_SOULS, PETS, UPDATE);

    private final ExecutorService executor = Executors.newFixedThreadPool(2, task ->
            Thread.ofPlatform().daemon().name("NEF repository").unstarted(task));

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> refresh());
        ClientTickEvents.END_CLIENT_TICK.register(client -> refresh());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> executor.shutdownNow());
    }

    public static RepoSource<Catalog> accessories() {
        return ACCESSORIES;
    }

    public static RepoSource<Map<Location, List<BlockPos>>> fairySouls() {
        return FAIRY_SOULS;
    }

    public static RepoSource<Map<String, String>> pets() {
        return PETS;
    }

    public static RepoSource<Update> update() {
        return UPDATE;
    }

    private void refresh() {
        for (RepoSource<?> source : SOURCES) source.refreshIfDue(executor);
    }

    private static <T> RepoSource<T> source(String name, Function<String, T> parse) {
        return new RepoSource<>(DIRECTORY.resolve(name + "-cache.json"), BASE.resolve(name + ".json"), parse);
    }

    static Map<String, String> parsePets(String json) {
        var pets = object(PETS_TRAILING_COMMA.matcher(json).replaceFirst("")).get("pets");
        if (pets == null || !pets.isJsonObject() || pets.getAsJsonObject().isEmpty()) {
            throw new JsonParseException("Expected a pet catalog");
        }
        Map<String, String> entries = new LinkedHashMap<>();
        for (var entry : pets.getAsJsonObject().entrySet()) {
            String rarity = string(pets.getAsJsonObject(), entry.getKey()).strip();
            if (entry.getKey().isBlank() || rarity.isEmpty()) throw new JsonParseException("Invalid pet row");
            entries.put(entry.getKey(), rarity);
        }
        return Map.copyOf(entries);
    }

    static Update parseUpdate(String json) {
        var root = object(json);
        String version = string(root, "version");
        var warning = root.get("has_warning");
        if (version.isBlank() || warning == null || !warning.isJsonPrimitive() || !warning.getAsJsonPrimitive().isBoolean()) {
            throw new JsonParseException("Invalid update metadata");
        }
        var fabric = root.get("fabric");
        Release fabricRelease = null;
        if (fabric != null && !fabric.isJsonNull()) {
            if (!fabric.isJsonObject()) throw new JsonParseException("Expected a Fabric release object");
            var entry = fabric.getAsJsonObject();
            String fabricVersion = string(entry, "version");
            if (fabricVersion.isBlank()) throw new JsonParseException("Missing Fabric release version");
            fabricRelease = new Release(fabricVersion, string(entry, "update_msg"), string(entry, "update_link"));
        }
        return new Update(version, string(root, "update_msg"), string(root, "update_link"),
                string(root, "discord_link"), warning.getAsBoolean(), string(root, "warning_msg"), fabricRelease);
    }

    private static JsonObject object(String json) {
        var value = JsonParser.parseString(json);
        if (!value.isJsonObject()) throw new JsonParseException("Expected a repository object");
        return value.getAsJsonObject();
    }

    private static String string(JsonObject object, String key) {
        var value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Expected text for " + key);
        }
        return value.getAsString();
    }

    public record Update(String version, String message, String updateLink, String discordLink,
                         boolean hasWarning, String warningMessage, Release fabric) {
    }

    public record Release(String version, String message, String updateLink) {
    }
}
