package com.nef.notenoughfakepixel.features.skyblock.garden;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.features.skyblock.garden.GardenOverlay.Layout;
import com.nef.notenoughfakepixel.features.skyblock.garden.GardenOverlay.Panel;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

final class GardenPositionEditor extends Screen {
    private final Screen parent;
    private final Panel panel;
    private final Position original;
    private final float scale;
    private final String background;
    private final Consumer<Position> updatePosition;
    private Position position;
    private boolean dragging;
    private double grabX;
    private double grabY;

    GardenPositionEditor(Screen parent, Panel panel, Position position, float scale, String background,
                         Consumer<Position> updatePosition) {
        super(Text.literal(panel.title + " Position"));
        this.parent = parent;
        this.panel = panel;
        this.original = position;
        this.position = position;
        this.scale = scale;
        this.background = background;
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
        Layout layout = layout();
        context.drawBorder(layout.x() - 1, layout.y() - 1, layout.scaledWidth() + 2, layout.scaledHeight() + 2, 0xFFAAAAAA);
        GardenOverlay.drawPanel(context, textRenderer, panel.preview, layout, panel.background.resolve(background));
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, 8, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Drag or use arrows. Shift moves faster.", width / 2, 21, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer, "Press R to reset.", width / 2, 32, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        Layout bounds = layout();
        if (mouseX < bounds.x() || mouseX >= bounds.x() + bounds.scaledWidth()
                || mouseY < bounds.y() || mouseY >= bounds.y() + bounds.scaledHeight()) return false;

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
        moveTo(layout(), (int) Math.round(mouseX - grabX), (int) Math.round(mouseY - grabY));
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
        Layout bounds = layout();
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

    private Layout layout() {
        return GardenOverlay.layout(textRenderer, panel.preview, position, scale, width, height);
    }

    private void moveTo(Layout bounds, int x, int y) {
        int rawX = offsetAt(x, position.x(), position.centerX(), width, bounds.scaledWidth());
        int rawY = offsetAt(y, position.y(), position.centerY(), height, bounds.scaledHeight());
        position = new Position(rawX, rawY, position.centerX(), position.centerY());
    }

    private static int offsetAt(int position, int previousOffset, boolean centered, int viewport, int panelSize) {
        int end = Math.max(0, viewport - panelSize);
        int start = Math.clamp(position, 0, end);
        if (centered) return start - viewport / 2;
        if (previousOffset < 0 && start < end) return start - end;
        return start;
    }
}
