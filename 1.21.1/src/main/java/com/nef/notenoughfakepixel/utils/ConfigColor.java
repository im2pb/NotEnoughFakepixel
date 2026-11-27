package com.nef.notenoughfakepixel.utils;

import io.github.notenoughupdates.moulconfig.ChromaColour;

import java.util.Objects;

/** Resolves a MoulConfig color setting, reusing its parsed value until the setting changes. */
public final class ConfigColor {
    private String setting;
    private int staticColor = 0xFFFFFFFF;
    private ChromaColour animatedColor;

    public int resolve(String value) {
        if (!Objects.equals(setting, value)) {
            setting = value;
            ChromaColour parsed = parse(value);
            staticColor = parsed == null ? 0xFFFFFFFF : argb(parsed);
            animatedColor = parsed != null && parsed.getTimeForFullRotationInMillis() > 0 ? parsed : null;
        }
        return animatedColor == null ? staticColor : argb(animatedColor);
    }

    private static int argb(ChromaColour color) {
        return (color.getAlpha() << 24) | (color.getEffectiveColour().getRGB() & 0xFFFFFF);
    }

    @SuppressWarnings("deprecation") // MoulConfig's color editor saves these settings in the legacy string format.
    private static ChromaColour parse(String value) {
        if (value == null) return null;

        String[] components = value.split(":", -1);
        if (components.length != 5) return null;

        try {
            for (String component : components) {
                int channel = Integer.parseInt(component);
                if (channel < 0 || channel > 255) return null;
            }
        } catch (NumberFormatException error) {
            return null;
        }
        return ChromaColour.forLegacyString(value);
    }
}
