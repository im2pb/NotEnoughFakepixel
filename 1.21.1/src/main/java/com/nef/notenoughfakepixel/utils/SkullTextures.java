package com.nef.notenoughfakepixel.utils;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class SkullTextures {
    private SkullTextures() {}

    /** Reads the skin URL from a profile property without requesting the texture. */
    public static String skinUrl(String encoded) {
        if (encoded.length() > 8192) return "";
        try {
            String json = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            var root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return "";
            var textures = root.getAsJsonObject().get("textures");
            if (textures == null || !textures.isJsonObject()) return "";
            var skin = textures.getAsJsonObject().get("SKIN");
            if (skin == null || !skin.isJsonObject()) return "";
            var url = skin.getAsJsonObject().get("url");
            return url != null && url.isJsonPrimitive() && url.getAsJsonPrimitive().isString() ? url.getAsString() : "";
        } catch (IllegalArgumentException | JsonParseException ignored) {
            return "";
        }
    }

    public static boolean matches(String url, String address) {
        return url.equals("https://" + address) || url.equals("http://" + address);
    }
}
