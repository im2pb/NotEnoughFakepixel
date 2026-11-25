package com.nef.notenoughfakepixel.variables;

import lombok.RequiredArgsConstructor;

import java.util.Locale;

@RequiredArgsConstructor
public enum Gamemode {

    LOBBY("FAKEPIXEL"),
    SKYWARS("SKYWARS"),
    BEDWARS("BED WARS"),
    SKYBLOCK("SKYBLOCK"),
    MURDERMYSTERY("MURDER MYSTERY"),
    DUELS("DUELS"),
    CATACOMBS("CATACOMB");

    private final String titleFragment;

    public static Gamemode fromTitle(String input) {
        String title = input.toUpperCase(Locale.ROOT);
        for (Gamemode gamemode : values()) {
            if (gamemode != LOBBY && title.contains(gamemode.titleFragment)) return gamemode;
        }
        return LOBBY;
    }

    public boolean isSkyblock() {
        return this == SKYBLOCK;
    }
}
