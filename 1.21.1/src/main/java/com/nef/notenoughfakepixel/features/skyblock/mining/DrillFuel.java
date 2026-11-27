package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.math.BigDecimal;
import java.util.regex.Pattern;

record DrillFuel(int amount, int capacity) {
    private static final String PREFIX = "^(?:Drill\\s+)?Fuel\\s*:\\s*";
    private static final String QUANTITY = "((?:[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]+)?)\\s*([km]?)";
    private static final Pattern FUEL_PREFIX = Pattern.compile(PREFIX, Pattern.CASE_INSENSITIVE);
    private static final Pattern FUEL_VALUE = Pattern.compile(
            PREFIX + QUANTITY + "\\s*/\\s*" + QUANTITY + "(?![\\p{L}\\d,.])", Pattern.CASE_INSENSITIVE);

    static DrillFuel read(ItemStack stack) {
        if (stack.isEmpty() || !isDrill(stack)) return null;
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return null;

        for (Text line : lore.lines()) {
            var match = FUEL_VALUE.matcher(plain(line));
            if (!match.find()) continue;
            try {
                int amount = units(match.group(1), match.group(2));
                int capacity = units(match.group(3), match.group(4));
                if (capacity > 0) return new DrillFuel(amount, capacity);
            } catch (ArithmeticException ignored) {
                // Fractional units and values outside the integer range cannot be displayed.
            }
        }
        return null;
    }

    static boolean isDrill(ItemStack stack) {
        String id = SkyblockItemData.id(stack);
        if (!id.isEmpty()) return id.endsWith("_DRILL") || id.contains("_DRILL_");
        var lore = stack.get(DataComponentTypes.LORE);
        return stack.isOf(Items.PRISMARINE_SHARD) && lore != null && lore.lines().stream().anyMatch(DrillFuel::isFuelLine);
    }

    static boolean isFuelLine(Text line) {
        return FUEL_PREFIX.matcher(plain(line)).find();
    }

    String line() {
        Formatting color = amount > capacity * 0.5 ? Formatting.GREEN
                : amount > capacity * 0.25 ? Formatting.YELLOW : Formatting.RED;
        return Formatting.GRAY + "Drill Fuel: " + color + amount + Formatting.GRAY + "/" + capacity;
    }

    private static String plain(Text line) {
        return Formatting.strip(line.getString()).strip();
    }

    private static int units(String quantity, String suffix) {
        int places = suffix.equalsIgnoreCase("k") ? 3 : suffix.equalsIgnoreCase("m") ? 6 : 0;
        return new BigDecimal(quantity.replace(",", "")).movePointRight(places).intValueExact();
    }
}
