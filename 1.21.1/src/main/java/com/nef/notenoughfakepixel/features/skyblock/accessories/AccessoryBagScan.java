package com.nef.notenoughfakepixel.features.skyblock.accessories;

import com.nef.notenoughfakepixel.serverdata.AccessoriesData;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Accessory;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Rarity;
import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

final class AccessoryBagScan {
    private static final long NAVIGATION_TIMEOUT = 5_000;
    private static final Pattern RARITY_FOOTER = Pattern.compile(
            "\\b(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL)( ACCESSORY\\b)?");

    enum Direction {
        PREVIOUS(-1), NEXT(1), JUMP(0);

        private final int step;

        Direction(int step) {
            this.step = step;
        }
    }

    record Page(boolean previous, boolean next, List<Accessory> accessories) {
        static Page read(ItemStack[] stacks) {
            boolean previous = false, next = false;
            List<Accessory> accessories = new ArrayList<>();
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) continue;
                Direction direction = navigation(stack);
                if (direction != null) {
                    previous |= direction == Direction.PREVIOUS;
                    next |= direction == Direction.NEXT;
                    continue;
                }

                String name = AccessoriesData.clean(stack.getName().getString());
                if (!name.isEmpty() && !SkyblockItemData.id(stack).isEmpty()) {
                    accessories.add(new Accessory(name, rarity(stack)));
                }
            }
            return new Page(previous, next, List.copyOf(accessories));
        }

        private static Rarity rarity(ItemStack stack) {
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (var line : lore.lines().reversed()) {
                    String label = AccessoriesData.clean(line.getString()).toUpperCase(Locale.ROOT);
                    var footer = RARITY_FOOTER.matcher(label);
                    while (footer.find()) {
                        if (footer.group(2) != null || (footer.start() == 0 && footer.end() == label.length())) {
                            return Rarity.valueOf(footer.group(1));
                        }
                    }
                }
            }
            return null;
        }
    }

    private final NavigableMap<Integer, Page> pages = new TreeMap<>();
    private Page current;
    private int index = -1;
    private int lastIndex = -1;
    private Direction pending;
    private long deadline;
    private long revision;

    static Direction navigation(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String label = AccessoriesData.clean(stack.getName().getString()).toLowerCase(Locale.ROOT);
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

    void receive(Page page, boolean replacement) {
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

    List<Accessory> accessories() {
        return pages.values().stream().flatMap(page -> page.accessories().stream()).toList();
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
