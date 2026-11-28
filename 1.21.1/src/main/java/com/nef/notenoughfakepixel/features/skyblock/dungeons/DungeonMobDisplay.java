package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Dungeons;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.EntityGlow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@RegisterEvents
public final class DungeonMobDisplay implements ClientModInitializer {
    private static final Pattern ZERO_HEALTH = Pattern.compile("(?:^|\\s)0(?:\\s*/\\s*[^\\s❤♥]+)?\\s*[❤♥]$");
    private static final Pattern FEL_NAME = Pattern.compile("\\bFels?\\b");
    private static final Set<String> WITHER_NAMES = Set.of("Maxor", "Storm", "Goldor", "Necron");
    private static final Set<String> FEL_HEAD_TEXTURES = Set.of(
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv"
                    + "N2E1OWJiMGE3YTMyOTY1YjNkOTBkOGVhZmE4OTlkMTgzNWY0MjQ1MDllYWRkNGU2YjcwOWFkYTUwYjljZiJ9fX0=",
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdh"
                    + "NTliYjBhN2EzMjk2NWIzZDkwZDhlYWZhODk5ZDE4MzVmNDI0NTA5ZWFkZDRlNmI3MDlhZGE1MGI5Y2YifX19");

    private final Map<Kind, ConfigColor> colors = new EnumMap<>(Kind.class);
    private Map<LivingEntity, Match> matches = Map.of();
    private ClientWorld world;

    @Override
    public void onInitializeClient() {
        EntityGlow.register(this::color);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newWorld) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
    }

    private void update(MinecraftClient client) {
        if (!DungeonContext.active() || client.world == null || client.player == null) {
            clear();
            return;
        }

        Dungeons settings = ClientConfig.configuration().dungeons;
        world = client.world;
        var found = new HashMap<LivingEntity, Match>();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity mob) || mob instanceof PlayerEntity || !alive(mob)) continue;
            if (mob instanceof ArmorStandEntity stand) {
                matchStand(stand, client.player, settings, found);
            } else if (!mob.isInvisible()) {
                matchMob(mob, client.player, settings, found);
            }
        }
        matches = found;
    }

    private void matchStand(ArmorStandEntity stand, PlayerEntity player, Dungeons settings, Map<LivingEntity, Match> found) {
        String name = name(stand);
        if (ZERO_HEALTH.matcher(name).find()) return;

        boolean felHead = settings.mobs.dungeonsFelMob && hasFelHead(stand);
        if (settings.mobs.dungeonsFelMob && (felHead || FEL_NAME.matcher(name).find())) {
            LivingEntity fel = nearestMob(stand, mob -> mob instanceof EndermanEntity);
            if (fel != null || felHead) {
                add(found, fel == null ? stand : fel, Kind.FEL, stand);
                return;
            }
        }
        if (settings.dungeonsWithersBox && WITHER_NAMES.contains(name)) {
            LivingEntity wither = nearestMob(stand, mob -> mob instanceof WitherEntity);
            if (wither != null) add(found, wither, Kind.WITHER, stand);
        } else if (settings.mobs.dungeonsStarredMobGlow && starredName(name)) {
            LivingEntity mob = nearestMob(stand, target -> true);
            if (mob != null && (settings.mobs.dungeonsStarredMobsEsp || player.canSee(mob))) {
                add(found, mob, starredKind(name), stand);
            }
        }
    }

    private static void matchMob(LivingEntity mob, PlayerEntity player, Dungeons settings, Map<LivingEntity, Match> found) {
        String name = name(mob);
        if (ZERO_HEALTH.matcher(name).find()) return;

        Kind kind;
        if (settings.mobs.dungeonsBatMobs && mob instanceof BatEntity) kind = Kind.BAT;
        else if (settings.dungeonsWithersBox && mob instanceof WitherEntity && WITHER_NAMES.contains(name)) kind = Kind.WITHER;
        else if (settings.mobs.dungeonsFelMob && mob instanceof EndermanEntity && FEL_NAME.matcher(name).find()) kind = Kind.FEL;
        else if (settings.mobs.dungeonsStarredMobGlow && starredName(name)
                && (settings.mobs.dungeonsStarredMobsEsp || player.canSee(mob))) kind = starredKind(name);
        else return;

        add(found, mob, kind, null);
    }

    private LivingEntity nearestMob(ArmorStandEntity stand, Predicate<LivingEntity> matchesType) {
        return world.getEntitiesByClass(LivingEntity.class, stand.getBoundingBox().expand(0.5, 3, 0.5),
                        mob -> eligibleTarget(mob) && matchesType.test(mob)).stream()
                .min(Comparator.comparingDouble((LivingEntity mob) -> mob.squaredDistanceTo(stand))
                        .thenComparingInt(Entity::getId)).orElse(null);
    }

    private static boolean eligibleTarget(LivingEntity mob) {
        return !(mob instanceof ArmorStandEntity) && !(mob instanceof PlayerEntity) && alive(mob)
                && !mob.isInvisible() && !ZERO_HEALTH.matcher(name(mob)).find();
    }

    private static void add(Map<LivingEntity, Match> found, LivingEntity mob, Kind kind, ArmorStandEntity stand) {
        double distance = stand == null ? 0 : stand.squaredDistanceTo(mob);
        found.merge(mob, new Match(kind, stand, distance), (current, candidate) -> {
            if (current.kind().starred() != candidate.kind().starred()) {
                return current.kind().starred() ? candidate : current;
            }
            return candidate.distance() < current.distance() ? candidate : current;
        });
    }

    private Integer color(Entity entity) {
        Match match = matches.get(entity);
        if (match == null || entity.getWorld() != world || !DungeonContext.active()
                || !alive((LivingEntity) entity) || (match.label() != null && !match.label().isAlive())) return null;
        if (!(entity instanceof ArmorStandEntity) && entity.isInvisible()) return null;

        Dungeons settings = ClientConfig.configuration().dungeons;
        Kind kind = match.kind();
        if (!kind.enabled(settings)) return null;
        String setting = switch (kind) {
            case STARRED -> settings.mobs.dungeonsStarredBoxColor;
            case STORMY -> settings.mobs.dungeonsStormyColor;
            case WITHERMANCER -> settings.mobs.dungeonsWithermancerColor;
            case COMMANDER -> settings.mobs.dungeonsZombieCommanderColor;
            case SKELETON_MASTER -> settings.mobs.dungeonsSkeletonMasterColor;
            case FEL -> settings.mobs.dungeonsFelColor;
            case BAT -> settings.mobs.dungeonsBatColor;
            case WITHER -> settings.dungeonsWithersBoxColor;
        };
        return colors.computeIfAbsent(kind, key -> new ConfigColor()).resolve(setting);
    }

    static boolean hasFelHead(ArmorStandEntity stand) {
        var head = stand.getEquippedStack(EquipmentSlot.HEAD);
        if (!head.isOf(Items.PLAYER_HEAD)) return false;

        var profile = head.get(DataComponentTypes.PROFILE);
        if (profile == null) return false;
        for (var texture : profile.properties().get("textures")) {
            if (FEL_HEAD_TEXTURES.contains(texture.value())) return true;
        }
        return false;
    }

    static boolean starredName(String name) {
        return name.indexOf('✮') >= 0 && !ZERO_HEALTH.matcher(name.stripTrailing()).find();
    }

    private static Kind starredKind(String name) {
        if (name.contains("Stormy")) return Kind.STORMY;
        if (name.contains("Withermancer")) return Kind.WITHERMANCER;
        if (name.contains("Zombie Commander")) return Kind.COMMANDER;
        if (name.contains("Skeleton Master")) return Kind.SKELETON_MASTER;
        return Kind.STARRED;
    }

    private static boolean alive(LivingEntity mob) {
        return mob.isAlive() && mob.getHealth() > 0.1F;
    }

    private static String name(Entity entity) {
        return Formatting.strip(entity.getName().getString()).strip();
    }

    private void clear() {
        world = null;
        matches = Map.of();
    }

    private record Match(Kind kind, ArmorStandEntity label, double distance) {
    }

    private enum Kind {
        STARRED, STORMY, WITHERMANCER, COMMANDER, SKELETON_MASTER, FEL, BAT, WITHER;

        boolean starred() {
            return this != FEL && this != BAT && this != WITHER;
        }

        boolean enabled(Dungeons settings) {
            return switch (this) {
                case FEL -> settings.mobs.dungeonsFelMob;
                case BAT -> settings.mobs.dungeonsBatMobs;
                case WITHER -> settings.dungeonsWithersBox;
                default -> settings.mobs.dungeonsStarredMobGlow;
            };
        }
    }
}
