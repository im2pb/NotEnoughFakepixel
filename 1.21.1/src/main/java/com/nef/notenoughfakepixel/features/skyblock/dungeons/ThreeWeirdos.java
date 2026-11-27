package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.WorldBoxRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@RegisterEvents
public final class ThreeWeirdos implements ClientModInitializer {
    private static final List<String> CLUES = List.of(
            "The reward is not in my chest!",
            "At least one of them is lying, and the reward is not in ",
            "My chest doesn't have the reward we are all telling the truth.",
            "My chest has the reward and I'm telling the truth!",
            "The reward isn't in any of our chests.",
            "Both of them are telling the truth. Also, ");
    private static final Pattern NPC_LINE = Pattern.compile("^\\[NPC]\\s+([^:]+):\\s*(.+)$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final List<Direction> ADJACENT = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final int CHEST_Y = 69;

    private final WorldBoxRenderer renderer = new WorldBoxRenderer();
    private String answer;
    private BlockPos chest;

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(this::allowMessage);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> clearAnswer());
        ClientTickEvents.END_CLIENT_TICK.register(this::updateChest);
        WorldRenderEvents.LAST.register(this::render);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    private boolean allowMessage(Text message, boolean overlay) {
        if (overlay || !enabled()) return true;
        String line = normalized(message.getString());
        if (isSolved(line)) {
            clearAnswer();
            return true;
        }
        String name = answerName(line);
        if (name == null) return true;

        var client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return true;
        if (!name.equals(answer)) {
            answer = name;
            chest = null;
            client.inGameHud.getChatHud().addMessage(Text.literal("[Riddle Solver] ").formatted(Formatting.GREEN)
                    .append(Text.literal(name).formatted(Formatting.RED))
                    .append(Text.literal(" has the blessing!").formatted(Formatting.WHITE)));
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F));
            updateChest(client);
        }
        return false;
    }

    static String answerName(String line) {
        var npc = NPC_LINE.matcher(line);
        if (!npc.matches()) return null;

        String clue = npc.group(2);
        return CLUES.stream().anyMatch(clue::contains) ? npc.group(1).strip() : null;
    }

    static boolean isSolved(String line) {
        return line.startsWith("PUZZLE SOLVED!") && line.contains("wasn't fooled by three weirdos!");
    }

    private static String normalized(String text) {
        return WHITESPACE.matcher(Formatting.strip(text).replace('’', '\'')).replaceAll(" ").strip();
    }

    private void updateChest(MinecraftClient client) {
        if (!enabled() || client.world == null || client.player == null) {
            clearAnswer();
            return;
        }
        if (answer == null) return;

        Predicate<BlockPos> isChest = position -> client.world.getBlockState(position).isOf(Blocks.CHEST);
        if (chest != null && isChest.test(chest)) return;

        chest = null;
        double nearest = Double.POSITIVE_INFINITY;
        Pattern npcName = Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(answer) + "(?![\\p{L}\\p{N}_])");
        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player || entity.isRemoved() || !matchesNpc(entity, npcName)) continue;
            double distance = client.player.squaredDistanceTo(entity);
            if (distance >= nearest) continue;

            BlockPos candidate = findChest(entity.getBlockPos(), isChest);
            if (candidate != null) {
                chest = candidate;
                nearest = distance;
            }
        }
    }

    private static boolean matchesNpc(Entity entity, Pattern name) {
        Text custom = entity.getCustomName();
        Text label = custom == null ? entity.getName() : custom;
        return name.matcher(normalized(label.getString())).find();
    }

    static BlockPos findChest(BlockPos npc, Predicate<BlockPos> isChest) {
        BlockPos legacy = adjacentChest(new BlockPos(npc.getX(), CHEST_Y, npc.getZ()), isChest);
        if (legacy != null) return legacy;
        for (int y = npc.getY(); y >= npc.getY() - 2; y--) {
            if (y == CHEST_Y) continue;
            BlockPos chest = adjacentChest(new BlockPos(npc.getX(), y, npc.getZ()), isChest);
            if (chest != null) return chest;
        }
        return null;
    }

    private static BlockPos adjacentChest(BlockPos origin, Predicate<BlockPos> isChest) {
        for (Direction direction : ADJACENT) {
            BlockPos adjacent = origin.offset(direction);
            if (isChest.test(adjacent)) return adjacent;
        }
        return null;
    }

    private void render(WorldRenderContext context) {
        if (chest == null || !enabled()) return;
        Box box = new Box(chest).expand(0.05, 0.002, 0.05);
        renderer.draw(context, List.of(new WorldBoxRenderer.Marker(box, 0x8000FF00)), true, false);
    }

    private static boolean enabled() {
        return DungeonContext.active() && ClientConfig.configuration().dungeons.puzzles.dungeonsThreeWeirdos;
    }

    private void clearAnswer() {
        answer = null;
        chest = null;
    }
}
