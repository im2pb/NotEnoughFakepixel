package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.mixin.HandledScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.lwjgl.glfw.GLFW;

public final class ScrollableTooltips implements ClientModInitializer {
    private static final int TOOLTIP_MARGIN = 4;
    @RegisterInstance
    private static final ScrollableTooltips INSTANCE = new ScrollableTooltips();

    private final TooltipPositioner scrollPositioner = this::position;
    private HandledScreen<?> screen;
    private Slot slot;
    private ItemStack stack = ItemStack.EMPTY;
    private int offset;
    private int maxOffset;
    private boolean tooltipDrawn;

    private ScrollableTooltips() {
    }

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, current, width, height) -> {
            if (!(current instanceof HandledScreen<?> container)) return;

            screen = container;
            reset();
            ScreenEvents.afterRender(container).register((ignored, context, mouseX, mouseY, delta) -> {
                if (!tooltipDrawn) reset();
                tooltipDrawn = false;
            });
            ScreenEvents.remove(container).register(ignored -> {
                if (screen == container) {
                    screen = null;
                    reset();
                }
            });
            ScreenMouseEvents.allowMouseScroll(container).register((ignored, x, y, horizontal, vertical) ->
                    vertical == 0 || !scroll(((HandledScreenAccessor) container).nef$slotAt(x, y), vertical > 0 ? -10 : 10));
            ScreenKeyboardEvents.allowKeyPress(container).register((ignored, key, scanCode, modifiers) -> switch (key) {
                case GLFW.GLFW_KEY_UP -> !scroll(container.focusedSlot, -5);
                case GLFW.GLFW_KEY_DOWN -> !scroll(container.focusedSlot, 5);
                default -> true;
            });
        });
    }

    public static TooltipPositioner positioner(TooltipPositioner vanilla) {
        var client = MinecraftClient.getInstance();
        if (INSTANCE.screen == null || INSTANCE.screen != client.currentScreen || !enabled()
                || INSTANCE.screen.focusedSlot == null || !INSTANCE.screen.focusedSlot.hasStack()
                || !INSTANCE.screen.getScreenHandler().getCursorStack().isEmpty()) return vanilla;
        return INSTANCE.scrollPositioner;
    }

    private Vector2ic position(int screenWidth, int screenHeight, int x, int y, int width, int height) {
        Vector2ic vanilla = HoveredTooltipPositioner.INSTANCE.getPosition(screenWidth, screenHeight, x, y, width, height);
        Slot focused = screen.focusedSlot;
        ItemStack hovered = focused.getStack();
        if (focused != slot || hovered != stack) {
            slot = focused;
            stack = hovered;
            offset = 0;
        }

        tooltipDrawn = true;
        maxOffset = Math.max(0, height - screenHeight + TOOLTIP_MARGIN * 2);
        offset = Math.clamp(offset, 0, maxOffset);
        return maxOffset == 0 ? vanilla : new Vector2i(vanilla.x(), TOOLTIP_MARGIN - offset);
    }

    private boolean scroll(Slot target, int amount) {
        if (maxOffset == 0 || target == null || target != slot || target.getStack() != stack
                || !screen.getScreenHandler().getCursorStack().isEmpty() || !enabled()) return false;

        offset = Math.clamp(offset + amount, 0, maxOffset);
        return true;
    }

    private void reset() {
        slot = null;
        stack = ItemStack.EMPTY;
        offset = 0;
        maxOffset = 0;
        tooltipDrawn = false;
    }

    private static boolean enabled() {
        return ClientConfig.configuration().misc.qolScrollableTooltips;
    }
}
