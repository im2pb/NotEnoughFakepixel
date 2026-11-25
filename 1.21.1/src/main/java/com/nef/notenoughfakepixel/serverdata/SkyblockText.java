package com.nef.notenoughfakepixel.serverdata;

import net.minecraft.util.Formatting;

import java.util.regex.Pattern;

final class SkyblockText {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern INTEGER = Pattern.compile("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)");

    private SkyblockText() {
    }

    static String normalize(String text) {
        return WHITESPACE.matcher(Formatting.strip(text)).replaceAll(" ").strip();
    }

    static int parseNonnegativeInt(String text) {
        String number = text.strip();
        if (!INTEGER.matcher(number).matches()) return -1;
        try {
            return Integer.parseInt(number.replace(",", ""));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
