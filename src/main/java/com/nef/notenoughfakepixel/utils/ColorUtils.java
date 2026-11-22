/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import com.nef.notenoughfakepixel.config.gui.core.ChromaColour;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ColorUtils {
    private static final String DEFAULT_COLOR_STRING = "0:255:255:255:255"; 
    private static final Color DEFAULT_COLOR = new Color(255, 255, 255, 255);

    private static final Map<String, Color> STATIC_COLORS = new ConcurrentHashMap<>();
    private static final int STATIC_COLORS_MAX = 256;

    public static @NotNull Color getColor(String colorString) {
        if (colorString != null) {
            Color cached = STATIC_COLORS.get(colorString);
            if (cached != null) return cached;
        }

        if (colorString == null || colorString.split(":").length != 5) {
            System.err.println("Invalid color string: " + colorString + ". Expected 'chromaSpeed:alpha:r:g:b'. Using default: " + DEFAULT_COLOR_STRING);
            return DEFAULT_COLOR;
        }

        try {
            int argb = ChromaColour.specialToChromaRGB(colorString);
            Color color = new Color(argb, true); 
            if (ChromaColour.getSpeed(colorString) <= 0) { 
                if (STATIC_COLORS.size() >= STATIC_COLORS_MAX) STATIC_COLORS.clear();
                STATIC_COLORS.put(colorString, color);
            }
            return color;
        } catch (Exception e) {
            System.err.println("Error parsing color string: " + colorString + ". Error: " + e.getMessage() + ". Using default: " + DEFAULT_COLOR_STRING);
            return DEFAULT_COLOR;
        }
    }

    public static String cleanColor(String in) {
        return in.replaceAll("(?i)\\u00A7.", "");
    }

    public static String colorToHex(Color color, boolean includeAlpha) {
        if (color == null) {
            return includeAlpha ? "#FFFFFFFF" : "#FFFFFF"; 
        }
        if (includeAlpha) {
            return String.format("#%08X", color.getRGB()); 
        } else {
            return String.format("#%06X", color.getRGB() & 0xFFFFFF); 
        }
    }
}
