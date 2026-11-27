package com.nef.notenoughfakepixel.features.skyblock.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;

final class PagedMenuScan<T> {
    private static final long NAVIGATION_TIMEOUT = 5_000;

    enum Direction {
        PREVIOUS(-1), NEXT(1), JUMP(0);

        private final int step;

        Direction(int step) {
            this.step = step;
        }
    }

    record Page<T>(boolean previous, boolean next, List<T> items) {
        static <T> Page<T> read(ItemStack[] stacks, Function<ItemStack, T> parser) {
            boolean previous = false, next = false;
            List<T> items = new ArrayList<>();
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) continue;
                Direction direction = navigation(stack);
                if (direction != null) {
                    previous |= direction == Direction.PREVIOUS;
                    next |= direction == Direction.NEXT;
                    continue;
                }

                T item = parser.apply(stack);
                if (item != null) items.add(item);
            }
            return new Page<>(previous, next, List.copyOf(items));
        }
    }

    private final NavigableMap<Integer, Page<T>> pages = new TreeMap<>();
    private Page<T> current;
    private int index = -1;
    private int lastIndex = -1;
    private Direction pending;
    private long deadline;
    private long revision;

    static Direction navigation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String label = Formatting.strip(stack.getName().getString()).strip().toLowerCase(Locale.ROOT);
        return switch (label) {
            case "next page", "next page →", "next page »" -> Direction.NEXT;
            case "previous page", "← previous page", "« previous page" -> Direction.PREVIOUS;
            case "first page", "« first page", "last page", "last page »" -> Direction.JUMP;
            default -> null;
        };
    }

    void navigate(Direction direction, long now) {
        if (current == null) return;
        if (pending != null || direction == Direction.JUMP) clearPages();
        pending = direction;
        deadline = now + NAVIGATION_TIMEOUT;
    }

    void expireNavigation(long now) {
        if (pending != null && now >= deadline) {
            clearPages();
            current = null;
            pending = null;
        }
    }

    boolean awaitingPage() {
        return pending != null;
    }

    void receive(Page<T> page, boolean replacement) {
        if (pending != null) {
            if (!replacement && page.equals(current)) return;
            int target = index < 0 ? -1 : index + pending.step;
            if (target >= 0 && page.previous() != (target > 0)) return;
            index = target;
            pending = null;
        } else if (replacement || (current != null
                && (page.previous() != current.previous() || page.next() != current.next()))) {
            clearPages();
        }

        current = page;
        if (index < 0 && !page.previous()) index = 0;
        if (index < 0) return;

        if (!page.next()) {
            pages.tailMap(index, false).clear();
            lastIndex = index;
        } else if (index == lastIndex) {
            lastIndex = -1;
        }
        if (!page.equals(pages.put(index, page))) revision++;
    }

    long revision() {
        return revision;
    }

    boolean complete() {
        return pending == null && lastIndex >= 0 && pages.size() == lastIndex + 1;
    }

    List<T> items() {
        return pages.values().stream().flatMap(page -> page.items().stream()).toList();
    }

    void clear() {
        clearPages();
        current = null;
        pending = null;
    }

    private void clearPages() {
        pages.clear();
        index = -1;
        lastIndex = -1;
        revision++;
    }
}
