package com.nef.notenoughfakepixel.config.gui.editors;

import com.nef.notenoughfakepixel.config.gui.annotations.ConfigTitleDisplay;
import com.nef.notenoughfakepixel.env.VersionChecker;
import io.github.notenoughupdates.moulconfig.common.RenderContext;
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;
import net.fabricmc.loader.api.FabricLoader;

public final class GuiOptionEditorTitleDisplay extends GuiOptionEditor {
    private final String title;
    private final float scale;
    private final int color;
    private final boolean shadow;

    public GuiOptionEditorTitleDisplay(ProcessedOption option, ConfigTitleDisplay display) {
        super(option);
        title = switch (display.type()) {
            case VERSION -> "v" + VersionChecker.installedVersion();
            case TEXT -> display.text();
            case MODNAME -> FabricLoader.getInstance().getModContainer("notenoughfakepixel")
                    .map(mod -> mod.getMetadata().getName())
                    .orElse("NotEnoughFakepixel");
        };
        scale = switch (display.size()) {
            case BIG -> 2f;
            case MEDIUM -> 1.5f;
            case SMALL -> 1f;
        };
        color = 0xFF000000 | display.color();
        shadow = display.shadow();
    }

    @Override
    public int getHeight() {
        return Math.round(20 * scale);
    }

    @Override
    public void render(RenderContext context, int x, int y, int width) {
        int height = getHeight();
        context.drawDarkRect(x, y, width, height, true);

        var font = context.getMinecraft().getDefaultFontRenderer();
        int textWidth = font.getStringWidth(title);
        if (textWidth == 0 || width <= 8) return;

        float textScale = Math.min(scale, (width - 8f) / textWidth);
        context.pushMatrix();
        context.translate(x + (width - textWidth * textScale) / 2f,
                y + (height - font.getHeight() * textScale) / 2f, 0);
        context.scale(textScale, textScale, 1);
        context.drawString(font, title, 0, 0, color, shadow);
        context.popMatrix();
    }
}
