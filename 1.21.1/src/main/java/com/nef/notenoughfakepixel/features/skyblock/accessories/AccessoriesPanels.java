package com.nef.notenoughfakepixel.features.skyblock.accessories;

import com.nef.notenoughfakepixel.config.features.Accessories;
import com.nef.notenoughfakepixel.mixin.HandledScreenAccessor;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Accessory;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Estimate;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

final class AccessoriesPanels {
    private static final int GAP = 6;
    private static final int PADDING = 5;
    private static final int WIDTH = 140;
    private static final int MIN_WIDTH = 35;
    private static final int BACKGROUND = 0xDD141414;
    private static final int BORDER = 0xFF545454;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private Rect listBounds;
    private int offset;
    private int maxOffset;
    private int step;

    void draw(DrawContext context, TextRenderer font, GenericContainerScreen screen, Accessories settings,
              Estimate estimate, String status, int mouseX, int mouseY) {
        var menu = (HandledScreenAccessor) screen;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int right = menu.nef$left() + menu.nef$backgroundWidth() + GAP;
        int rightWidth = Math.min(WIDTH, screenWidth - right - GAP);
        int leftWidth = Math.min(WIDTH, menu.nef$left() - GAP * 2);
        int top = Math.max(GAP, menu.nef$top());

        if (settings.showMpEstimation && leftWidth >= MIN_WIDTH) {
            drawEstimation(context, font, menu.nef$left() - GAP - leftWidth, top, leftWidth, estimate, status);
        }
        if (settings.showMissingAccessoriesList && rightWidth >= MIN_WIDTH) {
            int height = Math.min(Math.min(200, menu.nef$backgroundHeight()), screenHeight - top - GAP);
            if (height > 2 * (font.fontHeight + PADDING)) {
                drawList(context, font, new Rect(right, top, rightWidth, height), estimate, status, mouseX, mouseY);
            }
        }
    }

    boolean scrollAt(double x, double y, double vertical) {
        return vertical != 0 && listBounds != null && listBounds.contains(x, y) && scroll(vertical > 0 ? -1 : 1);
    }

    boolean scroll(int direction) {
        if (listBounds == null || maxOffset <= 0) return false;
        offset = Math.clamp(offset + direction * step, 0, maxOffset);
        return true;
    }

    void clearBounds() {
        listBounds = null;
    }

    void reset() {
        clearBounds();
        offset = 0;
        maxOffset = 0;
    }

    private void drawList(DrawContext context, TextRenderer font, Rect panel, Estimate estimate, String status,
                          int mouseX, int mouseY) {
        background(context, panel);
        String title = font.getWidth("Missing Accessories") <= panel.width() - 2 * PADDING
                ? "Missing Accessories" : "Missing";
        int textWidth = panel.width() - 2 * PADDING;
        context.drawTextWithShadow(font, font.trimToWidth(title, textWidth), panel.x() + PADDING, panel.y() + PADDING, WHITE);

        int header = panel.y() + font.fontHeight + 2 * PADDING;
        context.fill(panel.x() + PADDING, header - 1, panel.right() - PADDING, header, BORDER);
        if (estimate == null || estimate.missing().isEmpty()) {
            maxOffset = 0;
            String message = estimate == null ? status : "No missing accessories.";
            drawWrapped(context, font, Text.literal(message), panel.x() + PADDING, header + PADDING, textWidth);
            return;
        }

        var view = new Rect(panel.x() + PADDING, header + 2, panel.width() - 2 * PADDING, panel.bottom() - header - PADDING - 2);
        listBounds = panel;
        step = font.fontHeight + 2;
        List<Accessory> missing = estimate.missing();
        int contentHeight = missing.size() * step;
        maxOffset = Math.max(0, contentHeight - view.height());
        offset = Math.clamp(offset, 0, maxOffset);
        int rowWidth = view.width() - (maxOffset > 0 ? 5 : 0);
        Accessory hovered = null;

        context.enableScissor(view.x(), view.y(), view.right(), view.bottom());
        try {
            for (int index = offset / step; index < missing.size(); index++) {
                int y = view.y() + index * step - offset;
                if (y >= view.bottom()) break;
                Accessory accessory = missing.get(index);
                boolean clipped = font.getWidth(accessory.name()) > rowWidth;
                String label = clipped ? font.trimToWidth(accessory.name(), Math.max(0, rowWidth - font.getWidth("…"))) + "…"
                        : accessory.name();
                int color = 0xFF000000 | accessory.rarity().color().getColorValue();
                context.drawTextWithShadow(font, label, view.x(), y, color);
                if (clipped && view.contains(mouseX, mouseY) && mouseX < view.x() + rowWidth
                        && mouseY >= y && mouseY < y + step) hovered = accessory;
            }
        } finally {
            context.disableScissor();
        }

        if (maxOffset > 0) drawScrollbar(context, view, contentHeight);
        if (hovered != null) {
            context.drawTooltip(font, Text.literal(hovered.name()).formatted(hovered.rarity().color()), mouseX, mouseY);
        }
    }

    private void drawScrollbar(DrawContext context, Rect view, int contentHeight) {
        int x = view.right() - 2;
        int thumbHeight = Math.clamp(view.height() * view.height() / contentHeight, Math.min(12, view.height()), view.height());
        int thumbY = view.y() + (view.height() - thumbHeight) * offset / maxOffset;
        context.fill(x, view.y(), x + 2, view.bottom(), BORDER);
        context.fill(x, thumbY, x + 2, thumbY + thumbHeight, GRAY);
    }

    private static void drawEstimation(DrawContext context, TextRenderer font, int x, int y, int width,
                                       Estimate estimate, String status) {
        int textWidth = width - 2 * PADDING;
        Text title = Text.literal(font.getWidth("MP Estimation") <= textWidth ? "MP Estimation" : "MP").formatted(Formatting.GOLD);
        var lines = estimate == null
                ? List.of(title, Text.literal(status).formatted(Formatting.GRAY))
                : List.of(title, mpLine("MP", estimate.currentMp(), estimate.maxMp()),
                        mpLine("Recomb", estimate.currentMp(), estimate.recombobulatedMp()));
        int lineHeight = font.fontHeight + 2;
        int height = lines.stream().mapToInt(line -> font.wrapLines(line, textWidth).size() * lineHeight).sum() + 2 * PADDING;
        background(context, new Rect(x, y, width, height));

        int textY = y + PADDING;
        for (Text text : lines) textY = drawWrapped(context, font, text, x + PADDING, textY, textWidth);
    }

    private static Text mpLine(String label, long current, long max) {
        Formatting color = current >= max ? Formatting.AQUA : current * 3 >= max * 2 ? Formatting.GREEN
                : current * 3 >= max ? Formatting.YELLOW : Formatting.RED;
        return Text.literal(label + " ").formatted(Formatting.GRAY)
                .append(Text.literal(Long.toString(current)).formatted(color))
                .append(Text.literal(" / ").formatted(Formatting.GRAY))
                .append(Text.literal(Long.toString(max)).formatted(Formatting.AQUA));
    }

    private static int drawWrapped(DrawContext context, TextRenderer font, Text text, int x, int y, int width) {
        for (var line : font.wrapLines(text, width)) {
            context.drawTextWithShadow(font, line, x, y, GRAY);
            y += font.fontHeight + 2;
        }
        return y;
    }

    private static void background(DrawContext context, Rect area) {
        context.fill(area.x(), area.y(), area.right(), area.bottom(), BACKGROUND);
        context.drawBorder(area.x(), area.y(), area.width(), area.height(), BORDER);
    }

    private record Rect(int x, int y, int width, int height) {
        int right() {
            return x + width;
        }

        int bottom() {
            return y + height;
        }

        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
