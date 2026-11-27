package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.IslandWaypoints.Waypoint;
import com.nef.notenoughfakepixel.nefrepo.NefRepo;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.Logger;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class FairySouls implements ClientModInitializer {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("notenoughfakepixel.fairy_souls");
    private static final String FOUND = "SOUL! You found a Fairy Soul!";
    private static final String ALREADY_FOUND = "You already found that Fairy Soul!";
    @RegisterInstance
    private static final FairySouls INSTANCE = new FairySouls(
            FabricLoader.getInstance().getConfigDir().resolve("Notenoughfakepixel"));

    private final FairySoulProgress progress;
    private final WaypointRenderer renderer = new WaypointRenderer();
    private final ConfigColor color = new ConfigColor();
    private List<BlockPos> lastCoordinates = List.of();
    private Set<BlockPos> lastFound = Set.of();
    private List<Waypoint> markers = List.of();

    private FairySouls(Path directory) {
        progress = new FairySoulProgress(directory.resolve("gainedsouls.json"));
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                progress.load();
            } catch (IOException exception) {
                LOGGER.warn("Could not read Fairy Soul progress.", exception);
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        WorldRenderEvents.LAST.register(this::render);
    }

    private void onGameMessage(Text message, boolean overlay) {
        if (overlay || !SkyblockData.isSkyblock() || !isFindMessage(message)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        Location island = SkyblockData.currentLocation();
        BlockPos nearest = nearestSoul(coordinates(island), client.player.getBlockPos());
        if (nearest == null) return;

        try {
            progress.record(island, nearest);
        } catch (IOException exception) {
            LOGGER.warn("Could not save the Fairy Soul at {} on {}.", nearest, island, exception);
            Logger.feedbackError("Could not save Fairy Soul progress. See the client log.");
        }
    }

    static boolean isFindMessage(Text message) {
        return message.getString().lines().map(line -> Formatting.strip(line).strip())
                .anyMatch(line -> FOUND.equalsIgnoreCase(line) || ALREADY_FOUND.equalsIgnoreCase(line));
    }

    static BlockPos nearestSoul(List<BlockPos> coordinates, BlockPos player) {
        double closestDistance = 25;
        BlockPos nearest = null;
        boolean tied = false;
        for (BlockPos position : coordinates) {
            double distance = position.getSquaredDistance(player);
            if (distance < closestDistance) {
                closestDistance = distance;
                nearest = position;
                tied = false;
            } else if (distance == closestDistance && nearest != null && !nearest.equals(position)) {
                tied = true;
            }
        }
        return tied ? null : nearest;
    }

    private void render(WorldRenderContext context) {
        var settings = WaypointRenderer.visibleSettings(context);
        if (settings == null) return;
        var fairySouls = settings.fairySoulWaypointsSettings;
        if (!fairySouls.fairySoulWaypoints) return;

        List<Waypoint> visible = markers(SkyblockData.currentLocation());
        if (!visible.isEmpty()) {
            renderer.renderBlockMarkers(context, visible, color.resolve(fairySouls.fairySoulWaypointsColor), true);
        }
    }

    private List<Waypoint> markers(Location island) {
        List<BlockPos> coordinates = coordinates(island);
        Set<BlockPos> found = progress.found(island);
        if (coordinates != lastCoordinates || found != lastFound) {
            lastCoordinates = coordinates;
            lastFound = found;
            markers = coordinates.stream()
                    .filter(position -> !found.contains(position))
                    .map(position -> Waypoint.atBlock("Fairy Soul", position.getX(), position.getY(), position.getZ()))
                    .toList();
        }
        return markers;
    }

    private static List<BlockPos> coordinates(Location island) {
        var catalog = NefRepo.fairySouls().snapshot().data();
        return catalog == null ? List.of() : catalog.getOrDefault(island, List.of());
    }

    public static void confirmReset() {
        MinecraftClient client = MinecraftClient.getInstance();
        var parent = client.currentScreen;
        client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) INSTANCE.reset();
            client.setScreen(parent);
        }, Text.literal("Reset Fairy Soul Progress?"),
                Text.literal("This clears the local progress shared by all SkyBlock profiles."),
                Text.literal("Reset"), Text.translatable("gui.cancel")));
    }

    private void reset() {
        try {
            progress.reset();
            Logger.feedback("Fairy Soul progress reset.");
        } catch (IOException exception) {
            LOGGER.warn("Could not reset Fairy Soul progress.", exception);
            Logger.feedbackError("Could not reset Fairy Soul progress. See the client log.");
        }
    }
}
