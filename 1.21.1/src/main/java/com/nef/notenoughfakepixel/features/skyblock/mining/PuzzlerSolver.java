package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.WorldBoxRenderer;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

@RegisterEvents
public final class PuzzlerSolver implements ClientModInitializer {
    private static final String NPC_PREFIX = "[NPC] Puzzler:";
    private static final BlockPos ORIGIN = new BlockPos(181, 195, 135);
    private static final int HIGHLIGHT_COLOR = 0x6400FF00;

    private final WorldBoxRenderer renderer = new WorldBoxRenderer();
    private List<WorldBoxRenderer.Marker> markers = List.of();

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> markers = List.of());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> markers = List.of()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!markers.isEmpty() && !enabled()) markers = List.of();
        });
        WorldRenderEvents.LAST.register(context -> {
            if (!markers.isEmpty() && SkyblockData.currentLocation() == Location.DWARVEN && enabled()) {
                renderer.draw(context, markers, true, false);
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private void onGameMessage(Text message, boolean overlay) {
        if (overlay || !enabled()) return;
        BlockPos target = solve(message.getString());
        if (target != null) {
            markers = List.of(new WorldBoxRenderer.Marker(new Box(target).expand(0.01), HIGHLIGHT_COLOR));
        }
    }

    // Direction mapping from the original Moulberry solver, relative to the NPC.
    static BlockPos solve(String message) {
        String line = Formatting.strip(message).strip();
        if (!line.startsWith(NPC_PREFIX)) return null;

        int x = 0;
        int z = 0;
        boolean hasDirection = false;
        for (int i = NPC_PREFIX.length(); i < line.length(); i++) {
            char symbol = line.charAt(i);
            if (Character.isWhitespace(symbol) || Character.isSpaceChar(symbol)) continue;
            switch (symbol) {
                case '\u25C0' -> x++; // Left
                case '\u25B2' -> z++; // Up
                case '\u25BC' -> z--; // Down
                case '\u25B6' -> x--; // Right
                default -> {
                    return null;
                }
            }
            hasDirection = true;
        }
        return hasDirection ? ORIGIN.add(x, 0, z) : null;
    }

    private static boolean enabled() {
        return ClientConfig.configuration().mining.dwarvenMines.miningPuzzlerSolver;
    }
}
