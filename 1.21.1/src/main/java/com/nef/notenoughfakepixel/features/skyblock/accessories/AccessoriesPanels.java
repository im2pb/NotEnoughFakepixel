package com.nef.notenoughfakepixel.features.skyblock.accessories;

import com.nef.notenoughfakepixel.config.features.Accessories;
import com.nef.notenoughfakepixel.features.skyblock.inventory.ContainerListPanel;
import com.nef.notenoughfakepixel.features.skyblock.inventory.ContainerListPanel.Bounds;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Estimate;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

final class AccessoriesPanels {
    private static final Text TITLE = Text.literal("Missing Accessories");
    private static final Text COMPACT_TITLE = Text.literal("Missing");
    final ContainerListPanel missing = new ContainerListPanel();
    private Estimate displayed;
    private List<Text> missingRows = List.of();

    void draw(DrawContext context, TextRenderer font, GenericContainerScreen screen, Accessories settings,
              Estimate estimate, String status, int mouseX, int mouseY) {
        if (settings.showMpEstimation) {
            var left = ContainerListPanel.beside(context, screen, false);
            if (left != null) drawEstimation(context, font, left, estimate, status);
        }
        if (settings.showMissingAccessoriesList) {
            if (displayed != estimate) {
                displayed = estimate;
                missingRows = estimate == null ? List.of() : estimate.missing().stream()
                        .<Text>map(accessory -> Text.literal(accessory.name()).formatted(accessory.rarity().color()))
                        .toList();
            }
            missing.draw(context, font, ContainerListPanel.beside(context, screen, true), TITLE, COMPACT_TITLE,
                    missingRows, estimate == null ? status : "No missing accessories.", mouseX, mouseY);
        }
    }

    private static void drawEstimation(DrawContext context, TextRenderer font, Bounds panel, Estimate estimate, String status) {
        int pad = ContainerListPanel.PADDING;
        int textWidth = panel.width() - 2 * pad;
        Text title = Text.literal(font.getWidth("MP Estimation") <= textWidth ? "MP Estimation" : "MP").formatted(Formatting.GOLD);
        var lines = estimate == null
                ? List.of(title, Text.literal(status).formatted(Formatting.GRAY))
                : List.of(title, mpLine("MP", estimate.currentMp(), estimate.maxMp()),
                        mpLine("Recomb", estimate.currentMp(), estimate.recombobulatedMp()));
        int lineHeight = font.fontHeight + 2;
        int height = lines.stream().mapToInt(line -> font.wrapLines(line, textWidth).size() * lineHeight).sum() + 2 * pad;
        ContainerListPanel.background(context, new Bounds(panel.x(), panel.y(), panel.width(), height));

        int textY = panel.y() + pad;
        for (Text text : lines) textY = ContainerListPanel.drawWrapped(context, font, text, panel.x() + pad, textY, textWidth);
    }

    private static Text mpLine(String label, long current, long max) {
        Formatting color = current >= max ? Formatting.AQUA : current * 3 >= max * 2 ? Formatting.GREEN
                : current * 3 >= max ? Formatting.YELLOW : Formatting.RED;
        return Text.literal(label + " ").formatted(Formatting.GRAY)
                .append(Text.literal(Long.toString(current)).formatted(color))
                .append(Text.literal(" / ").formatted(Formatting.GRAY))
                .append(Text.literal(Long.toString(max)).formatted(Formatting.AQUA));
    }
}
