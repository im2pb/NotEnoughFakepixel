package com.nef.notenoughfakepixel.features.skyblock.enchanting;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.features.skyblock.enchanting.ExperimentRound.Game;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ExperimentationSolver implements ClientModInitializer {
    private static final int NEXT_COLOR = 0xFF46FF69;

    @RegisterInstance
    public static final ExperimentationSolver INSTANCE = new ExperimentationSolver();

    private GenericContainerScreen screen;
    private ClientWorld world;
    private String profile;
    private Game game;
    private ExperimentRound round;
    private SuperpairsBoard pairs;

    private ExperimentationSolver() {
    }

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newWorld) -> clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (screen != null && !current(client)) clear();
        });
    }

    public void contentsReceived(MinecraftClient client, int syncId, List<ItemStack> contents) {
        if (!accepts(client, syncId)) return;
        if (pairs != null) pairs.contentsReceived(contents);
        else round.contentsReceived(contents);
    }

    public void slotReceived(MinecraftClient client, int syncId, int slot, ItemStack stack) {
        if (slot < 0 || !accepts(client, syncId)) return;
        if (pairs != null) pairs.slotReceived(slot, stack);
        else round.slotReceived(slot, stack);
    }

    public boolean blocksClick(MinecraftClient client, int syncId, int slot, SlotActionType action) {
        return round != null && (action == SlotActionType.PICKUP || action == SlotActionType.QUICK_MOVE)
                && current(client) && screen.getScreenHandler().syncId == syncId
                && ClientConfig.configuration().experimentation.experimentationPreventMissclicks
                && round.blocksClick(slot);
    }

    public boolean hidesTooltip(MinecraftClient client, HandledScreen<?> menu, Slot slot) {
        if (!(menu instanceof GenericContainerScreen chest) || slot == null || !SkyblockData.isSkyblock()
                || !ClientConfig.configuration().experimentation.experimentationHideTooltips) return false;

        Game opened = game(chest.getTitle());
        if (client.currentScreen != chest || slot.id >= chest.getScreenHandler().getInventory().size()
                || !enabled(opened)) return false;

        ItemStack stack = slot.getStack();
        if (opened == Game.SUPERPAIRS) {
            return SuperpairsBoard.cover(stack) && displayStack(client, menu, slot, stack) == stack;
        }
        return !stack.isOf(Items.CLOCK) && !stack.isOf(Items.BOOKSHELF);
    }

    public ItemStack displayStack(MinecraftClient client, HandledScreen<?> menu, Slot slot, ItemStack stack) {
        return pairs != null && screen == menu && current(client) ? pairs.rememberedStack(slot.id, stack) : stack;
    }

    public void render(MinecraftClient client, HandledScreen<?> menu, DrawContext context, int left, int top) {
        if (!current(client) || screen != menu) return;

        var options = ClientConfig.configuration().experimentation;
        if (pairs != null) {
            var hints = pairs.hints();
            if (hints.isEmpty()) return;
            context.draw();
            for (var hint : hints) {
                int color = dyeColor(switch (hint.kind()) {
                    case MATCHED -> options.supMatched;
                    case POSSIBLE -> options.supPossible;
                    case UNMATCHED -> options.supUnmatched;
                    case POWERUP -> options.supPower;
                });
                if (color == 0) continue;
                Slot slot = screen.getScreenHandler().getSlot(hint.slot());
                drawHint(context, left + slot.x, top + slot.y, color);
            }
            context.draw();
            return;
        }

        var clues = round.clues();
        if (clues.isEmpty()) return;
        context.draw();
        int upcomingColor = dyeColor(options.seqUpcoming);
        var matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0, 0, 300);
        for (var clue : clues) {
            Slot slot = screen.getScreenHandler().getSlot(clue.slot());
            int x = left + slot.x;
            int y = top + slot.y;
            if (clue.offset() == 0) {
                drawHint(context, x, y, NEXT_COLOR);
            } else if (clue.offset() == 1 && upcomingColor != 0) {
                drawHint(context, x, y, upcomingColor);
            }
            if (clue.number() > 0) {
                context.fill(RenderLayer.getGuiOverlay(), x + 2, y + 3, x + 14, y + 13, 0xC0000000);
                context.drawCenteredTextWithShadow(client.textRenderer, Integer.toString(clue.number()),
                        x + 8, y + 4, 0xFFFFFF);
            }
        }
        matrices.pop();
        context.draw();
    }

    private static int dyeColor(int id) {
        return id > 0 && id <= 16 ? 0xFF000000 | DyeColor.byId(id - 1).getEntityColor() : 0;
    }

    private static void drawHint(DrawContext context, int x, int y, int color) {
        // The overlay layer passes depth testing even over a full-slot item model.
        var layer = RenderLayer.getGuiOverlay();
        context.fill(layer, x + 1, y + 1, x + 15, y + 15, 0x40000000 | (color & 0xFFFFFF));
        context.fill(layer, x, y, x + 16, y + 1, color);
        context.fill(layer, x, y + 15, x + 16, y + 16, color);
        context.fill(layer, x, y + 1, x + 1, y + 15, color);
        context.fill(layer, x + 15, y + 1, x + 16, y + 15, color);
    }

    private boolean accepts(MinecraftClient client, int syncId) {
        if (syncId <= 0 || client.player == null || client.world == null || !SkyblockData.isSkyblock()
                || !(client.currentScreen instanceof GenericContainerScreen menu)
                || menu.getScreenHandler().syncId != syncId
                || menu.getScreenHandler() != client.player.currentScreenHandler) return false;

        if (current(client)) return true;
        clear();
        Game opened = game(menu.getTitle());
        int size = menu.getScreenHandler().getInventory().size();
        if (!enabled(opened) || size < 27) return false;

        screen = menu;
        world = client.world;
        profile = SkyblockData.currentProfile();
        game = opened;
        if (opened == Game.SUPERPAIRS) pairs = new SuperpairsBoard(size);
        else round = new ExperimentRound(opened, size);
        return true;
    }

    private boolean current(MinecraftClient client) {
        return screen != null && screen == client.currentScreen && world == client.world
                && client.player != null && screen.getScreenHandler() == client.player.currentScreenHandler
                && SkyblockData.isSkyblock() && Objects.equals(profile, SkyblockData.currentProfile()) && enabled(game);
    }

    private void clear() {
        screen = null;
        world = null;
        profile = null;
        game = null;
        round = null;
        pairs = null;
    }

    private static boolean enabled(Game game) {
        if (game == null) return false;
        var options = ClientConfig.configuration().experimentation;
        return options.experimentationSolvers && switch (game) {
            case CHRONOMATRON -> options.experimentationChronomatronSolver;
            case ULTRASEQUENCER -> options.experimentationUltraSequencerSolver;
            case SUPERPAIRS -> options.experimentationSuperpairsSolver;
        };
    }

    private static Game game(Text title) {
        String name = Formatting.strip(title.getString()).strip().toLowerCase(Locale.ROOT);
        if (matches(name, "chronomatron") || matches(name, "metaphysical")) return Game.CHRONOMATRON;
        if (matches(name, "ultrasequencer")) return Game.ULTRASEQUENCER;
        if (name.startsWith("superpairs (") && name.endsWith(")")) return Game.SUPERPAIRS;
        return null;
    }

    private static boolean matches(String title, String game) {
        return title.equals(game) || title.startsWith(game + " (") && title.endsWith(")");
    }
}
