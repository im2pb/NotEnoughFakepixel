package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Set;
import java.util.regex.Pattern;

@RegisterEvents
public final class CombatVisuals implements ClientModInitializer {
    private static final Set<String> WITHER_BLADES = Set.of("HYPERION", "VALKYRIE", "SCYLLA", "ASTRAEA");
    private static final Pattern IMPLOSION_LORE = Pattern.compile("^(?:Item )?Ability: (?:Implosion|Wither Impact)\\b");
    private static final long IMPLOSION_WINDOW_MS = 500;
    private static final double IMPLOSION_RADIUS_SQUARED = 16 * 16;
    private static ImplosionUse recentImplosion;

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> recentImplosion = null);
    }

    public static void recordItemUse(PlayerEntity player, Hand hand) {
        if (!(player instanceof ClientPlayerEntity) || !hideImplosionEffects()) return;

        if (hasImplosion(player.getStackInHand(hand))) {
            recentImplosion = new ImplosionUse(player.getPos(), Util.getMeasuringTimeMs() + IMPLOSION_WINDOW_MS);
        }
    }

    public static boolean hideExplosionParticle(ParticleS2CPacket packet) {
        var type = packet.getParameters().getType();
        if (type != ParticleTypes.EXPLOSION && type != ParticleTypes.EXPLOSION_EMITTER) return false;

        var use = recentImplosion;
        return use != null && Util.getMeasuringTimeMs() < use.expiresAt() && hideImplosionEffects()
                && use.origin().squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ()) <= IMPLOSION_RADIUS_SQUARED;
    }

    public static boolean hideEndermanTeleport(Entity entity) {
        return entity instanceof EndermanEntity && entity.getWorld().isClient() && SkyblockData.isSkyblock()
                && ClientConfig.configuration().qol.qolDisableEnderManTeleport;
    }

    public static boolean hideLightning(World world) {
        // Lightning entities have no packet metadata distinguishing enchantment bolts from weather.
        return world instanceof ClientWorld && SkyblockData.isSkyblock()
                && ClientConfig.configuration().qol.soundTweaks.qolDisableThunderlordBolt && !world.isThundering();
    }

    private static boolean hideImplosionEffects() {
        return SkyblockData.isSkyblock() && ClientConfig.configuration().qol.soundTweaks.qolDisableHyperionExplosions;
    }

    private static boolean hasImplosion(ItemStack stack) {
        var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (!WITHER_BLADES.contains(SkyblockItemData.id(customData))) return false;

        var data = customData.copyNbt();
        if (hasImplosionTag(data.getCompound("ExtraAttributes")) || hasImplosionTag(data)) return true;

        var lore = stack.get(DataComponentTypes.LORE);
        return lore != null && lore.lines().stream().anyMatch(line ->
                IMPLOSION_LORE.matcher(Formatting.strip(line.getString()).strip()).find());
    }

    private static boolean hasImplosionTag(NbtCompound data) {
        if (data.contains("implosion")) return true;

        var scrolls = data.getList("ability_scroll", NbtElement.STRING_TYPE);
        for (int i = 0; i < scrolls.size(); i++) {
            String scroll = scrolls.getString(i);
            if (scroll.equals("IMPLOSION_SCROLL") || scroll.equals("WITHER_IMPACT")) return true;
        }
        return false;
    }

    private record ImplosionUse(Vec3d origin, long expiresAt) {
    }
}
