package com.nef.notenoughfakepixel.variables;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.regex.Pattern;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum Location {

    DWARVEN("sbm-", "sbm_sandbox-", "sbm_test-"),
    HUB("skyblock-", "skyblock_sandbox-", "skyblocktest-"),
    PRIVATE_HUB("skyblock_private-", "none", "none"),
    ADMIN_HUB("skyblock_admin-", "skyblock_admin_sandbox-", "skyblock_admin_test-"),
    DUNGEON_HUB("sbdh-", "sbdh_sandbox-", "sbdh_test-"),
    BARN("sbfarms-", "sbfarms_sandbox-", "sbfarms_test"),
    PARK("sbpark-", "sbpark_sandbox-", "sbpark_test-"),
    GOLD_MINE("sbmines-", "sbmines_sandbox-", "sbmines_test-"),
    PRIVATE_ISLAND("sbi-", "sbi_sandbox-", "sbi_test-"),
    JERRY("sbj-", "sbj_sandbox-", "sbj_test-"),
    SPIDERS_DEN("sbspiders-", "sbspiders_sandbox-", "sbspiders_test-"),
    THE_END("sbend-", "sbend_sandbox-", "sbend_test-"),
    CRIMSON_ISLE("sbcris-", "sbcris_sandbox-", "sbcris_test-"),
    DUNGEON("sbdungeon-", "sbdungeon_sandbox-", "sbdungeon_test-"),
    CRYSTAL_HOLLOWS("sbch-", "sbch_sandbox-", "sbtest_alpha-"),
    GARDEN("sbg-", "sbg_sandbox-", "sbg_test-"),
    NONE("", "", "");

    private static final Pattern SERVER_SUFFIX = Pattern.compile("-\\d*$");
    private final String main;
    private final String sandbox;
    private final String alpha;

    public static Location fromServerId(String serverId) {
        String normalized = Formatting.strip(serverId).strip().toLowerCase(Locale.ROOT);
        String prefix = SERVER_SUFFIX.matcher(normalized).replaceFirst("");
        for (Location location : values()) {
            if (matchesServerId(prefix, location.main)
                    || matchesServerId(prefix, location.sandbox)
                    || matchesServerId(prefix, location.alpha)) {
                return location;
            }
        }
        return NONE;
    }

    private static boolean matchesServerId(String serverId, String prefix) {
        if (prefix.isEmpty() || prefix.equals("none")) return false;
        if (prefix.endsWith("-")) prefix = prefix.substring(0, prefix.length() - 1);
        return prefix.equals(serverId);
    }

    public boolean isDungeon() {
        return this == DUNGEON;
    }

    public boolean isHub() {
        return this == HUB || this == PRIVATE_HUB || this == ADMIN_HUB;
    }

    public boolean isCrimson() {
        return this == CRIMSON_ISLE;
    }

    public boolean isEnd() {
        return this == THE_END;
    }

    public boolean isCrystalHollows() {
        return this == CRYSTAL_HOLLOWS;
    }

}
