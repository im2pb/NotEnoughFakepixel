/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

/** Pure classification policy shared by player rendering and NPC-adjacent hiding. */
public final class PlayerVisibilityPolicy {
    private PlayerVisibilityPolicy() {
    }

    /**
     * A player-shaped entity is a real remote player only when it is represented in the
     * server player list and is not identified as a known NPC. Entities that fail any
     * part of this test are deliberately left visible.
     */
    public static boolean isRealOnlinePlayer(boolean remotePlayerEntity,
                                             boolean hasPlayerInfo,
                                             boolean recognizedNpc) {
        return remotePlayerEntity && hasPlayerInfo && !recognizedNpc;
    }

    public static boolean shouldHidePlayer(boolean enabled,
                                            boolean remotePlayerEntity,
                                            boolean hasPlayerInfo,
                                            boolean recognizedNpc) {
        return enabled && isRealOnlinePlayer(remotePlayerEntity, hasPlayerInfo, recognizedNpc);
    }
}
