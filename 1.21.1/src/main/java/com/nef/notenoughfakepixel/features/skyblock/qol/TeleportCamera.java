package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TeleportCamera implements ClientModInitializer {
    private static final Pattern WARP_RANGE = Pattern.compile("\\b(\\d{1,3}) blocks away[.!]?$", Pattern.CASE_INSENSITIVE);
    private static final int DEFAULT_ETHERWARP_RANGE = 57;
    private static final long TELEPORT_ANIMATION_MS = 125;
    private static final long TELEPORT_RESPONSE_MS = 1000;
    private static final int MAX_PENDING_USES = 8;
    private static final double MAX_TELEPORT_DISTANCE = 80;
    private static final double ZOOM_RESPONSE_MS = 200;

    @RegisterInstance
    public static final TeleportCamera INSTANCE = new TeleportCamera();

    private final ArrayDeque<Long> pendingUses = new ArrayDeque<>();
    private final EtherwarpOverlay etherwarpOverlay = new EtherwarpOverlay();
    private ClientPlayerEntity trackedPlayer;
    private int lastUseAge;
    private CameraMotion motion;

    private ItemStack etherwarpStack = ItemStack.EMPTY;
    private WarpItem warpItem = WarpItem.NONE;
    private double zoom = 1;
    private double sensitivity = 1;
    private long lastZoomUpdate;

    private TeleportCamera() {
    }

    @Override
    public void onInitializeClient() {
        etherwarpOverlay.register();
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            clearMotion();
            clearEtherwarp();
        });
    }

    public void recordUse(PlayerEntity player, Hand hand) {
        if (!ClientConfig.configuration().qol.enableSmoothAote || !firstPersonPlayer(player)
                || !SkyblockItemData.isAspect(SkyblockItemData.id(player.getStackInHand(hand)))) return;

        if (trackedPlayer != player) {
            clearMotion();
            trackedPlayer = (ClientPlayerEntity) player;
        } else if (lastUseAge == player.age) {
            // Vanilla can send both a block use and an item use for one click.
            return;
        }

        lastUseAge = player.age;
        long now = Util.getMeasuringTimeMs();
        expireUses(now);
        if (pendingUses.size() == MAX_PENDING_USES) pendingUses.removeFirst();
        pendingUses.addLast(now + TELEPORT_RESPONSE_MS);
    }

    public void positionChanged(PlayerEntity player, Vec3d before) {
        if (trackedPlayer == null) return;
        if (trackedPlayer != player || !firstPersonPlayer(player) || !ClientConfig.configuration().qol.enableSmoothAote) {
            clearMotion();
            return;
        }

        Vec3d displacement = before.subtract(player.getPos());
        double distanceSquared = displacement.lengthSquared();
        if (distanceSquared < 0.25) return;

        long now = Util.getMeasuringTimeMs();
        expireUses(now);
        if (distanceSquared > MAX_TELEPORT_DISTANCE * MAX_TELEPORT_DISTANCE || pendingUses.pollFirst() == null) {
            clearMotion();
            return;
        }

        if (motion != null) displacement = displacement.add(motion.offset(now));
        motion = new CameraMotion(displacement, now);
    }

    public Vec3d cameraPosition(Entity focus, Vec3d position, boolean thirdPerson) {
        if (trackedPlayer == null) return position;
        if (thirdPerson || focus != trackedPlayer || !firstPersonPlayer(focus)
                || !ClientConfig.configuration().qol.enableSmoothAote) {
            clearMotion();
            return position;
        }

        long now = Util.getMeasuringTimeMs();
        expireUses(now);
        if (motion != null && now - motion.startedAt() >= TELEPORT_ANIMATION_MS) motion = null;
        if (motion == null) return position;

        Vec3d offset = motion.offset(now);
        Vec3d destination = position.add(offset);
        // Clip the visual offset from the player's real eye position so it cannot move through a wall.
        var hit = focus.getWorld().raycast(new RaycastContext(position, destination, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, focus));
        if (hit.getType() == HitResult.Type.MISS) return destination;

        double clearDistance = Math.max(0, position.distanceTo(hit.getPos()) - 0.15);
        return position.add(offset.normalize().multiply(clearDistance));
    }

    public double zoomFov(Camera camera, float tickDelta, double fov) {
        var settings = ClientConfig.configuration().qol.etherwarpSettings;
        boolean showTarget = settings.qolEtherwarpOverlay || settings.qolEtherwarpText;
        if ((!settings.qolEtherwarpZoom && !showTarget) || MinecraftClient.getInstance().currentScreen != null
                || camera.isThirdPerson() || !firstPersonPlayer(camera.getFocusedEntity())) {
            clearEtherwarp();
            return fov;
        }

        var player = (ClientPlayerEntity) camera.getFocusedEntity();
        ItemStack held = player.getMainHandStack();
        if (etherwarpStack != held) {
            etherwarpStack = held;
            warpItem = readWarpItem(held);
        }

        double targetZoom = 1;
        double targetSensitivity = 1;
        if (warpItem.range() == 0 || (warpItem.requiresSneak() && !player.isSneaking())) {
            etherwarpOverlay.clear();
        } else {
            var hit = player.raycast(warpItem.range(), tickDelta, false);
            if (showTarget) etherwarpOverlay.update(player.getWorld(), hit);
            else etherwarpOverlay.clear();

            if (settings.qolEtherwarpZoom && hit.getType() == HitResult.Type.BLOCK) {
                double distance = hit.getPos().distanceTo(player.getCameraPosVec(tickDelta));
                double scale = MathHelper.clamp(1 - distance / 60, 0, 1);
                targetZoom = 0.25 + 0.75 * scale * scale * scale;
                targetSensitivity = Math.min(1, 0.25 + 0.76 * scale);
            }
        }

        if (!settings.qolEtherwarpZoom) {
            resetZoom();
            return fov;
        }

        long now = Util.getMeasuringTimeMs();
        double blend = lastZoomUpdate == 0 ? 0 : -Math.expm1(-(now - lastZoomUpdate) / ZOOM_RESPONSE_MS);
        lastZoomUpdate = now;
        zoom = approach(zoom, targetZoom, blend);
        sensitivity = approach(sensitivity, targetSensitivity, blend);
        return fov * zoom;
    }

    public double mouseMultiplier() {
        return sensitivity != 1 && zoomAllowed() && firstPersonPlayer(MinecraftClient.getInstance().player)
                ? sensitivity : 1;
    }

    private static double approach(double value, double target, double blend) {
        return Math.abs(value - target) < 0.001 ? target : MathHelper.lerp(blend, value, target);
    }

    private static boolean firstPersonPlayer(Entity entity) {
        var client = MinecraftClient.getInstance();
        return entity != null && entity == client.player && entity == client.getCameraEntity()
                && client.world != null && client.options.getPerspective().isFirstPerson()
                && !entity.isSpectator() && SkyblockData.isSkyblock();
    }

    private static boolean zoomAllowed() {
        return MinecraftClient.getInstance().currentScreen == null
                && ClientConfig.configuration().qol.etherwarpSettings.qolEtherwarpZoom;
    }

    private static WarpItem readWarpItem(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        String id = SkyblockItemData.id(data);
        boolean aspect = SkyblockItemData.isAspect(id);
        if (!aspect && !id.equals("ETHERWARP_CONDUIT")) return WarpItem.NONE;

        var root = data.copyNbt();
        boolean upgraded = hasEtherwarp(root.getCompound("ExtraAttributes")) || hasEtherwarp(root);
        int range = 0;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (var line : lore.lines()) {
                String text = Formatting.strip(line.getString()).strip();
                var matcher = WARP_RANGE.matcher(text);
                if (matcher.find()) range = Math.max(range, Integer.parseInt(matcher.group(1)));
                if (text.toLowerCase(Locale.ROOT).contains("ability: ether transmission")) upgraded = true;
            }
        }

        if (aspect && !upgraded) return WarpItem.NONE;
        return new WarpItem(MathHelper.clamp(range >= 16 ? range : DEFAULT_ETHERWARP_RANGE, 1, 128), aspect);
    }

    private static boolean hasEtherwarp(NbtCompound data) {
        return data.getBoolean("conduit") || data.getBoolean("ethermerge");
    }

    private void expireUses(long now) {
        while (!pendingUses.isEmpty() && pendingUses.peekFirst() <= now) pendingUses.removeFirst();
    }

    private void clearMotion() {
        pendingUses.clear();
        trackedPlayer = null;
        motion = null;
    }

    private void clearEtherwarp() {
        etherwarpStack = ItemStack.EMPTY;
        warpItem = WarpItem.NONE;
        etherwarpOverlay.clear();
        resetZoom();
    }

    private void resetZoom() {
        zoom = sensitivity = 1;
        lastZoomUpdate = 0;
    }

    private record CameraMotion(Vec3d displacement, long startedAt) {
        Vec3d offset(long now) {
            return displacement.multiply(MathHelper.clamp(1 - (now - startedAt) / (double) TELEPORT_ANIMATION_MS, 0, 1));
        }
    }

    private record WarpItem(int range, boolean requiresSneak) {
        private static final WarpItem NONE = new WarpItem(0, false);
    }
}
