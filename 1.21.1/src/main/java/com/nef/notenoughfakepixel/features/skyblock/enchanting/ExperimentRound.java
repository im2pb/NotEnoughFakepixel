package com.nef.notenoughfakepixel.features.skyblock.enchanting;

import net.minecraft.block.StainedGlassBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

final class ExperimentRound {
    private static final int ROW_SIZE = 9;
    private static final int ROUND_SLOT = 4;
    private static final Pattern ROUND_NAME = Pattern.compile("\\bRound\\s*[:#]?\\s*([0-9]{1,2})\\b", Pattern.CASE_INSENSITIVE);

    private final Game game;
    private final Cell[] board;
    private final int timerSlot;
    private final List<DyeColor> sequence = new ArrayList<>();
    private final BitSet revealedColors = new BitSet();
    private final BitSet maskedNumbers = new BitSet();
    private EnumSet<DyeColor> activeColors = EnumSet.noneOf(DyeColor.class);
    private List<Integer> numberedSlots = List.of();
    private int highestNumber;
    private int expectedNumbers;
    private int expectedNotes;
    private int next;
    private boolean remembering;
    private boolean guessing;
    private boolean complete;
    private boolean ambiguous;
    private boolean nextColorReady;
    private boolean nextColorLit;

    ExperimentRound(Game game, int slots) {
        this.game = game;
        board = new Cell[slots];
        Arrays.fill(board, Cell.UNKNOWN);
        timerSlot = slots - 5;
    }

    void contentsReceived(List<ItemStack> stacks) {
        if (stacks.size() < board.length) return;
        if (game == Game.CHRONOMATRON) expectedNotes = noteCount(stacks.get(ROUND_SLOT));
        for (int slot = 0; slot < board.length; slot++) board[slot] = Cell.read(stacks.get(slot));
        boardChanged();
    }

    void slotReceived(int slot, ItemStack stack) {
        if (slot < 0 || slot >= board.length) return;
        if (game == Game.CHRONOMATRON && slot == ROUND_SLOT) expectedNotes = noteCount(stack);
        board[slot] = Cell.read(stack);
        boardChanged();
    }

    private void boardChanged() {
        var previousColors = activeColors;
        if (game == Game.CHRONOMATRON) activeColors = litColors();

        if (board[timerSlot].kind() != Tile.CLOCK) {
            if (!remembering) {
                startRemembering();
                // The first playback flash can arrive in a lower slot before the old clock is replaced.
                previousColors = EnumSet.noneOf(DyeColor.class);
            }
            remember(previousColors);
            return;
        }
        if (!guessing) {
            if (remembering) remember(previousColors);
            guessing = true;
            remembering = false;
            complete = game == Game.CHRONOMATRON
                    ? !ambiguous && !sequence.isEmpty() && (expectedNotes == 0 || sequence.size() == expectedNotes)
                    : !numberedSlots.isEmpty()
                            && numberedSlots.size() == (expectedNumbers > 0 ? expectedNumbers : highestNumber);
            if (game == Game.CHRONOMATRON && complete) nextColorReady = !activeColors.contains(sequence.getFirst());
        }
        if (!complete) return;

        if (game == Game.CHRONOMATRON) updateColorGuess();
        else updateNumberGuess();
    }

    private void startRemembering() {
        // Each Ultrasequencer round adds one dye. The old and new dyes can briefly coexist in slot updates.
        expectedNumbers = game == Game.ULTRASEQUENCER && guessing && complete ? numberedSlots.size() + 1 : 0;
        sequence.clear();
        revealedColors.clear();
        maskedNumbers.clear();
        numberedSlots = List.of();
        highestNumber = 0;
        next = 0;
        remembering = true;
        guessing = complete = ambiguous = nextColorReady = nextColorLit = false;
    }

    private void remember(EnumSet<DyeColor> previousColors) {
        if (game == Game.ULTRASEQUENCER) {
            rememberNumbers();
            return;
        }

        for (int slot = 0; slot < board.length; slot++) {
            if (board[slot].kind() == Tile.TERRACOTTA) revealedColors.set(slot);
        }
        // Several cells can light up for the same note, even in separate slot packets.
        var pulses = activeColors.clone();
        pulses.removeAll(previousColors);
        if (pulses.size() > 1) ambiguous = true;
        if (pulses.size() == 1) sequence.add(pulses.iterator().next());
    }

    private void rememberNumbers() {
        int[] ordered = new int[board.length + 1];
        Arrays.fill(ordered, -1);
        int count = 0;
        boolean duplicate = false;
        for (int slot = 0; slot < board.length; slot++) {
            Cell cell = board[slot];
            if (slot == timerSlot || cell.kind() != Tile.DYE) continue;

            count++;
            int number = cell.count();
            highestNumber = Math.max(highestNumber, number);
            if (number < 1 || number >= ordered.length || ordered[number] != -1) {
                duplicate = true;
            } else {
                ordered[number] = slot;
            }
        }
        highestNumber = Math.max(highestNumber, count);
        // Keep the last complete sequence while the server covers the dyes.
        if (duplicate || count == 0 || count < numberedSlots.size()
                || (expectedNumbers > 0 && count != expectedNumbers)) return;

        var result = new ArrayList<Integer>(count);
        for (int number = 1; number <= count; number++) {
            if (ordered[number] == -1) return;
            result.add(ordered[number]);
        }
        numberedSlots = result;
    }

    private void updateColorGuess() {
        if (next >= sequence.size()) return;

        boolean lit = activeColors.contains(sequence.get(next));
        if (!nextColorReady) {
            nextColorReady = !lit;
        } else if (lit) {
            nextColorLit = true;
        } else if (nextColorLit) {
            next++;
            nextColorLit = false;
            nextColorReady = next < sequence.size() && !activeColors.contains(sequence.get(next));
        }
    }

    private void updateNumberGuess() {
        // The clock can arrive before the dyes are covered.
        for (int slot : numberedSlots) {
            Tile tile = board[slot].kind();
            if (tile != Tile.DYE && tile != Tile.UNKNOWN) maskedNumbers.set(slot);
        }
        while (next < numberedSlots.size()) {
            int slot = numberedSlots.get(next);
            if (!maskedNumbers.get(slot) || board[slot].kind() != Tile.DYE) break;
            next++;
        }
    }

    List<Clue> clues() {
        if (!guessing || !complete) return List.of();

        var clues = new ArrayList<Clue>();
        if (game == Game.CHRONOMATRON) {
            if (!canGuessColor()) return List.of();

            for (int slot = revealedColors.nextSetBit(0); slot >= 0; slot = revealedColors.nextSetBit(slot + 1)) {
                if (matchesColor(slot)) clues.add(new Clue(slot, 0, 0));
            }
        } else {
            for (int index = next; index < numberedSlots.size(); index++) {
                int slot = numberedSlots.get(index);
                if (maskedNumbers.get(slot) && board[slot].kind() != Tile.DYE) clues.add(new Clue(slot, index + 1, index - next));
            }
        }
        return clues;
    }

    boolean blocksClick(int slot) {
        if (!guessing || !complete || slot < 0 || slot >= board.length || slot == timerSlot) return false;

        boolean inGrid = slot >= ROW_SIZE && slot < board.length - ROW_SIZE;
        if (game == Game.ULTRASEQUENCER) {
            // Leave menu controls alone, including any in the first or last row.
            if (!inGrid && !numberedSlots.contains(slot)) return false;
            return next < numberedSlots.size() && maskedNumbers.get(numberedSlots.get(next))
                    && slot != numberedSlots.get(next);
        }
        if (canGuessColor() && (inGrid || revealedColors.get(slot))
                && board[slot].kind() == Tile.GLASS && !matchesColor(slot)) {
            for (int choice = revealedColors.nextSetBit(0); choice >= 0; choice = revealedColors.nextSetBit(choice + 1)) {
                if (matchesColor(choice)) return true;
            }
        }
        return false;
    }

    private boolean canGuessColor() {
        return next < sequence.size() && nextColorReady && !nextColorLit;
    }

    private boolean matchesColor(int slot) {
        return board[slot].kind() == Tile.GLASS && board[slot].color() == sequence.get(next);
    }

    private EnumSet<DyeColor> litColors() {
        var result = EnumSet.noneOf(DyeColor.class);
        for (int slot = 0; slot < board.length; slot++) {
            Cell cell = board[slot];
            if (cell.kind() == Tile.TERRACOTTA) result.add(cell.color());
        }
        return result;
    }

    private static int noteCount(ItemStack stack) {
        var round = ROUND_NAME.matcher(Formatting.strip(stack.getName().getString()));
        if (round.find()) return Integer.parseInt(round.group(1));
        return stack.getCount() > 1 ? stack.getCount() : 0;
    }

    enum Game {
        CHRONOMATRON, ULTRASEQUENCER, SUPERPAIRS
    }

    record Clue(int slot, int number, int offset) {
    }

    private enum Tile {
        UNKNOWN, OTHER, CLOCK, DYE, GLASS, TERRACOTTA
    }

    private record Cell(Tile kind, DyeColor color, int count) {
        private static final Cell UNKNOWN = new Cell(Tile.UNKNOWN, null, 0);
        private static final Cell OTHER = new Cell(Tile.OTHER, null, 0);
        private static final Cell CLOCK = new Cell(Tile.CLOCK, null, 0);

        static Cell read(ItemStack stack) {
            if (stack.isEmpty()) return OTHER;
            if (stack.isOf(Items.CLOCK)) return CLOCK;
            if (isNumberedDye(stack)) return new Cell(Tile.DYE, null, stack.getCount());
            if (!(stack.getItem() instanceof BlockItem item)) return OTHER;

            var block = item.getBlock();
            if (block instanceof StainedGlassBlock glass) return new Cell(Tile.GLASS, glass.getColor(), 0);
            if (block instanceof StainedGlassPaneBlock pane) return new Cell(Tile.GLASS, pane.getColor(), 0);
            var id = Registries.BLOCK.getId(block);
            String path = id.getPath();
            String suffix = "_terracotta";
            if (!id.getNamespace().equals("minecraft") || !path.endsWith(suffix)) return OTHER;

            DyeColor color = DyeColor.byName(path.substring(0, path.length() - suffix.length()), null);
            return color == null ? OTHER : new Cell(Tile.TERRACOTTA, color, 0);
        }

        private static boolean isNumberedDye(ItemStack stack) {
            if (stack.getItem() instanceof DyeItem) return true;
            // Some legacy INK_SACK variants arrive as materials on modern clients.
            return (stack.isOf(Items.BONE_MEAL) || stack.isOf(Items.LAPIS_LAZULI))
                    && Integer.toString(stack.getCount()).equals(Formatting.strip(stack.getName().getString()).strip());
        }
    }
}
