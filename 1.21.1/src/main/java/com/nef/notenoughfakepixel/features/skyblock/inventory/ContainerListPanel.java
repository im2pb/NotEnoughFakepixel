package com.nef.notenoughfakepixel.features.skyblock.inventory;

import com.nef.notenoughfakepixel.mixin.HandledScreenAccessor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

import java.util.List;

public final class ContainerListPanel {
    public static final int PADDING = 5;
    private static final int GAP = 6;
    private static final int WIDTH = 140;
    private static final int MIN_WIDTH = 35;
    private static final int BACKGROUND = 0xDD141414;
    private static final int BORDER = 0xFF545454;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private Bounds bounds;
    private int offset;
    private int maxOffset;
    private int step;

    public static Bounds beside(DrawContext context, GenericContainerScreen screen, boolean rightSide) {
        var menu = (HandledScreenAccessor) screen;
        int edge = menu.nef$left() + menu.nef$backgroundWidth() + GAP;
        int width = Math.min(WIDTH, rightSide ? context.getScaledWindowWidth() - edge - GAP : menu.nef$left() - GAP * 2);
        int top = Math.max(GAP, menu.nef$top());
        int height = Math.min(Math.min(200, menu.nef$backgroundHeight()), context.getScaledWindowHeight() - top - GAP);
        return width < MIN_WIDTH || height <= 0 ? null
                : new Bounds(rightSide ? edge : menu.nef$left() - GAP - width, top, width, height);
    }

    public void draw(DrawContext context, TextRenderer font, Bounds panel, Text title, Text compactTitle,
                     List<Text> rows, String status, int mouseX, int mouseY) {
        clearBounds();
        step = font.fontHeight + 2;
        if (panel == null || panel.height() <= 2 * (font.fontHeight + PADDING)) return;
        background(context, panel);

        int textWidth = panel.width() - 2 * PADDING;
        Text heading = font.getWidth(title) <= textWidth ? title : compactTitle;
        drawClipped(context, font, heading, panel.x() + PADDING, panel.y() + PADDING, textWidth);
        int header = panel.y() + font.fontHeight + 2 * PADDING;
        context.fill(panel.x() + PADDING, header - 1, panel.right() - PADDING, header, BORDER);
        var view = new Bounds(panel.x() + PADDING, header + 2, textWidth, panel.bottom() - header - PADDING - 2);

        context.enableScissor(view.x(), view.y(), view.right(), view.bottom());
        Text hovered = null;
        try {
            if (rows.isEmpty()) {
                maxOffset = 0;
                drawWrapped(context, font, Text.literal(status), view.x(), view.y() + 2, view.width());
            } else {
                bounds = panel;
                int contentHeight = rows.size() * step;
                maxOffset = Math.max(0, contentHeight - view.height());
                offset = Math.clamp(offset, 0, maxOffset);
                int rowWidth = view.width() - (maxOffset > 0 ? 5 : 0);
                for (int index = offset / step; index < rows.size(); index++) {
                    int y = view.y() + index * step - offset;
                    if (y >= view.bottom()) break;
                    Text row = rows.get(index);
                    boolean clipped = drawClipped(context, font, row, view.x(), y, rowWidth);
                    if (clipped && view.contains(mouseX, mouseY) && mouseX < view.x() + rowWidth
                            && mouseY >= y && mouseY < y + step) hovered = row;
                }
                if (maxOffset > 0) drawScrollbar(context, view, contentHeight);
            }
        } finally {
            context.disableScissor();
        }
        if (hovered != null) context.drawTooltip(font, hovered, mouseX, mouseY);
    }

    public boolean scrollAt(double x, double y, double vertical) {
        return vertical != 0 && bounds != null && bounds.contains(x, y) && scroll(vertical > 0 ? -1 : 1);
    }

    public boolean scroll(int direction) {
        if (bounds == null || maxOffset <= 0) return false;
        offset = Math.clamp(offset + direction * step, 0, maxOffset);
        return true;
    }

    public void clearBounds() {
        bounds = null;
    }

    public void reset() {
        clearBounds();
        offset = 0;
        maxOffset = 0;
    }

    private static boolean drawClipped(DrawContext context, TextRenderer font, Text text, int x, int y, int width) {
        if (font.getWidth(text) <= width) {
            context.drawTextWithShadow(font, text, x, y, WHITE);
            return false;
        }
        String ellipsis = "…";
        var clipped = font.trimToWidth(text, Math.max(0, width - font.getWidth(ellipsis)));
        int end = context.drawTextWithShadow(font, Language.getInstance().reorder(clipped), x, y, WHITE);
        context.drawTextWithShadow(font, ellipsis, end, y, GRAY);
        return true;
    }

    private void drawScrollbar(DrawContext context, Bounds view, int contentHeight) {
        int x = view.right() - 2;
        int thumbHeight = Math.clamp(view.height() * view.height() / contentHeight, Math.min(12, view.height()), view.height());
        int thumbY = view.y() + (view.height() - thumbHeight) * offset / maxOffset;
        context.fill(x, view.y(), x + 2, view.bottom(), BORDER);
        context.fill(x, thumbY, x + 2, thumbY + thumbHeight, GRAY);
    }

    public static int drawWrapped(DrawContext context, TextRenderer font, Text text, int x, int y, int width) {
        for (var line : font.wrapLines(text, width)) {
            context.drawTextWithShadow(font, line, x, y, GRAY);
            y += font.fontHeight + 2;
        }
        return y;
    }

    public static void background(DrawContext context, Bounds area) {
        context.fill(area.x(), area.y(), area.right(), area.bottom(), BACKGROUND);
        context.drawBorder(area.x(), area.y(), area.width(), area.height(), BORDER);
    }

    public record Bounds(int x, int y, int width, int height) {
        private int right() { return x + width; }
        private int bottom() { return y + height; }

        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < right() && mouseY >= y && mouseY < bottom();
        }
    }
}
