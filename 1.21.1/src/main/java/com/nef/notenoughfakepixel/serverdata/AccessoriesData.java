package com.nef.notenoughfakepixel.serverdata;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class AccessoriesData {
    private static final Pattern SPACES = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Comparator<Accessory> DISPLAY_ORDER = Comparator
            .comparing(Accessory::rarity).reversed()
            .thenComparing(Accessory::name, String.CASE_INSENSITIVE_ORDER);

    private AccessoriesData() {
    }

    public enum Rarity {
        COMMON(3, 5, Formatting.WHITE),
        UNCOMMON(5, 8, Formatting.GREEN),
        RARE(8, 12, Formatting.BLUE),
        EPIC(12, 16, Formatting.DARK_PURPLE),
        LEGENDARY(16, 22, Formatting.GOLD),
        MYTHIC(22, 28, Formatting.LIGHT_PURPLE),
        DIVINE(28, 28, Formatting.AQUA),
        SPECIAL(3, 5, Formatting.RED);

        private final int mp;
        private final int recombobulatedMp;
        private final Formatting color;

        Rarity(int mp, int recombobulatedMp, Formatting color) {
            this.mp = mp;
            this.recombobulatedMp = recombobulatedMp;
            this.color = color;
        }

        public Formatting color() {
            return color;
        }
    }

    public record Accessory(String name, Rarity rarity) {
    }

    public record Catalog(List<Accessory> accessories, int bonuses) {
        public Catalog {
            accessories = List.copyOf(accessories);
        }
    }

    public record Estimate(List<Accessory> missing, long currentMp, long maxMp, long recombobulatedMp) {
    }

    public static Catalog parse(String json) {
        var document = JsonParser.parseString(json);
        if (!document.isJsonObject()) throw new JsonParseException("Expected an accessory catalog object");

        var root = document.getAsJsonObject();
        var rows = root.get("accessories");
        var bonus = root.get("bonuses");
        if (rows == null || !rows.isJsonArray() || rows.getAsJsonArray().isEmpty()
                || bonus == null || !bonus.isJsonPrimitive() || !bonus.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("Expected accessories and their MP bonus");
        }

        Map<String, Accessory> accessories = new LinkedHashMap<>();
        try {
            for (JsonElement row : rows.getAsJsonArray()) {
                if (!row.isJsonObject()) throw new JsonParseException("Expected an accessory row");
                var entry = row.getAsJsonObject();
                String name = clean(string(entry.get("name")));
                Rarity rarity = Rarity.valueOf(string(entry.get("rarity")).strip().toUpperCase(Locale.ROOT));
                if (name.isEmpty()) throw new JsonParseException("Empty accessory name");
                accessories.merge(key(name), new Accessory(name, rarity), AccessoriesData::higherMp);
            }

            int bonuses = bonus.getAsBigDecimal().intValueExact();
            if (bonuses < 0) throw new JsonParseException("Negative MP bonus");
            return new Catalog(List.copyOf(accessories.values()), bonuses);
        } catch (ArithmeticException | IllegalArgumentException exception) {
            throw new JsonParseException("Invalid accessory rarity or MP bonus", exception);
        }
    }

    public static Estimate estimate(Catalog catalog, Iterable<Accessory> bag) {
        Map<String, Accessory> catalogByName = new HashMap<>();
        long max = catalog.bonuses(), recombobulated = catalog.bonuses();
        for (Accessory accessory : catalog.accessories()) {
            catalogByName.put(key(accessory.name()), accessory);
            max += accessory.rarity().mp;
            recombobulated += accessory.rarity().recombobulatedMp;
        }

        Map<String, Accessory> owned = new HashMap<>();
        for (Accessory accessory : bag) {
            String key = key(accessory.name());
            Accessory identified = accessory.rarity() == null ? catalogByName.get(key) : accessory;
            if (identified != null) owned.merge(key, identified, AccessoriesData::higherMp);
        }

        List<Accessory> missing = catalog.accessories().stream().filter(item -> !owned.containsKey(key(item.name())))
                .sorted(DISPLAY_ORDER).toList();
        long current = owned.values().stream().mapToLong(item -> item.rarity().mp).sum();
        return new Estimate(missing, current, max, recombobulated);
    }

    public static String clean(String text) {
        return SPACES.matcher(Formatting.strip(text)).replaceAll(" ").strip();
    }

    private static String key(String name) {
        String normalized = clean(name).toLowerCase(Locale.ROOT);
        return normalized.contains("abicase") ? "abicase" : normalized;
    }

    private static Accessory higherMp(Accessory first, Accessory second) {
        return second.rarity().mp > first.rarity().mp ? second : first;
    }

    private static String string(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new JsonParseException("Expected accessory text");
        }
        return element.getAsString();
    }
}
