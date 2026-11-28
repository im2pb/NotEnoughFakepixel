package com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints;

import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.IslandWaypoints.Waypoint;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.SkullTextures;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class GiftWaypoints implements ClientModInitializer {
    private static final String GIFT_TEXTURE =
            "textures.minecraft.net/texture/10f5398510b1a05afc5b201ead8bfc583e57d7202f5193b0b761fcbd0ae2";
    private static final String FOUND_GIFT = "GIFT! You found a White Gift!";
    private static final String ALREADY_FOUND = "You have already found this Gift this year!";
    private static final String FOUND_ALL = "GIFT! You found all of the Gifts! Talk to St. Jerry to receive a reward!";
    private static final int SCAN_INTERVAL = 20;
    private static final long CONFIRMATION_TIMEOUT = TimeUnit.SECONDS.toNanos(5);
    @RegisterInstance
    private static final GiftWaypoints INSTANCE = new GiftWaypoints();

    private final WaypointRenderer renderer = new WaypointRenderer();
    private final ConfigColor color = new ConfigColor();
    private final Map<BlockPos, Waypoint> markers = new LinkedHashMap<>();
    private final Set<BlockPos> found = new HashSet<>();
    private final Map<String, Boolean> checkedTextures = new HashMap<>();
    private ClientWorld visitWorld;
    private GiftInteraction pending;
    private int ticksUntilScan;
    private boolean complete;

    private GiftWaypoints() {
    }

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> clearVisit());
        ClientTickEvents.END_CLIENT_TICK.register(this::scan);
        ClientReceiveMessageEvents.GAME.register(this::onGameMessage);
        WorldRenderEvents.LAST.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private void scan(MinecraftClient client) {
        if (!updateVisit(client.world) || complete || ticksUntilScan-- > 0) return;
        ticksUntilScan = SCAN_INTERVAL - 1;
        for (Entity entity : visitWorld.getEntities()) {
            if (!(entity instanceof ArmorStandEntity stand) || !stand.isAlive()) continue;

            BlockPos position = stand.getBlockPos();
            if (found.contains(position) || markers.containsKey(position)
                    || !isGiftHead(stand.getEquippedStack(EquipmentSlot.HEAD))) continue;
            markers.put(position, Waypoint.atBlock("Gift", position.getX(), position.getY() + 1, position.getZ()));
        }
    }

    public static void recordInteraction(Entity entity) {
        var feature = INSTANCE;
        var client = MinecraftClient.getInstance();
        if (!(entity instanceof ArmorStandEntity stand) || entity.getWorld() != client.world
                || !feature.updateVisit(client.world) || feature.complete) return;

        if (feature.isGiftHead(stand.getEquippedStack(EquipmentSlot.HEAD))) {
            feature.pending = new GiftInteraction(stand.getBlockPos(), System.nanoTime());
        }
    }

    private void onGameMessage(Text message, boolean overlay) {
        if (overlay || !updateVisit(MinecraftClient.getInstance().world) || complete) return;

        for (String rawLine : message.getString().lines().toList()) {
            String line = Formatting.strip(rawLine).strip();
            if (FOUND_ALL.equals(line)) {
                complete = true;
                markers.clear();
                pending = null;
                return;
            }
            if (pending != null && (ALREADY_FOUND.equals(line) || line.startsWith(FOUND_GIFT))) {
                if (System.nanoTime() - pending.time() <= CONFIRMATION_TIMEOUT) {
                    found.add(pending.position());
                    markers.remove(pending.position());
                }
                pending = null;
            }
        }
    }

    private boolean updateVisit(ClientWorld world) {
        ClientWorld activeWorld = SkyblockData.isSkyblock() && SkyblockData.currentLocation() == Location.JERRY ? world : null;
        if (activeWorld != visitWorld) {
            clearVisit();
            visitWorld = activeWorld;
        }
        return activeWorld != null;
    }

    private void clearVisit() {
        visitWorld = null;
        pending = null;
        markers.clear();
        found.clear();
        checkedTextures.clear();
        ticksUntilScan = 0;
        complete = false;
    }

    private void render(WorldRenderContext context) {
        if (visitWorld != context.world() || SkyblockData.currentLocation() != Location.JERRY || markers.isEmpty()) return;

        var settings = WaypointRenderer.visibleSettings(context);
        if (settings == null || !settings.giftWaypointsSettings.giftWaypoints) return;
        renderer.renderBlockMarkers(context, markers.values(), color.resolve(settings.giftWaypointsSettings.giftWaypointsColor), false);
    }

    private boolean isGiftHead(ItemStack head) {
        if (!head.isOf(Items.PLAYER_HEAD)) return false;
        var profile = head.get(DataComponentTypes.PROFILE);
        if (profile == null) return false;

        for (var property : profile.properties().get("textures")) {
            if (checkedTextures.computeIfAbsent(property.value(), GiftWaypoints::isGiftTexture)) return true;
        }
        return false;
    }

    private static boolean isGiftTexture(String encoded) {
        return SkullTextures.matches(SkullTextures.skinUrl(encoded), GIFT_TEXTURE);
    }

    private record GiftInteraction(BlockPos position, long time) {
    }
}
