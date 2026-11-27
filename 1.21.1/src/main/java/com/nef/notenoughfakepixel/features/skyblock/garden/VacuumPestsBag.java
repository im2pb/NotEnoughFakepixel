package com.nef.notenoughfakepixel.features.skyblock.garden;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VacuumPestsBag {
    private static final Set<String> VACUUM_IDS = Set.of(
            "SKYMART_VACUUM",
            "SKYMART_TURBO_VACUUM",
            "INFINI_VACUUM_HOOVERIUS",
            "INFINI_VACUUM",
            "SKYMART_HYPER_VACUUM"
    );
    private static final Pattern VACUUM_BAG_LINE = Pattern.compile(
            "^\\s*Vacuum Bag:\\s*([0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)\\s+Pests\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
    private static final ConfigColor COUNT_COLOR = new ConfigColor();

    private VacuumPestsBag() {
    }

    public static int count(ItemStack stack) {
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null || !VACUUM_IDS.contains(SkyblockItemData.id(stack))
                || !ClientConfig.configuration().garden.vacuumPestsBag.enabled) return -1;

        for (Text line : lore.lines()) {
            int count = parseCount(line.getString());
            if (count >= 0) return count;
        }
        return -1;
    }

    static int parseCount(String line) {
        Matcher matcher = VACUUM_BAG_LINE.matcher(Formatting.strip(line));
        if (!matcher.matches()) return -1;
        try {
            return Integer.parseInt(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException error) {
            return -1;
        }
    }

    public static void drawCount(DrawContext context, TextRenderer font, int x, int y, int count) {
        String text = Integer.toString(count);
        int color = COUNT_COLOR.resolve(ClientConfig.configuration().garden.vacuumPestsBag.color) | 0xFF000000;
        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(0, 0, 200);
            context.drawTextWithShadow(font, text, x + 17 - font.getWidth(text), y + 9, color);
        } finally {
            matrices.pop();
        }
    }
}
