package com.nef.notenoughfakepixel.features.skyblock.enchanting;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

final class SuperpairsBoard {
    private final Card[] cards;
    private List<Hint> hints = List.of();
    private int first = -1;
    private int second = -1;
    private boolean instantFind;
    private int instantReveal = -1;
    private boolean finished;

    SuperpairsBoard(int size) {
        cards = new Card[size];
    }

    void contentsReceived(List<ItemStack> contents) {
        if (contents.size() < cards.length || finished) return;
        var revealed = new ArrayList<Integer>();
        boolean changed = false;
        for (int slot = 0; slot < cards.length; slot++) {
            changed |= update(slot, contents.get(slot), revealed);
        }
        if (changed) updateHints(revealed);
    }

    void slotReceived(int slot, ItemStack stack) {
        if (slot < 0 || slot >= cards.length || finished) return;
        var revealed = new ArrayList<Integer>(1);
        if (update(slot, stack, revealed)) updateHints(revealed);
    }

    List<Hint> hints() {
        return hints;
    }

    ItemStack rememberedStack(int slot, ItemStack current) {
        if (finished || slot < 0 || slot >= cards.length || !cover(current)) return current;
        Card card = cards[slot];
        return card != null && card.covered && card.reward != null ? card.reward.stack() : current;
    }

    private boolean update(int slot, ItemStack stack, List<Integer> revealed) {
        Card card = cards[slot];
        if (cover(stack)) {
            if (card == null) {
                cards[slot] = new Card();
            } else if (!card.covered) {
                card.covered = true;
                card.matched = false;
                return true;
            }
            return false;
        }
        if (card == null || stack.isEmpty()) return false;

        var lore = stack.get(DataComponentTypes.LORE);
        List<String> lines = lore == null ? List.of() : lore.lines().stream().map(SuperpairsBoard::plain).toList();
        if (lines.stream().anyMatch(line -> line.equalsIgnoreCase("Click any to proceed!"))) {
            finished = true;
            return true;
        }
        if (!card.covered) return false;

        boolean findsPair = lines.stream().anyMatch(line -> line.equalsIgnoreCase("Powerup for next click!"));
        boolean powerup = findsPair || lines.stream().anyMatch(line -> line.equalsIgnoreCase("Instant Powerup!"));
        var identity = new Identity(stack.getItem(), stack.getCount(), stack.getDamage(), plain(stack.getName()), lines);
        card.reward = new Reward(stack.copy(), identity, powerup, findsPair);
        card.covered = false;
        revealed.add(slot);
        return true;
    }

    private void updateHints(List<Integer> revealed) {
        if (finished) {
            hints = List.of();
            return;
        }
        if ((first >= 0 && cards[first].covered) || (second >= 0 && cards[second].covered)) {
            if (sameReward(first, second)) {
                cards[first].rejected.set(second);
                cards[second].rejected.set(first);
            }
            first = second = -1;
        }
        for (int slot : revealed) observeReveal(slot);

        var updated = new ArrayList<Hint>();
        for (int slot = 0; slot < cards.length; slot++) {
            Card card = cards[slot];
            if (card == null || card.reward == null) continue;

            if (card.reward.powerup()) updated.add(new Hint(slot, Kind.POWERUP));
            else if (card.matched) updated.add(new Hint(slot, Kind.MATCHED));
            else if (hasPartner(slot)) updated.add(new Hint(slot, Kind.POSSIBLE));
            else if (card.covered) updated.add(new Hint(slot, Kind.UNMATCHED));
        }
        hints = updated;
    }

    private void observeReveal(int slot) {
        // The server covers mismatches before accepting another flip.
        if (second >= 0) {
            confirm(first, second);
            first = second = -1;
        }
        Reward reward = cards[slot].reward;
        if (reward.powerup()) {
            if (reward.findsPair()) instantFind = true;
            return;
        }
        if (instantFind) {
            if (instantReveal < 0) {
                instantReveal = slot;
                return;
            }
            instantFind = false;
            int earlier = instantReveal;
            instantReveal = -1;
            if (confirm(earlier, slot)) return;
            if (confirm(first, earlier)) first = -1;
        }
        if (first < 0) first = slot;
        else second = slot;
    }

    private boolean confirm(int a, int b) {
        if (!sameReward(a, b) || cards[a].covered || cards[b].covered || cards[a].rejected.get(b)) return false;
        cards[a].matched = cards[b].matched = true;
        return true;
    }

    private boolean hasPartner(int slot) {
        Card card = cards[slot];
        for (int other = 0; other < cards.length; other++) {
            Card partner = cards[other];
            if (slot != other && partner != null && !partner.matched && !card.rejected.get(other)
                    && sameReward(slot, other)) return true;
        }
        return false;
    }

    private boolean sameReward(int a, int b) {
        return a >= 0 && b >= 0 && a != b && cards[a].reward != null && cards[b].reward != null
                && !cards[a].reward.powerup() && !cards[b].reward.powerup()
                && cards[a].reward.identity().equals(cards[b].reward.identity());
    }

    static boolean cover(ItemStack stack) {
        if (!stack.isOf(Items.LIGHT_GRAY_STAINED_GLASS)) return false;
        String name = plain(stack.getName());
        return name.isEmpty() || name.equals("?");
    }

    private static String plain(Text text) {
        return Formatting.strip(text.getString()).strip();
    }

    enum Kind { MATCHED, POSSIBLE, UNMATCHED, POWERUP }

    record Hint(int slot, Kind kind) {}

    private record Identity(Item item, int count, int damage, String name, List<String> lore) {}

    private record Reward(ItemStack stack, Identity identity, boolean powerup, boolean findsPair) {}

    private static final class Card {
        private final BitSet rejected = new BitSet();
        private Reward reward;
        private boolean covered = true;
        private boolean matched;
    }
}
