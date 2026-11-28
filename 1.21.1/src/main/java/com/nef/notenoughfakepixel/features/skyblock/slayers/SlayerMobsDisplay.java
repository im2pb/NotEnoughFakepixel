package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.EntityGlow;
import com.nef.notenoughfakepixel.variables.Slayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RegisterEvents
public final class SlayerMobsDisplay implements ClientModInitializer {
    private static final String HEALTH_VALUE = "\\d+(?:,\\d{3})*(?:\\.\\d+)?[kmbtq]?";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PREFIX = Pattern.compile("^(?:☠\\uFE0F?|\\[lv\\s*-?\\d+])\\s*");
    private static final Pattern MODIFIER = Pattern.compile("^(e?corrupted(?: runic)?|runic)\\s+");
    private static final Pattern STATS = Pattern.compile("\\s+(?:(?<health>" + HEALTH_VALUE + ")"
            + "(?:\\s*/\\s*" + HEALTH_VALUE + ")?\\s*[❤♥]\\uFE0F?|\\d+\\s+hits?$)");
    private static final Pattern ZERO_HEALTH = Pattern.compile("0+(?:\\.0+)?[kmbtq]?");
    private static final Map<String, SlayerMob> NAMES = names();
    private static final Pattern MINIBOSS_NAME = Pattern.compile("(?<![\\p{L}\\p{N}_])(" + NAMES.entrySet().stream()
            .filter(entry -> !entry.getValue().boss())
            .map(entry -> Pattern.quote(entry.getKey()))
            .collect(Collectors.joining("|")) + ")(?![\\p{L}\\p{N}_])");
    private static final double MAX_HORIZONTAL_DISTANCE = 1.25;
    private static final double MAX_HEIGHT_ERROR = 1;
    private static final double MINIBOSS_HORIZONTAL_DISTANCE = 1.75;
    private static final double MINIBOSS_HEIGHT_ERROR = 1.5;

    private final ConfigColor bossColor = new ConfigColor();
    private final ConfigColor minibossColor = new ConfigColor();
    private Map<LivingEntity, Match> matches = Map.of();
    private ClientWorld world;

    @Override
    public void onInitializeClient() {
        EntityGlow.register(this::color);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newWorld) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
    }

    private void update(MinecraftClient client) {
        if (client.world == null || client.player == null || !SkyblockData.isSkyblock()) {
            clear();
            return;
        }

        var settings = ClientConfig.configuration().slayer;
        if (!settings.bossSettings.slayerBosses && !settings.minibossSettings.slayerMinibosses) {
            clear();
            return;
        }

        world = client.world;
        var found = new HashMap<LivingEntity, Match>();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || !entity.isAlive()) continue;
            var name = entity.getCustomName();
            if (name == null) continue;

            SlayerMob slayer = identify(name.getString());
            if (slayer == null) continue;

            if (living instanceof ArmorStandEntity label) {
                matchNameplate(label, slayer, found);
            } else if (matchesMob(living, slayer)) {
                found.put(living, new Match(slayer, null, 0));
            }
        }
        matches = found;
    }

    private void matchNameplate(ArmorStandEntity label, SlayerMob slayer, Map<LivingEntity, Match> found) {
        double x = label.getX();
        double y = label.getY();
        double z = label.getZ();
        boolean miniboss = !slayer.boss();
        double radius = miniboss ? MINIBOSS_HORIZONTAL_DISTANCE : MAX_HORIZONTAL_DISTANCE;
        var bounds = nearNameplate(x, y, z, radius);
        double targetX = label.getLerpTargetX();
        double targetY = label.getLerpTargetY();
        double targetZ = label.getLerpTargetZ();
        if (miniboss) {
            // World queries use the rendered bounds even when the best match is at the tracked position.
            bounds = bounds.union(nearNameplate(targetX, targetY, targetZ, radius)).expand(1.5, 1, 1.5);
        }
        LivingEntity nearest = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (LivingEntity mob : world.getEntitiesByClass(LivingEntity.class, bounds, candidate -> matchesMob(candidate, slayer))) {
            double score = nameplateScore(x - mob.getX(), y - mob.getBoundingBox().maxY, z - mob.getZ(), slayer, radius);
            if (miniboss) {
                // The mob and its packet-only nameplate can interpolate from different positions.
                double top = mob.getLerpTargetY() + (mob.getBoundingBox().maxY - mob.getY());
                double trackedScore = nameplateScore(targetX - mob.getLerpTargetX(),
                        targetY - top, targetZ - mob.getLerpTargetZ(), slayer, radius);
                score = Math.min(score, trackedScore);
            }
            if (score < bestScore) {
                nearest = mob;
                bestScore = score;
            }
        }
        if (nearest == null) return;

        Match existing = found.get(nearest);
        if (existing == null || (existing.label() != null && bestScore < existing.score())) {
            found.put(nearest, new Match(slayer, label, bestScore));
        }
    }

    private static Box nearNameplate(double x, double y, double z, double radius) {
        return new Box(x - radius, y - 5, z - radius, x + radius, y + 1, z + radius);
    }

    private static double nameplateScore(double dx, double dy, double dz, SlayerMob slayer, double radius) {
        double horizontal = dx * dx + dz * dz;
        double heightError = Math.abs(dy - slayer.nameplateOffset());
        double heightTolerance = slayer.boss() ? MAX_HEIGHT_ERROR : MINIBOSS_HEIGHT_ERROR;
        if (horizontal > radius * radius || heightError > heightTolerance) {
            return Double.POSITIVE_INFINITY;
        }
        return 4 * horizontal + heightError * heightError;
    }

    private static boolean matchesMob(LivingEntity mob, SlayerMob slayer) {
        if (!mob.isAlive() || mob.isInvisible()) return false;

        EntityType<?> type = mob.getType();
        if (type == slayer.type()) return true;
        return !slayer.boss() && ((slayer.type() == EntityType.ZOMBIE && type == EntityType.ZOMBIE_VILLAGER)
                || (slayer.type() == EntityType.SPIDER && type == EntityType.CAVE_SPIDER));
    }

    private Integer color(Entity entity) {
        Integer attunementColor = BlazeSlayerDisplay.INSTANCE.glowColor(entity);
        if (attunementColor != null) return attunementColor;

        Match match = matches.get(entity);
        if (match == null || entity.getWorld() != world || !SkyblockData.isSkyblock()
                || !matchesMob((LivingEntity) entity, match.slayer())
                || (match.label() != null && !match.label().isAlive())) return null;

        var settings = ClientConfig.configuration().slayer;
        if (match.slayer().boss()) {
            return settings.bossSettings.slayerBosses ? bossColor.resolve(settings.bossSettings.slayerBossColor) : null;
        }
        return settings.minibossSettings.slayerMinibosses ? minibossColor.resolve(settings.minibossSettings.slayerColor) : null;
    }

    private void clear() {
        world = null;
        matches = Map.of();
    }

    private static SlayerMob identify(String raw) {
        String name = WHITESPACE.matcher(Formatting.strip(raw)).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
        name = PREFIX.matcher(name).replaceFirst("");
        var stats = STATS.matcher(name);
        if (stats.find()) {
            String health = stats.group("health");
            if (health != null && ZERO_HEALTH.matcher(health.replace(",", "")).matches()) return null;
            name = name.substring(0, stats.start()).stripTrailing();
        }
        var modifier = MODIFIER.matcher(name);
        if (modifier.find()) {
            name = name.substring(modifier.end());
            // Fakepixel places an obfuscated E after corrupted mob names.
            if (modifier.group(1).contains("corrupted") && name.endsWith("e") && !NAMES.containsKey(name)) {
                name = name.substring(0, name.length() - 1);
            }
        }
        SlayerMob exact = NAMES.get(name);
        if (exact != null) return exact;

        // A player-owned soul also includes the original mob's name in its tag.
        if (name.contains("'s ") || name.contains("’s ")) return null;
        var miniboss = MINIBOSS_NAME.matcher(name);
        return miniboss.find() ? NAMES.get(miniboss.group(1)) : null;
    }

    static Slayer bossFamily(String name) {
        var mob = identify(name);
        if (mob == null || !mob.boss()) return Slayer.NONE;

        var type = mob.type();
        if (type == EntityType.ZOMBIE) return Slayer.REVENANT;
        if (type == EntityType.WOLF) return Slayer.SVEN;
        if (type == EntityType.SPIDER) return Slayer.TARANTULA;
        if (type == EntityType.ENDERMAN) return Slayer.VOIDGLOOM;
        if (type == EntityType.BLAZE) return Slayer.INFERNO;
        return Slayer.NONE;
    }

    private static Map<String, SlayerMob> names() {
        var names = new HashMap<String, SlayerMob>();
        add(names, new SlayerMob(EntityType.ZOMBIE, true, 0.25), "Revenant Horror", "Atoned Horror");
        add(names, new SlayerMob(EntityType.ZOMBIE, false, 0.25), "Deformed Revenant", "Revenant Champion",
                "Revenant Sycophant", "Atoned Revenant", "Atoned Champion");
        add(names, new SlayerMob(EntityType.WOLF, true, 0.25), "Sven Packmaster");
        add(names, new SlayerMob(EntityType.WOLF, false, 0.25), "Pack Enforcer", "Sven Alpha", "Sven Follower", "Sven Champion");
        add(names, new SlayerMob(EntityType.SPIDER, true, 0.95), "Tarantula Broodfather");
        add(names, new SlayerMob(EntityType.SPIDER, false, 0.25), "Tarantula Vermin", "Tarantula Beast", "Mutant Tarantula");
        add(names, new SlayerMob(EntityType.ENDERMAN, true, 0.95), "Voidgloom Seraph");
        add(names, new SlayerMob(EntityType.ENDERMAN, false, 0.25), "Voidling Devotee", "Voidling Radical", "Voidcrazed Maniac");
        add(names, new SlayerMob(EntityType.BLAZE, true, 0.25), "Inferno Demonlord");
        add(names, new SlayerMob(EntityType.BLAZE, false, 0.25), "Flare Demon", "Kindleheart Demon", "Burningsoul Demon");
        return Map.copyOf(names);
    }

    private static void add(Map<String, SlayerMob> names, SlayerMob mob, String... labels) {
        for (String label : labels) names.put(label.toLowerCase(Locale.ROOT), mob);
    }

    private record SlayerMob(EntityType<?> type, boolean boss, double nameplateOffset) {
    }

    private record Match(SlayerMob slayer, ArmorStandEntity label, double score) {
    }
}
