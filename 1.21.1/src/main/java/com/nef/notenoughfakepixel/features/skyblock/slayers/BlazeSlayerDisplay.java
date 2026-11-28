package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class BlazeSlayerDisplay implements ClientModInitializer {
    private static final double FIGHT_RANGE_SQUARED = 32 * 32;
    private static final double PILLAR_RANGE_SQUARED = 30 * 30;
    private static final Pattern PILLAR = Pattern.compile("([1-9]\\d?)s\\s+([1-9]\\d?)\\s+hits?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTUNEMENT = Pattern.compile(
            "(ASHEN|SPIRIT|CRYSTAL|AURIC|IMMUNE)(?:\\s+☕\\s*\\d+)?(?:\\s+\\d{1,2}(?::\\d{2})?)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COMBATANT = Pattern.compile(
            "(?:☠\\s+)?(Inferno Demonlord|Quazii|Typhoeus)\\s+(\\d[\\d,.]*[kmb]?)[ ]*[❤♥]",
            Pattern.CASE_INSENSITIVE);

    @RegisterInstance
    public static final BlazeSlayerDisplay INSTANCE = new BlazeSlayerDisplay();

    private Map<LivingEntity, Attunement> attunedMobs = Map.of();
    private List<Fighter> fighters = List.of();
    private ClientWorld world;
    private ArmorStandEntity pillar;
    private Text pillarTitle;
    private long lastPillarSound;

    private BlazeSlayerDisplay() {}

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newWorld) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderPillar(context));
    }

    private void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || !active()) {
            clear();
            return;
        }
        world = client.world;
        var settings = ClientConfig.configuration().slayer;
        boolean trackFight = settings.blazeAttunementSettings.slayerBlazeAttunementOutline
                || settings.blazeAttunementSettings.slayerBlazeAttunementFill || settings.bossSettings.slayerBossHP;
        boolean trackPillar = settings.infernoDemonlord.slayerFirePillarDisplay;
        var health = new ArrayList<HealthLabel>();
        var statuses = new ArrayList<StatusLabel>();
        ArmorStandEntity nearestPillar = null;
        int seconds = 0;
        double nearestPillarDistance = PILLAR_RANGE_SQUARED;
        if (trackFight || trackPillar) {
            for (Entity entity : client.world.getEntities()) {
                if (!(entity instanceof ArmorStandEntity stand) || !stand.isAlive() || stand.getCustomName() == null) continue;
                double distance = client.player.squaredDistanceTo(stand);
                if (distance > FIGHT_RANGE_SQUARED) continue;

                String label = plain(stand.getCustomName().getString());
                if (trackFight) {
                    var target = combatant(label);
                    if (target != null) health.add(new HealthLabel(stand, target));
                    var status = attunement(label);
                    if (status != null) statuses.add(new StatusLabel(stand, status));
                }
                if (trackPillar && distance < nearestPillarDistance) {
                    int remaining = pillarSeconds(label);
                    if (remaining != 0) {
                        nearestPillar = stand;
                        nearestPillarDistance = distance;
                        seconds = remaining;
                    }
                }
            }
        }

        var targets = new HashMap<LivingEntity, Attunement>();
        var visibleFighters = new ArrayList<Fighter>();
        for (var label : health) {
            Attunement status = nearestStatus(label, statuses);
            var mob = nearestMob(world, label);
            if (mob != null && status != null && status != Attunement.IMMUNE) targets.put(mob, status);
            visibleFighters.add(new Fighter(label.stand(), label.kind(), status));
        }
        attunedMobs = targets;
        fighters = visibleFighters;
        updatePillar(client, nearestPillar, seconds);
    }

    private static LivingEntity nearestMob(ClientWorld world, HealthLabel label) {
        var stand = label.stand();
        var bounds = new Box(stand.getX() - 1.25, stand.getY() - 4, stand.getZ() - 1.25,
                stand.getX() + 1.25, stand.getY() + 0.5, stand.getZ() + 1.25);
        return world.getEntitiesByClass(LivingEntity.class, bounds,
                        mob -> mob.getType() == label.kind().entityType && mob.isAlive() && !mob.isInvisible())
                .stream().min(Comparator.comparingDouble(mob -> stand.getPos()
                        .squaredDistanceTo(mob.getX(), mob.getBoundingBox().maxY + 0.25, mob.getZ()))).orElse(null);
    }

    private static Attunement nearestStatus(HealthLabel label, List<StatusLabel> statuses) {
        var stand = label.stand();
        Attunement result = null;
        double closest = 0.65 * 0.65;
        for (var status : statuses) {
            if (!label.kind().supports(status.attunement())) continue;
            double rise = status.stand().getY() - stand.getY();
            if (rise < 0.1 || rise > 1.0) continue;
            double score = status.stand().squaredDistanceTo(stand.getX(), stand.getY() + 0.5, stand.getZ());
            if (score < closest) {
                closest = score;
                result = status.attunement();
            }
        }
        return result;
    }

    List<Text> healthLines(Entity player) {
        var active = fighters.stream().filter(fighter -> fighter.stand().isAlive() && fighter.stand().getCustomName() != null)
                .sorted(Comparator.comparingDouble(fighter -> player.squaredDistanceTo(fighter.stand()))).toList();
        for (var fighter : active) {
            if (fighter.kind() == Combatant.INFERNO) return List.of(healthLine(fighter));
        }
        return active.stream().map(BlazeSlayerDisplay::healthLine).toList();
    }

    private static Text healthLine(Fighter fighter) {
        var label = fighter.stand().getCustomName().copy();
        var status = fighter.attunement();
        if (status != null) {
            label.append(Text.literal(" [" + status.name() + "]").styled(style -> style.withColor(status.color)));
        }
        return label;
    }

    Integer glowColor(Entity entity) {
        return outlineOrFill(entity, true);
    }

    public int tint(LivingEntity entity, int original) {
        Integer color = outlineOrFill(entity, false);
        return color == null ? original : (original & 0xFF000000) | color;
    }

    private Integer outlineOrFill(Entity entity, boolean outline) {
        var status = attunedMobs.get(entity);
        if (status == null || entity.getWorld() != world || !entity.isAlive() || !active()) return null;
        var settings = ClientConfig.configuration().slayer.blazeAttunementSettings;
        return (outline ? settings.slayerBlazeAttunementOutline : settings.slayerBlazeAttunementFill) ? status.color : null;
    }

    private void updatePillar(MinecraftClient client, ArmorStandEntity nearest, int seconds) {
        long now = Util.getMeasuringTimeMs();
        if (nearest != pillar) lastPillarSound = now;
        pillar = nearest;
        pillarTitle = nearest == null ? null : nearest.getCustomName();
        if (pillar != null && now - lastPillarSound >= seconds * 150L) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1f));
            lastPillarSound = now;
        }
    }

    private void renderPillar(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (pillarTitle == null || pillar == null || !pillar.isAlive() || !active() || client.options.hudHidden
                || !ClientConfig.configuration().slayer.infernoDemonlord.slayerFirePillarDisplay) return;
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(context.getScaledWindowWidth() / 2f, context.getScaledWindowHeight() / 2f - 32, 0);
            matrices.scale(4, 4, 1);
            context.drawCenteredTextWithShadow(client.textRenderer, pillarTitle, 0, 0, 0xFFFFFF);
        } finally {
            matrices.pop();
        }
    }

    private void clear() {
        world = null;
        attunedMobs = Map.of();
        fighters = List.of();
        pillar = null;
        pillarTitle = null;
    }

    private static Combatant combatant(String label) {
        var match = COMBATANT.matcher(label);
        if (!match.matches() || match.group(2).matches("0+(?:[.,]0+)?[kmb]?")) return null;
        return switch (match.group(1).toLowerCase(Locale.ROOT)) {
            case "inferno demonlord" -> Combatant.INFERNO;
            case "quazii" -> Combatant.QUAZII;
            case "typhoeus" -> Combatant.TYPHOEUS;
            default -> null;
        };
    }

    private static Attunement attunement(String label) {
        var match = ATTUNEMENT.matcher(label);
        return match.matches() ? Attunement.valueOf(match.group(1).toUpperCase(Locale.ROOT)) : null;
    }

    private static int pillarSeconds(String label) {
        var match = PILLAR.matcher(label);
        return match.matches() ? Integer.parseInt(match.group(1)) : 0;
    }

    private static String plain(String label) {
        return Normalizer.normalize(Formatting.strip(label), Normalizer.Form.NFKC).strip();
    }

    private static boolean active() {
        return SkyblockData.isSkyblock() && SkyblockData.currentLocation().isCrimson();
    }

    private record HealthLabel(ArmorStandEntity stand, Combatant kind) {}
    private record StatusLabel(ArmorStandEntity stand, Attunement attunement) {}
    private record Fighter(ArmorStandEntity stand, Combatant kind, Attunement attunement) {}

    private enum Combatant {
        INFERNO(EntityType.BLAZE), QUAZII(EntityType.WITHER_SKELETON), TYPHOEUS(EntityType.ZOMBIFIED_PIGLIN);

        private final EntityType<?> entityType;

        Combatant(EntityType<?> entityType) {
            this.entityType = entityType;
        }

        boolean supports(Attunement attunement) {
            return attunement == Attunement.IMMUNE || switch (this) {
                case INFERNO -> true;
                case QUAZII -> attunement == Attunement.SPIRIT || attunement == Attunement.CRYSTAL;
                case TYPHOEUS -> attunement == Attunement.ASHEN || attunement == Attunement.AURIC;
            };
        }
    }

    private enum Attunement {
        ASHEN(0x404040), SPIRIT(0xFFFFFF), CRYSTAL(0x00FFFF), AURIC(0xFFFF00), IMMUNE(0xAA0000);

        private final int color;

        Attunement(int color) {
            this.color = color;
        }
    }
}
