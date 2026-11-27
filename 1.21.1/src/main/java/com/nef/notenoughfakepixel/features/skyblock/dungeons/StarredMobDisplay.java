package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Dungeons;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.WorldBoxRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class StarredMobDisplay implements ClientModInitializer {
    private static final int BOX = 0;
    private static final int OUTLINE = 1;
    private static final Pattern ZERO_HEALTH = Pattern.compile("(?:^|\\s)0(?:\\s*/\\s*[^\\s❤♥]+)?\\s*[❤♥]$");
    @RegisterInstance
    private static final StarredMobDisplay INSTANCE = new StarredMobDisplay();

    private final WorldBoxRenderer renderer = new WorldBoxRenderer();
    private final ConfigColor normal = new ConfigColor();
    private final ConfigColor stormy = new ConfigColor();
    private final ConfigColor withermancer = new ConfigColor();
    private final ConfigColor commander = new ConfigColor();
    private final ConfigColor master = new ConfigColor();
    private List<StarredLabel> labels = List.of();
    private Map<Entity, StarredLabel> outlined = Map.of();
    private ClientWorld world;

    private StarredMobDisplay() {
    }

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newWorld) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(this::update);
        WorldRenderEvents.LAST.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private void update(MinecraftClient client) {
        if (!DungeonContext.active() || client.world == null || client.player == null) {
            clear();
            return;
        }
        var settings = ClientConfig.configuration().dungeons.mobs;
        int mode = settings.dungeonsStarredMobs;
        if (mode != BOX && mode != OUTLINE) {
            clear();
            return;
        }

        world = client.world;
        var boxLabels = new ArrayList<StarredLabel>();
        var outlineLabels = new HashMap<Entity, StarredLabel>();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity stand) || !stand.isAlive()) continue;
            String name = label(stand);
            if (!starredName(name)) continue;

            var match = new StarredLabel(stand, Kind.fromName(name));
            if (mode == BOX) {
                boxLabels.add(match);
            } else {
                LivingEntity mob = nearestMob(world, stand);
                if (mob != null && (settings.dungeonsStarredMobsEsp || client.player.canSee(mob))) {
                    outlineLabels.merge(mob, match, (first, second) -> stand.squaredDistanceTo(mob)
                            < first.stand().squaredDistanceTo(mob) ? second : first);
                }
            }
        }
        labels = boxLabels;
        outlined = outlineLabels;
    }

    static boolean starredName(String name) {
        return name.indexOf('✮') >= 0 && !ZERO_HEALTH.matcher(name.stripTrailing()).find();
    }

    private static String label(Entity entity) {
        return Formatting.strip(entity.getName().getString());
    }

    private static LivingEntity nearestMob(ClientWorld world, ArmorStandEntity stand) {
        return world.getEntitiesByClass(LivingEntity.class, stand.getBoundingBox().expand(0.5, 3, 0.5),
                        StarredMobDisplay::eligibleMob).stream()
                .min(Comparator.comparingDouble((LivingEntity mob) -> mob.squaredDistanceTo(stand))
                        .thenComparingInt(Entity::getId))
                .orElse(null);
    }

    static boolean eligibleMob(LivingEntity mob) {
        return !(mob instanceof ArmorStandEntity) && !(mob instanceof PlayerEntity)
                && mob.isAlive() && mob.getHealth() > 0.1F && !mob.isInvisible()
                && !ZERO_HEALTH.matcher(label(mob).stripTrailing()).find();
    }

    public static Integer outlineColor(Entity entity) {
        var match = INSTANCE.outlined.get(entity);
        if (match == null || !DungeonContext.active() || entity.getWorld() != INSTANCE.world
                || !eligibleMob((LivingEntity) entity) || !match.stand().isAlive()
                || !starredName(label(match.stand()))) return null;

        var settings = ClientConfig.configuration().dungeons.mobs;
        return settings.dungeonsStarredMobs == OUTLINE ? INSTANCE.color(match.kind(), settings) : null;
    }

    private void render(WorldRenderContext context) {
        if (labels.isEmpty() || world != context.world() || !DungeonContext.active()) return;
        var settings = ClientConfig.configuration().dungeons.mobs;
        if (settings.dungeonsStarredMobs != BOX) return;

        float delta = context.tickCounter().getTickDelta(false);
        var markers = new ArrayList<WorldBoxRenderer.Marker>(labels.size());
        for (var label : labels) {
            if (!label.stand().isAlive() || !starredName(label(label.stand()))) continue;
            int color = color(label.kind(), settings);
            if ((color >>> 24) != 0) {
                markers.add(new WorldBoxRenderer.Marker(label.kind().boxAt(label.stand().getLerpedPos(delta)), color));
            }
        }
        renderer.draw(context, markers, false, settings.dungeonsStarredMobsEsp);
    }

    private int color(Kind kind, Dungeons.Mobs settings) {
        return switch (kind) {
            case STORMY -> stormy.resolve(settings.dungeonsStormyColor);
            case WITHERMANCER -> withermancer.resolve(settings.dungeonsWithermancerColor);
            case COMMANDER -> commander.resolve(settings.dungeonsZombieCommanderColor);
            case SKELETON_MASTER -> master.resolve(settings.dungeonsSkeletonMasterColor);
            case FEL, NORMAL -> normal.resolve(settings.dungeonsStarredBoxColor);
        };
    }

    private void clear() {
        world = null;
        labels = List.of();
        outlined = Map.of();
    }

    private record StarredLabel(ArmorStandEntity stand, Kind kind) {
    }

    enum Kind {
        NORMAL(-2, 0), STORMY(-2, 0), WITHERMANCER(-2.6, -0.2),
        COMMANDER(-2, 0), SKELETON_MASTER(-2, 0), FEL(-3, 0);

        private final double bottom;
        private final double top;

        Kind(double bottom, double top) {
            this.bottom = bottom;
            this.top = top;
        }

        static Kind fromName(String name) {
            if (name.contains("Stormy")) return STORMY;
            if (name.contains("Withermancer")) return WITHERMANCER;
            if (name.contains("Zombie Commander")) return COMMANDER;
            if (name.contains("Skeleton Master")) return SKELETON_MASTER;
            if (name.contains("Fels")) return FEL;
            return NORMAL;
        }

        Box boxAt(Vec3d position) {
            return new Box(position.x - 0.5, position.y + bottom, position.z - 0.5,
                    position.x + 0.5, position.y + top, position.z + 0.5);
        }
    }
}
