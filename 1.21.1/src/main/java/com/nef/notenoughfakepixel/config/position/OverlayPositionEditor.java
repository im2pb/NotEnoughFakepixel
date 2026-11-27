package com.nef.notenoughfakepixel.config.position;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.OverlayLayout.Bounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class OverlayPositionEditor extends Screen {
    private final Screen parent;
    private final Position original;
    private final OverlayLayout layout;
    private final BiConsumer<DrawContext, Bounds> drawPreview;
    private final Consumer<Position> updatePosition;
    private Position position;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public static void open(String title, Position position, OverlayLayout layout,
                            BiConsumer<DrawContext, Bounds> drawPreview, Consumer<Position> updatePosition) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new OverlayPositionEditor(client.currentScreen, title, position, layout, drawPreview, updatePosition));
    }

    private OverlayPositionEditor(Screen parent, String title, Position position, OverlayLayout layout,
                                  BiConsumer<DrawContext, Bounds> drawPreview, Consumer<Position> updatePosition) {
        super(Text.literal(title + " Position"));
        this.parent = parent;
        this.original = position;
        this.position = position;
        this.layout = layout;
        this.drawPreview = drawPreview;
        this.updatePosition = updatePosition;
    }

    @Override
    protected void init() {
        dragging = false;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 40, height - 28, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Bounds bounds = bounds();
        context.drawBorder(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2, 0xFFAAAAAA);
        drawPreview.accept(context, bounds);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 8, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Drag or use arrows. Shift moves faster.", width / 2, 21, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, "Press R to reset.", width / 2, 32, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        Bounds bounds = bounds();
        if (mouseX < bounds.x() || mouseX >= bounds.x() + bounds.width()
                || mouseY < bounds.y() || mouseY >= bounds.y() + bounds.height()) return false;

        dragging = true;
        grabX = mouseX - bounds.x();
        grabY = mouseY - bounds.y();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        moveTo(bounds(), (int) Math.round(mouseX - grabX), (int) Math.round(mouseY - grabY));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseReleased(mouseX, mouseY, button);
        dragging = false;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_R) {
            position = original;
            dragging = false;
            return true;
        }
        int step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
        Bounds bounds = bounds();
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> moveTo(bounds, bounds.x() - step, bounds.y());
            case GLFW.GLFW_KEY_RIGHT -> moveTo(bounds, bounds.x() + step, bounds.y());
            case GLFW.GLFW_KEY_UP -> moveTo(bounds, bounds.x(), bounds.y() - step);
            case GLFW.GLFW_KEY_DOWN -> moveTo(bounds, bounds.x(), bounds.y() + step);
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public void close() {
        updatePosition.accept(position);
        ClientConfig.saveConfiguration();
        client.setScreen(parent);
    }

    private Bounds bounds() {
        return layout.bounds(position, width, height);
    }

    private void moveTo(Bounds bounds, int x, int y) {
        position = layout.move(position, bounds, x, y, width, height);
    }
}
