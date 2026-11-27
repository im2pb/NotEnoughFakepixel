package com.nef.notenoughfakepixel.features.skyblock.pets;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class PetData {
    private static final Pattern LEVEL = Pattern.compile("\\[Lvl\\s+\\d+]\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_ICONS = Pattern.compile("\\s*[\\p{So}\\p{Sk}\\uFE0F]+$");
    private static final Pattern LORE_RARITY = Pattern.compile(
            "(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE)(?: PET)?");
    private static final List<Formatting> SCORE_COLORS = List.of(Formatting.WHITE, Formatting.GREEN, Formatting.BLUE,
            Formatting.DARK_PURPLE, Formatting.GOLD, Formatting.LIGHT_PURPLE);

    record Owned(String name, int score) {
    }

    record Upgrade(String name, int score, int gain) {
    }

    record Report(int score, List<String> missing, List<Upgrade> upgrades) {
    }

    private PetData() {
    }

    static Owned fromMenu(ItemStack stack) {
        Text display = stack.getName();
        var match = LEVEL.matcher(Formatting.strip(display.getString()).strip());
        if (!match.matches()) return null;
        String species = TRAILING_ICONS.matcher(match.group(1).strip()).replaceFirst("");
        if (species.isEmpty()) return null;

        int score = nameScore(display);
        if (score == 0) {
            var lore = stack.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (Text line : lore.lines().reversed()) {
                    var rarity = LORE_RARITY.matcher(Formatting.strip(line.getString()).strip().toUpperCase(Locale.ROOT));
                    if (rarity.matches()) {
                        score = rarityScore(rarity.group(1));
                        break;
                    }
                }
            }
        }
        return new Owned(species, Math.max(1, score));
    }

    static Report report(Map<String, String> catalog, List<Owned> items) {
        Map<String, Integer> owned = new HashMap<>();
        for (Owned pet : items) owned.merge(pet.name().toLowerCase(Locale.ROOT), pet.score(), Math::max);

        List<String> missing = new ArrayList<>();
        List<Upgrade> upgrades = new ArrayList<>();
        int total = 0;
        for (String species : catalog.keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()) {
            Integer score = owned.get(species.toLowerCase(Locale.ROOT));
            if (score == null) {
                missing.add(species);
                continue;
            }
            total += score;
            int gain = rarityScore(catalog.get(species)) - score;
            if (gain > 0) upgrades.add(new Upgrade(species, score, gain));
        }
        return new Report(total, List.copyOf(missing), List.copyOf(upgrades));
    }

    static Formatting scoreColor(int score) {
        return SCORE_COLORS.get(Math.clamp(score, 1, SCORE_COLORS.size()) - 1);
    }

    private static int nameScore(Text display) {
        boolean[] afterLevel = {false};
        int[] score = {0};
        TextVisitFactory.visitFormatted(display, Style.EMPTY, (index, style, codePoint) -> {
            if (!afterLevel[0]) {
                afterLevel[0] = codePoint == ']';
                return true;
            }
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) return true;
            var color = style.getColor();
            if (color != null) {
                if (color.getRgb() == Formatting.AQUA.getColorValue()) {
                    score[0] = 6;
                } else {
                    for (int i = 0; i < SCORE_COLORS.size(); i++) {
                        if (color.getRgb() == SCORE_COLORS.get(i).getColorValue()) {
                            score[0] = i + 1;
                            break;
                        }
                    }
                }
            }
            return false;
        });
        return score[0];
    }

    private static int rarityScore(String rarity) {
        return switch (rarity.strip().toUpperCase(Locale.ROOT)) {
            case "MYTHIC", "DIVINE" -> 6;
            case "LEGENDARY" -> 5;
            case "EPIC" -> 4;
            case "RARE" -> 3;
            case "UNCOMMON" -> 2;
            default -> 1;
        };
    }
}
