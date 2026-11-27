package com.nef.notenoughfakepixel.features.skyblock.chocolate;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight.HightlightSlot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.regex.Pattern;

@RegisterEvents
public final class BestChocolateUpgrade extends HightlightSlot {
    private static final int FIRST_RABBIT = 28;
    private static final int LAST_RABBIT = 34;
    private static final Pattern PRICE = Pattern.compile("(?:Cost:\\s*)?([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s+Chocolate",
            Pattern.UNICODE_CHARACTER_CLASS);

    public BestChocolateUpgrade() {
        super("Chocolate Factory", "");
    }

    @Override
    protected boolean enabled(Configuration config) {
        return config.chocolateFactory.chocolateChocolateShowBestUpgrade;
    }

    @Override
    protected boolean onlyInSkyblock() {
        return false;
    }

    @Override
    protected int color(Configuration config) {
        return 0x6400FF00;
    }

    @Override
    protected void renderSlots(DrawContext context, GenericContainerScreenHandler handler, int x, int y, int color) {
        Slot best = bestSlot(handler);
        if (best != null) drawSlot(context, best, x, y, color);
    }

    static Slot bestSlot(GenericContainerScreenHandler handler) {
        Slot best = null;
        double bestRatio = Double.POSITIVE_INFINITY;
        int end = Math.min(handler.getInventory().size(), LAST_RABBIT + 1);
        for (int index = FIRST_RABBIT; index < end; index++) {
            Slot slot = handler.getSlot(index);
            if (!slot.isEnabled()) continue;
            long cost = price(slot.getStack());
            if (cost < 0) continue;

            // Successive rabbits add 1 through 7 chocolate per second for each level.
            double ratio = (double) cost / (index - FIRST_RABBIT + 1);
            if (ratio <= bestRatio) {
                bestRatio = ratio;
                best = slot;
            }
        }
        return best;
    }

    private static long price(ItemStack stack) {
        if (!stack.isOf(Items.PLAYER_HEAD)) return -1;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return -1;

        for (Text row : lore.lines()) {
            var price = PRICE.matcher(plain(row).strip());
            if (!price.matches()) continue;
            try {
                return Long.parseLong(price.group(1).replace(",", ""));
            } catch (NumberFormatException ignored) {
                // Skip prices outside the supported range.
            }
        }
        return -1;
    }
}
