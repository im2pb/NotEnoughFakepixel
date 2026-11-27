package com.nef.notenoughfakepixel.features.skyblock.fishing;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Fishing;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.mixin.FishingBobberAccessor;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class FishingAlert implements ClientModInitializer {
    private static final double MAX_WAKE_DISTANCE = 12;
    private static final float WAKE_Y_OFFSET = 0.01F;
    private static final float WAKE_OFFSET_TOLERANCE = 0.001F;

    @RegisterInstance
    public static final FishingAlert INSTANCE = new FishingAlert();

    private final FishingWakeTracker wakes = new FishingWakeTracker();
    private final FishingBiteState bite = new FishingBiteState();
    private FishingBobberEntity bobber;
    private int buildupCooldown;
    private int latencyTicks;

    private FishingAlert() {
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::clear));
        HudRenderCallback.EVENT.register((context, ticks) -> render(context));
    }

    public void observeParticle(MinecraftClient client, ParticleS2CPacket packet) {
        var type = packet.getParameters().getType();
        boolean wake = type == ParticleTypes.FISHING || type == ParticleTypes.SMOKE || type == ParticleTypes.FLAME;
        boolean splash = type == ParticleTypes.BUBBLE || type == ParticleTypes.LAVA;
        if ((!wake && !splash) || client.player == null || client.player.fishHook == null || !SkyblockData.isSkyblock()) return;

        var settings = ClientConfig.configuration().fishing.alert;
        long now = Util.getMeasuringTimeMs();
        if (!updateBobber(client, settings, now) || bite.muted() || bite.biting(now)) return;
        if (!Double.isFinite(packet.getX()) || !Double.isFinite(packet.getY()) || !Double.isFinite(packet.getZ())) return;

        Vec3d position = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
        if (splash) {
            boolean matchesFluid = bite.inLava() ? type == ParticleTypes.LAVA : type == ParticleTypes.BUBBLE;
            if (matchesFluid && bite.waitingForSplash(now) && bobber.squaredDistanceTo(position) <= 1
                    && otherBobbers(client.world, position, 1, 1).stream()
                    .noneMatch(other -> other.squaredDistanceTo(position) <= 1)
                    && bite.confirmSplash(now)) wakes.clear();
            return;
        }

        if (!isWakeSignature(packet) || bobber.squaredDistanceTo(position) > MAX_WAKE_DISTANCE * MAX_WAKE_DISTANCE) return;
        double tolerance = verticalTolerance(client.world, position);
        if (Math.abs(bobber.getY() - position.y) > tolerance) return;
        double axis = Math.atan2(-packet.getOffsetZ(), packet.getOffsetX());
        var candidates = new ArrayList<FishingWakeTracker.Bobber>();
        candidates.add(new FishingWakeTracker.Bobber(bobber.getId(), bobber.getPos()));
        for (var other : otherBobbers(client.world, position, MAX_WAKE_DISTANCE, tolerance)) {
            candidates.add(new FishingWakeTracker.Bobber(other.getId(), other.getPos()));
        }
        if (!wakes.observe(position, axis, tolerance, candidates, bobber.getId(), now)) return;

        var approach = wakes.approach(bobber.getId(), now);
        double splashRange = 0.2 + (type == ParticleTypes.SMOKE ? 0.03 : 0.1) * latencyTicks;
        bite.observeApproach(approach != null && approach.distance() <= splashRange, now);
        double buildupRange = 0.4 + 0.1 * latencyTicks;
        if (approach != null && approach.distance() >= buildupRange && buildupCooldown == 0) {
            playPling(client, buildupPitch((float) approach.distance() - (float) (buildupRange - 0.1)), 0.5F);
            buildupCooldown = 4;
        }
    }

    private void tick(MinecraftClient client) {
        if (bobber == null && (client.player == null || client.player.fishHook == null || !SkyblockData.isSkyblock())) return;
        long now = Util.getMeasuringTimeMs();
        if (!updateBobber(client, ClientConfig.configuration().fishing.alert, now)) return;

        latencyTicks = latencyTicks(client);
        if (buildupCooldown > 0) buildupCooldown--;
        wakes.expire(now);
        if (wakes.approach(bobber.getId(), now) == null) bite.observeApproach(false, now);
        if (bite.consumeSound(now)) playPling(client, 2, 1);
    }

    private boolean updateBobber(MinecraftClient client, Fishing.FishingAlert settings, long now) {
        FishingBobberEntity hook = client.player == null ? null : client.player.fishHook;
        if (!settings.enabled || !SkyblockData.isSkyblock() || client.world == null || hook == null
                || hook.isRemoved() || hook.getWorld() != client.world || hook.getHookedEntity() != null) {
            if (bobber != null) clear();
            return false;
        }
        if (bobber != hook) {
            clear();
            bobber = hook;
        }
        boolean inLava = hook.isInLava() || client.world.getFluidState(hook.getBlockPos()).isIn(FluidTags.LAVA);
        boolean caught = ((FishingBobberAccessor) hook).nef$caughtFish();
        if (bite.update(inLava, settings.slugMode, caught, now)) {
            wakes.clear();
            buildupCooldown = 0;
        }
        return true;
    }

    private void render(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (bobber == null || bobber.isRemoved() || client.world != bobber.getWorld() || client.player == null
                || client.player.fishHook != bobber || !SkyblockData.isSkyblock() || client.options.hudHidden || bite.muted()) return;
        var settings = ClientConfig.configuration().fishing.alert;
        if (!settings.enabled) return;

        long now = Util.getMeasuringTimeMs();
        String label = alertLabel(bite.biting(now), wakes.approach(bobber.getId(), now), now,
                settings.biteEta, settings.incomingMessage);
        if (label == null) return;

        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(context.getScaledWindowWidth() / 2F, context.getScaledWindowHeight() / 2F + 20F, 0);
            matrices.scale(2, 2, 1);
            context.drawCenteredTextWithShadow(client.textRenderer, label, 0, 0, 0xFFFFFFFF);
        } finally {
            matrices.pop();
        }
    }

    static String alertLabel(boolean biting, FishingWakeTracker.Approach approach, long now,
                             boolean showEta, boolean showIncoming) {
        if (biting) return "\u00a7c\u00a7l!!!";
        if (approach == null) return null;

        long remaining = approach.arrival() - now;
        if (showEta && approach.arrival() != 0 && remaining > 100 && remaining < 10_000) {
            long tenths = (remaining + 50) / 100;
            return "\u00a7e\u00a7l" + tenths / 10 + "." + tenths % 10 + "s";
        }
        return showIncoming ? "\u00a7e\u00a7lINCOMING" : null;
    }

    private List<FishingBobberEntity> otherBobbers(ClientWorld world, Vec3d position, double horizontal, double vertical) {
        Box area = Box.of(position, horizontal * 2, vertical * 2, horizontal * 2);
        return world.getEntitiesByClass(FishingBobberEntity.class, area,
                hook -> hook != bobber && !hook.isRemoved() && hook.getHookedEntity() == null);
    }

    private double verticalTolerance(ClientWorld world, Vec3d position) {
        if (bite.inLava()) return 2;
        BlockPos block = BlockPos.ofFloored(position);
        for (int offset = -2; offset < 2; offset++) {
            var fluid = world.getFluidState(block.up(offset));
            if (fluid.isIn(FluidTags.LAVA) || (fluid.isIn(FluidTags.WATER) && !fluid.isStill())) return 2;
        }
        return 0.5;
    }

    static boolean isWakeSignature(ParticleS2CPacket packet) {
        return Math.abs(packet.getOffsetY() - WAKE_Y_OFFSET) <= WAKE_OFFSET_TOLERANCE
                && Float.isFinite(packet.getOffsetX()) && Float.isFinite(packet.getOffsetZ())
                && (packet.getOffsetX() != 0 || packet.getOffsetZ() != 0);
    }

    private static int latencyTicks(MinecraftClient client) {
        var handler = client.getNetworkHandler();
        var entry = handler == null ? null : handler.getPlayerListEntry(client.player.getUuid());
        return entry == null ? 0 : Math.clamp(entry.getLatency() / 50, 0, 6);
    }

    private static float buildupPitch(float distance) {
        float d = Math.clamp(distance, 0.1F, 5);
        return (1 / (d + 1 / 0.9F)) * (1 - d / 5) + 0.1F;
    }

    private static void playPling(MinecraftClient client, float pitch, float volume) {
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), pitch, volume));
    }

    private void clear() {
        bobber = null;
        wakes.clear();
        bite.clear();
        buildupCooldown = 0;
        latencyTicks = 0;
    }
}
