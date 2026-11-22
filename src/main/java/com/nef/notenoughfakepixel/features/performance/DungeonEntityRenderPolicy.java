/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.performance;

import java.util.Locale;

/**
 * Pure policy for the optional maximum-FPS dungeon render mode.
 * Entity simulation and networking are deliberately outside this policy.
 */
public final class DungeonEntityRenderPolicy {

    private DungeonEntityRenderPolicy() {
    }

    /**
     * Dropped item entities are always important gameplay/render feedback in dungeons.
     */
    public static boolean mustAlwaysRender(boolean isDroppedItem) {
        return isDroppedItem;
    }

    /**
     * Some Hypixel dungeon pickups/deployables are not sent as EntityItem. They are
     * represented by named ArmorStands (sometimes invisible) carrying a custom model.
     * Maximum-FPS culling must preserve these marker entities as well as real item drops.
     *
     * The input may contain Minecraft formatting codes. Matching is intentionally exact
     * after normalization. Only known key/deployable labels are exempted; nearby model
     * entities are handled through a separate small-radius association check.
     */
    public static boolean mustAlwaysRenderNamedDungeonMarker(String formattedName) {
        if (formattedName == null || formattedName.isEmpty()) return false;

        StringBuilder plain = new StringBuilder(formattedName.length());
        for (int i = 0; i < formattedName.length(); i++) {
            char c = formattedName.charAt(i);
            if (c == '\u00a7' && i + 1 < formattedName.length()) {
                i++; // Ignore the Minecraft formatting code itself.
                continue;
            }
            plain.append(c);
        }

        String name = plain.toString().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (name.isEmpty()) return false;

        // Keys may be exposed by the server as only "key" while the item model is a
        // separate nearby entity. Accept decorated labels such as "Blood Key (B)" too.
        if (name.equals("key") || name.contains("blood key") || name.contains("wither key")) return true;

        // Dungeon deployables and blessings can also be represented by named marker
        // entities instead of normal dropped Item entities.
        if (name.contains("superboom tnt") || name.startsWith("blessing of ")) return true;

        return name.equals("decoy")
                || name.equals("inflatable jerry")
                || name.equals("spirit leap");
    }

    /**
     * A server can send the floating key/deployable label and its 3D model as separate
     * entities. Keep nearby companion entities visible even when the model itself has no
     * custom name and is not an EntityItem/ArmorStand.
     */
    public static boolean isWithinNamedMarkerRadius(double entityX, double entityY, double entityZ,
                                                     double markerX, double markerY, double markerZ) {
        double dx = entityX - markerX;
        double dy = entityY - markerY;
        double dz = entityZ - markerZ;
        return dx * dx + dy * dy + dz * dz <= 6.25D; // 2.5-block association radius.
    }

    /**
     * Player-model NPCs are commonly sent by Hypixel as EntityOtherPlayerMP instances,
     * even though they are not real network players. Keep the classification explicit:
     * hide an EntityPlayer model when it is not the local player and has no matching
     * NetworkPlayerInfo entry. This rule is evaluated before generic inventory/marker
     * exemptions so a player-model mob cannot accidentally remain visible.
     */
    public static boolean shouldCullPlayerModelNpc(boolean optimizationActive, boolean inDungeon,
                                                   boolean isPlayerModelEntity, boolean isRealPlayer) {
        return optimizationActive && inDungeon && isPlayerModelEntity && !isRealPlayer;
    }

    /**
     * Returns true when an eligible non-player, non-item entity model should be omitted
     * from a dungeon frame. The caller must classify real players and bosses separately.
     * Entity objects continue to update normally.
     */
    public static boolean shouldCull(boolean optimizationActive, boolean inDungeon, boolean isRealPlayer,
                                     boolean isDungeonBoss, boolean isDroppedItem) {
        // The caller classifies player-model NPCs separately. Ordinary dungeon mobs are
        // culled at every distance; there is deliberately no close-range grace radius, so
        // mobs at or inside three blocks are hidden as well.
        return optimizationActive && inDungeon && !isRealPlayer && !isDungeonBoss && !isDroppedItem;
    }
}
