package com.nef.notenoughfakepixel.features.skyblock.crimson;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.IslandWaypoints.Waypoint;
import com.nef.notenoughfakepixel.features.skyblock.overlays.waypoints.WaypointRenderer;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.EntityGlow;
import com.nef.notenoughfakepixel.utils.SkullTextures;
import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import com.nef.notenoughfakepixel.variables.Area;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class AshfangHelper implements ClientModInitializer {
    private static final double SOUL_DAMAGE = 2_000_000;
    private static final double BASE_HEALTH = 50_000_000;
    private static final long RESPAWN_ESTIMATE_MS = 125_000;
    private static final int[] RESPAWN_NOTICES = {120, 60, 30, 10, 5};
    private static final String TEXTURE_HOST = "textures.minecraft.net/texture/";
    private static final String ORB_TEXTURE = TEXTURE_HOST + "1a69ccf7ad904c9a852ea2ff3f5b4e23adebf72ed12d5f24b78ce2d44b4a2";
    private static final String SOUL_TEXTURE = TEXTURE_HOST + "8287b397daf9516a0bd76f5f1b7bf979515df3d5d833e0635fa68b37ee082212";
    private static final String AMOUNT = "\\d+(?:,\\d{3})*(?:\\.\\d+)?[kmb]?";
    private static final Pattern NAMEPLATE = Pattern.compile("^(?:\\[lv\\s*-?\\d+]\\s*)?"
            + "(?:(?:e?corrupted(?: runic)?|runic)\\s+)?ashfang(?: (underling|follower|acolyte))?e?\\s+"
            + "(" + AMOUNT + ")(?:\\s*/\\s*(" + AMOUNT + "))?\\s*[❤♥]\\uFE0F?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SOUL_HIT = Pattern.compile("^(?:CRITICAL HIT\\s+)?The Blazing Soul dealt "
            + AMOUNT + " damage to Ashfang[!.]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIGHT_SPAM = Pattern.compile("^(?:Ashfang (?:Underling|Follower|Acolyte) can only be damaged by .+"
            + "|Ashfang(?: (?:Underling|Follower|Acolyte))?['’]s .+ hit you for .+"
            + "|Your attack was nullified by the Scorched Barrier!)$", Pattern.CASE_INSENSITIVE);

    @RegisterInstance
    public static final AshfangHelper INSTANCE = new AshfangHelper();

    private final WaypointRenderer waypoints = new WaypointRenderer();
    private final ConfigColor bossColor = new ConfigColor();
    private final ConfigColor orbColor = new ConfigColor();
    private final Map<String, Head> textures = new HashMap<>();
    private ClientWorld world;
    private Boss boss;
    private List<ArmorStandEntity> orbs = List.of();
    private Map<Entity, Integer> highlights = Map.of();
    private int souls;
    private long lastHitSound = -1;
    private long defeatedAt = -1;
    private int nextNotice;
    private boolean estimateAnnounced;
    private UUID defeatedBoss;

    private AshfangHelper() {}

    @Override
    public void onInitializeClient() {
        EntityGlow.register(this::glowColor);
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, nextWorld) -> clear());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> waypoints.close());
        ClientReceiveMessageEvents.ALLOW_GAME.register(this::allowMessage);
        ClientReceiveMessageEvents.GAME.register(this::onMessage);
        WorldRenderEvents.LAST.register(this::render);
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || !onCrimson()) {
            clear();
            return;
        }
        if (world != client.world) {
            clear();
            world = client.world;
        }

        var config = ClientConfig.configuration();
        if (inArena(client)) scan(client, config.crimson.ashfangSettings.crimsonAshfangHitboxes);
        else clearEncounter();
        updateRespawn(client, config.crimson.bossNotifiers.crimsonAshfangNotifier, Util.getMeasuringTimeMs());
    }

    private void scan(MinecraftClient client, boolean highlight) {
        var nearbyOrbs = new ArrayList<ArmorStandEntity>();
        var soulLabels = new ArrayList<ArmorStandEntity>();
        var soulHeads = new ArrayList<ArmorStandEntity>();
        var glows = new HashMap<Entity, Integer>();
        Boss closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof ArmorStandEntity stand) || !stand.isAlive() || !inBounds(stand.getBlockPos())) continue;
            Head head = head(stand.getEquippedStack(EquipmentSlot.HEAD));
            if (head == Head.ORB) nearbyOrbs.add(stand);
            if (head == Head.SOUL) soulHeads.add(stand);

            var customName = stand.getCustomName();
            if (customName == null) continue;
            String label = plain(customName.getString());
            if (label.equalsIgnoreCase("Blazing Soul")) {
                soulLabels.add(stand);
                continue;
            }
            var parsed = parseLabel(label);
            if (parsed == null || parsed.health().current() <= 0) continue;
            if (parsed.minion() == null) {
                if (stand.getUuid().equals(defeatedBoss)) continue;
                double distance = client.player.squaredDistanceTo(stand);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = new Boss(stand, parsed.health());
                }
            } else if (highlight) {
                var mob = minion(client.world, stand);
                if (mob != null) glows.put(mob, parsed.minion().color);
            }
        }

        if (highlight) {
            for (ArmorStandEntity label : soulLabels) {
                nearestSoul(label, soulHeads).ifPresent(stand -> glows.put(stand, 0xFFFF00));
            }
        }
        var settings = ClientConfig.configuration();
        if (settings.crimson.ashfangSettings.crimsonGravityOrbWaypoint && settings.waypoints.generalWaypointToggle
                && nearbyOrbs.stream().anyMatch(orb -> !orbs.contains(orb))) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_ITEM_PICKUP, 1.5f));
        }
        if (boss != null && closest != null && boss.label() == closest.label()
                && closest.health().current() < boss.health().current()) playHit(client);

        boss = closest;
        orbs = nearbyOrbs;
        souls = soulLabels.size();
        highlights = glows;
    }

    private static BlazeEntity minion(ClientWorld world, ArmorStandEntity label) {
        double x = label.getLerpTargetX();
        double y = label.getLerpTargetY();
        double z = label.getLerpTargetZ();
        var bounds = new Box(x - 2, y - 4, z - 2, x + 2, y + 1, z + 2);
        return world.getEntitiesByClass(BlazeEntity.class, bounds, blaze -> blaze.isAlive() && !blaze.isInvisible())
                .stream().filter(blaze -> minionDistance(x, y, z, blaze) <= 2.25)
                .min(Comparator.comparingDouble(blaze -> minionDistance(x, y, z, blaze))).orElse(null);
    }

    private static double minionDistance(double x, double y, double z, BlazeEntity mob) {
        double dx = x - mob.getLerpTargetX();
        double dz = z - mob.getLerpTargetZ();
        double top = mob.getLerpTargetY() + mob.getHeight() + 0.25;
        double dy = y - top;
        return 4 * (dx * dx + dz * dz) + dy * dy;
    }

    private static Optional<ArmorStandEntity> nearestSoul(ArmorStandEntity label, List<ArmorStandEntity> heads) {
        // The label is a separate hologram that disappears as soon as the soul launches.
        return heads.stream().filter(head -> head.squaredDistanceTo(label.getX(), label.getY() - 2, label.getZ()) < 2.25)
                .min(Comparator.comparingDouble(head -> head.squaredDistanceTo(label.getX(), label.getY() - 2, label.getZ())));
    }

    private Head head(ItemStack stack) {
        if (!stack.isOf(Items.PLAYER_HEAD)) return Head.OTHER;
        var profile = stack.get(DataComponentTypes.PROFILE);
        if (profile != null) {
            for (var property : profile.properties().get("textures")) {
                Head kind = textures.computeIfAbsent(property.value(), AshfangHelper::texture);
                if (kind != Head.OTHER) return kind;
            }
        }
        return "BURNING_EYE".equals(SkyblockItemData.id(stack)) ? Head.SOUL : Head.OTHER;
    }

    private static Head texture(String encoded) {
        String url = SkullTextures.skinUrl(encoded);
        if (SkullTextures.matches(url, ORB_TEXTURE)) return Head.ORB;
        return SkullTextures.matches(url, SOUL_TEXTURE) ? Head.SOUL : Head.OTHER;
    }

    private boolean allowMessage(Text message, boolean overlay) {
        var client = MinecraftClient.getInstance();
        if (overlay || !inArena(client)) return true;
        var lines = message.getString().lines().map(AshfangHelper::plain).filter(line -> !line.isEmpty()).toList();
        if (lines.stream().anyMatch(line -> SOUL_HIT.matcher(line).matches())) playHit(client);
        return !ClientConfig.configuration().crimson.ashfangSettings.crimsonAshfangMuteChat || lines.isEmpty()
                || !lines.stream().allMatch(line -> FIGHT_SPAM.matcher(line).matches() || SOUL_HIT.matcher(line).matches());
    }

    public static boolean muteSound(MinecraftClient client, PlaySoundS2CPacket packet) {
        return inArena(client) && ClientConfig.configuration().crimson.ashfangSettings.crimsonAshfangMuteSound
                && packet.getSound().value().getId().equals(SoundEvents.ENTITY_BAT_HURT.getId())
                && Math.abs(packet.getVolume() - 1f) < 0.001f && Math.abs(packet.getPitch() - 2f) < 0.001f
                && client.player.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ()) < 4;
    }

    private void playHit(MinecraftClient client) {
        if (!ClientConfig.configuration().crimson.ashfangSettings.crimsonAshfangHurtSound) return;
        long now = Util.getMeasuringTimeMs();
        if (lastHitSound >= 0 && now - lastHitSound < 250) return;
        lastHitSound = now;
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_WITHER_HURT, 0.7f, 0.7f));
    }

    private void onMessage(Text message, boolean overlay) {
        if (overlay || !onCrimson() || message.getString().lines()
                .map(AshfangHelper::plain).noneMatch("ASHFANG DOWN!"::equalsIgnoreCase)) return;
        long now = Util.getMeasuringTimeMs();
        if (defeatedAt >= 0 && now - defeatedAt < 5_000) return;
        defeatedAt = now;
        nextNotice = 0;
        estimateAnnounced = false;
        defeatedBoss = boss == null ? null : boss.label().getUuid();
        clearEncounter();
    }

    private void updateRespawn(MinecraftClient client, boolean notify, long now) {
        if (defeatedAt < 0) return;
        if (boss != null && now - defeatedAt >= 2_000) {
            if (notify) title(client, "Ashfang Ready");
            defeatedAt = -1;
            defeatedBoss = null;
            return;
        }
        long remaining = RESPAWN_ESTIMATE_MS - (now - defeatedAt);
        if (remaining <= 0) {
            if (notify && !estimateAnnounced) title(client, "Ashfang Respawn Due");
            estimateAnnounced = true;
            return;
        }

        int notice = 0;
        while (nextNotice < RESPAWN_NOTICES.length && remaining <= RESPAWN_NOTICES[nextNotice] * 1_000L) {
            notice = RESPAWN_NOTICES[nextNotice++];
        }
        if (notify && notice > 0) {
            client.inGameHud.getChatHud().addMessage(
                    Text.literal("[NEF] Ashfang may respawn in " + notice + "s.").formatted(Formatting.YELLOW));
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1f));
        }
    }

    private static void title(MinecraftClient client, String title) {
        client.inGameHud.setTitleTicks(2, 25, 2);
        client.inGameHud.setSubtitle(Text.empty());
        client.inGameHud.setTitle(Text.literal(title).formatted(Formatting.GOLD));
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_WITHER_SPAWN, 1f));
    }

    private Integer glowColor(Entity entity) {
        Integer color = highlights.get(entity);
        return color != null && entity.getWorld() == world && entity.isAlive() && onCrimson()
                && ClientConfig.configuration().crimson.ashfangSettings.crimsonAshfangHitboxes ? color : null;
    }

    private void render(WorldRenderContext context) {
        var client = MinecraftClient.getInstance();
        if (context.world() != world || !inArena(client)) return;
        var config = ClientConfig.configuration();
        if (!config.waypoints.generalWaypointToggle) return;

        var settings = config.crimson.ashfangSettings;
        if (showsBossWaypoint()) {
            var pos = boss.label().getBlockPos().down(6);
            var marker = Waypoint.atBlock("Ashfang", pos.getX(), pos.getY(), pos.getZ());
            waypoints.render(context, List.of(marker), true, bossColor.resolve(settings.crimsonAshfangWaypointColor), 2, true, true);
        }
        if (settings.crimsonGravityOrbWaypoint) {
            var markers = orbs.stream().filter(Entity::isAlive)
                    .map(orb -> new Waypoint("Gravity Orb", orb.getX(), orb.getY(), orb.getZ())).toList();
            waypoints.render(context, markers, true, orbColor.resolve(settings.crimsonBlazingSoulWaypointColor), 2, true, true);
        }
    }

    public boolean showsBossWaypoint() {
        var client = MinecraftClient.getInstance();
        return boss != null && boss.label().isAlive() && client.world == world && inArena(client)
                && ClientConfig.configuration().crimson.ashfangSettings.crimsonAshfangWaypoint;
    }

    Stats stats() {
        return boss == null || !boss.label().isAlive() ? null
                : new Stats(boss.health(), souls);
    }

    private void clear() {
        clearEncounter();
        world = null;
        defeatedAt = -1;
        defeatedBoss = null;
        lastHitSound = -1;
    }

    private void clearEncounter() {
        boss = null;
        orbs = List.of();
        highlights = Map.of();
        souls = 0;
        textures.clear();
    }

    static boolean inArena(MinecraftClient client) {
        return client.player != null && onCrimson()
                && (SkyblockData.currentArea() == Area.ASHFANG || inBounds(client.player.getBlockPos()));
    }

    private static boolean inBounds(BlockPos pos) {
        return pos.getX() >= -510 && pos.getX() <= -450 && pos.getY() >= 100 && pos.getY() <= 200
                && pos.getZ() >= -1040 && pos.getZ() <= -990;
    }

    private static boolean onCrimson() {
        return SkyblockData.isSkyblock() && SkyblockData.currentLocation().isCrimson();
    }

    private static String plain(String text) {
        return Formatting.strip(text).strip();
    }

    private static Label parseLabel(String text) {
        var match = NAMEPLATE.matcher(text);
        if (!match.matches()) return null;
        double current = healthAmount(match.group(2));
        double max = match.group(3) == null ? BASE_HEALTH : healthAmount(match.group(3));
        if (!Double.isFinite(current) || !Double.isFinite(max) || max <= 0) return null;
        Minion minion = match.group(1) == null ? null : Minion.valueOf(match.group(1).toUpperCase(Locale.ROOT));
        return new Label(minion, new Health(current, max));
    }

    private static double healthAmount(String text) {
        int end = text.length() - 1;
        double multiplier = switch (Character.toLowerCase(text.charAt(end))) {
            case 'k' -> 1_000;
            case 'm' -> 1_000_000;
            case 'b' -> 1_000_000_000;
            default -> 1;
        };
        String value = multiplier == 1 ? text : text.substring(0, end);
        return Double.parseDouble(value.replace(",", "")) * multiplier;
    }

    record Health(double current, double max) {}

    record Stats(Health health, int souls) {
        int hitsNeeded() {
            return (int) Math.ceil(health.current() / SOUL_DAMAGE);
        }
    }

    private record Label(Minion minion, Health health) {}
    private record Boss(ArmorStandEntity label, Health health) {}
    private enum Head { OTHER, ORB, SOUL }
    private enum Minion {
        UNDERLING(0xFF0000), FOLLOWER(0xFFFFFF), ACOLYTE(0x0000FF);

        private final int color;

        Minion(int color) {
            this.color = color;
        }
    }
}
