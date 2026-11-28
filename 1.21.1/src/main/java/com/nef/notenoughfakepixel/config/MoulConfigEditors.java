package com.nef.notenoughfakepixel.config;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.gui.annotations.ConfigTitleDisplay;
import com.nef.notenoughfakepixel.config.gui.editors.GuiOptionEditorTitleDisplay;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.common.IMinecraft;
import io.github.notenoughupdates.moulconfig.gui.GuiComponent;
import io.github.notenoughupdates.moulconfig.gui.GuiContext;
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor;
import io.github.notenoughupdates.moulconfig.gui.component.ButtonComponent;
import io.github.notenoughupdates.moulconfig.gui.component.ColumnComponent;
import io.github.notenoughupdates.moulconfig.gui.component.SliderComponent;
import io.github.notenoughupdates.moulconfig.gui.component.TextComponent;
import io.github.notenoughupdates.moulconfig.gui.component.TextFieldComponent;
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder;
import io.github.notenoughupdates.moulconfig.observer.GetSetter;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class MoulConfigEditors {
    private MoulConfigEditors() {
    }

    static void register(ManagedConfigBuilder<Configuration> builder) {
        // The 1.21 Modern release provides components but does not register these editors.
        builder.customProcessor(ConfigTitleDisplay.class, GuiOptionEditorTitleDisplay::new);
        builder.customProcessor(ConfigEditorInfoText.class, (option, info) -> new GuiOptionEditor(option) {});
        builder.customProcessor(ConfigEditorSlider.class, MoulConfigEditors::slider);
        builder.customProcessor(ConfigEditorDropdown.class, MoulConfigEditors::dropdown);
        builder.customProcessor(ConfigEditorText.class, (option, text) -> component(option,
                new TextFieldComponent(binding(() -> (String) option.get(), option::set),
                        64, () -> true, "", IMinecraft.instance.getDefaultFontRenderer())));
        builder.customProcessor(ConfigEditorKeybind.class, (option, keybind) -> component(option,
                new ButtonComponent(label(() -> keyName((int) option.get())), 1,
                        () -> MinecraftClient.getInstance().setScreen(
                                new KeyCaptureScreen(MinecraftClient.getInstance().currentScreen, option,
                                        keybind.defaultKey())))));
    }

    private static GuiOptionEditor slider(ProcessedOption option, ConfigEditorSlider range) {
        var slider = new SliderComponent(binding(
                () -> ((Number) option.get()).floatValue(),
                value -> option.set(option.getType() == int.class ? (Object) Math.round(value) : value)),
                range.minValue(), range.maxValue(), range.minStep(), 56);
        return component(option, new ColumnComponent(label(() -> option.get().toString()), slider));
    }

    private static GuiOptionEditor dropdown(ProcessedOption option, ConfigEditorDropdown dropdown) {
        String[] values = dropdown.values();
        Supplier<Integer> index = () -> {
            Object value = option.get();
            int selected = value instanceof Integer number ? number : Arrays.asList(values).indexOf(value);
            return selected >= 0 && selected < values.length ? selected : -1;
        };
        return component(option, new ButtonComponent(
                label(() -> index.get() == -1 ? "Select" : values[index.get()]), 1, () -> {
                    int next = (index.get() + 1) % values.length;
                    option.set(option.getType() == int.class ? next : values[next]);
                }));
    }

    private static TextComponent label(Supplier<String> label) {
        return new TextComponent(IMinecraft.instance.getDefaultFontRenderer(), label,
                60, TextComponent.TextAlignment.CENTER, false, false);
    }

    private static GuiOptionEditor component(ProcessedOption option, GuiComponent contents) {
        return new ComponentEditor(option) {
            private final GuiComponent delegate = wrapComponent(contents);

            @Override
            public GuiComponent getDelegate() {
                return delegate;
            }

            @Override
            public void setGuiContext(GuiContext context) {
                // MoulConfig's ComponentEditor only sets the context on the outer panel.
                delegate.foldRecursive(context, (component, guiContext) -> {
                    component.setContext(guiContext);
                    return guiContext;
                });
            }
        };
    }

    private static <T> GetSetter<T> binding(Supplier<T> get, Consumer<T> set) {
        return new GetSetter<>() {
            @Override
            public T get() {
                return get.get();
            }

            @Override
            public void set(T value) {
                set.accept(value);
            }
        };
    }

    private static String keyName(int key) {
        return key == GLFW.GLFW_KEY_UNKNOWN ? "Unbound"
                : InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
    }

    private static final class KeyCaptureScreen extends Screen {
        private final Screen parent;
        private final ProcessedOption option;
        private final int defaultKey;

        private KeyCaptureScreen(Screen parent, ProcessedOption option, int defaultKey) {
            super(Text.literal(option.getName()));
            this.parent = parent;
            this.option = option;
            this.defaultKey = defaultKey;
        }

        @Override
        protected void init() {
            addDrawableChild(ButtonWidget.builder(Text.literal("Reset to Default"), button -> {
                option.set(defaultKey);
                close();
            }).dimensions(width / 2 - 60, height / 2 + 32, 120, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            renderBackground(context, mouseX, mouseY, delta);
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, height / 2 - 18, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, "Press a key to bind", width / 2, height / 2, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, "Backspace to unbind, Esc to cancel",
                    width / 2, height / 2 + 14, 0xAAAAAA);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                option.set(keyCode == GLFW.GLFW_KEY_BACKSPACE ? GLFW.GLFW_KEY_UNKNOWN : keyCode);
            }
            close();
            return true;
        }

        @Override
        public void close() {
            MinecraftClient.getInstance().setScreen(parent);
        }
    }
}
