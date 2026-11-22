/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.map;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.config.gui.core.config.KeybindHelper;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonParty;
import com.nef.notenoughfakepixel.utils.Logger;
import com.nef.notenoughfakepixel.utils.SkyblockGate;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec4b;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RegisterEvents
public final class DungeonPlayerTracker {
    public static final class PlayerMarker {
        private final String name;
        private final String key;
        private final double mapX;
        private final double mapZ;

        private PlayerMarker(String name, String key, double mapX, double mapZ) {
            this.name = name;
            this.key = key;
            this.mapX = mapX;
            this.mapZ = mapZ;
        }

        public String getName() { return name; }
        
        public String getKey() { return key; }
        public double getMapX() { return mapX; }
        public double getMapZ() { return mapZ; }
        public boolean hasMapPosition() { return !Double.isNaN(mapX) && !Double.isNaN(mapZ); }
    }

    private static final Minecraft MC = Minecraft.getMinecraft();

    private static final double[] MAP_SCALES = {20.0 / 32.0, 22.0 / 32.0};
    
    private static final double GEOMETRY_TOLERANCE = 3.0;
    
    private static final double GEOMETRY_MARGIN = 2.0;

    private static final List<String> PLAYER_NAMES = new ArrayList<>();
    
    private static final Map<String, String> CONFIRMED = new HashMap<>();
    private static final Map<String, float[]> MAP_POSITIONS = new HashMap<>();
    private static final Map<String, String> KEYS_BY_NAME = new HashMap<>();
    private static volatile List<PlayerMarker> SNAPSHOT = Collections.emptyList();

    private static int confirmedForIconCount = -1;
    private static String lastLoggedAssignment = "";

    private int ticksSincePopulate;

    public static List<PlayerMarker> snapshot() {
        return SNAPSHOT;
    }

    /** Returns whether map player names should be collected and displayed. */
    public static boolean shouldShowPlayerNames() {
        return Config.feature.dungeonSystems.alwaysShowMapPlayerNames
                || KeybindHelper.isKeyDown(Config.feature.dungeonSystems.mapPlayerNamesKey);
    }

    public static void clearStatic() {
        MAP_POSITIONS.clear();
        KEYS_BY_NAME.clear();
        PLAYER_NAMES.clear();
        CONFIRMED.clear();
        confirmedForIconCount = -1;
        lastLoggedAssignment = "";
        SNAPSHOT = Collections.emptyList();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!SkyblockGate.isFakepixelDungeon() || MC.theWorld == null || MC.thePlayer == null) {
            if (!SNAPSHOT.isEmpty() || !MAP_POSITIONS.isEmpty() || !PLAYER_NAMES.isEmpty()) {
                clearStatic();
            }
            ticksSincePopulate = 0;
            return;
        }

        if (!shouldShowPlayerNames()) {
            ticksSincePopulate = 0;
            return;
        }

        if (++ticksSincePopulate >= 20 || PLAYER_NAMES.isEmpty()) {
            ticksSincePopulate = 0;
            populate();
        }
    }

    private void populate() {
        if (MC.thePlayer == null || MC.theWorld == null) return;
        String selfName = MC.thePlayer.getName();
        if (selfName == null || selfName.isEmpty()) return;

        List<String> roster = buildRoster(selfName);
        if (!roster.equals(PLAYER_NAMES)) {
            PLAYER_NAMES.clear();
            PLAYER_NAMES.addAll(roster);
        }
    }

    private List<String> buildRoster(String selfName) {
        return DungeonParty.orderedNames(selfName);
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        for (String existing : list) {
            if (existing != null && existing.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    public static void updateMapDecorations(Map<String, Vec4b> decorations) {
        if (decorations == null || decorations.isEmpty() || MC.thePlayer == null || INSTANCE == null) return;
        if (!shouldShowPlayerNames()) return;
        INSTANCE.applyDecorations(decorations);
    }

    private static DungeonPlayerTracker INSTANCE;

    {
        INSTANCE = this;
    }

    private void applyDecorations(Map<String, Vec4b> mapDecorations) {
        if (PLAYER_NAMES.isEmpty()) populate();
        if (PLAYER_NAMES.isEmpty() || MC.thePlayer == null || MC.theWorld == null) return;
        String selfName = MC.thePlayer.getName();

        List<String> keys = new ArrayList<>();
        List<Vec4b> icons = new ArrayList<>();
        for (Map.Entry<String, Vec4b> entry : mapDecorations.entrySet()) {
            Vec4b deco = entry.getValue();
            if (deco == null) continue;
            int type = deco.func_176110_a();
            if (type != 0 && type != 1 && type != 3) continue;
            keys.add(entry.getKey());
            icons.add(deco);
        }
        if (icons.isEmpty()) return;

        if (confirmedForIconCount != icons.size()) {
            CONFIRMED.clear();
            confirmedForIconCount = icons.size();
        }

        String[] assigned = new String[icons.size()];
        Set<String> used = new HashSet<>();
        String[] how = new String[icons.size()];

        int selfIndex = -1;
        for (int i = 0; i < icons.size(); i++) {
            if (icons.get(i).func_176110_a() == 1) {
                selfIndex = i;
                break;
            }
        }
        if (selfIndex >= 0) {
            assigned[selfIndex] = selfName;
            how[selfIndex] = "self";
            used.add(selfName.toLowerCase(Locale.ROOT));
        }

        for (int i = 0; i < icons.size(); i++) {
            if (assigned[i] != null) continue;
            String confirmed = CONFIRMED.get(keys.get(i));
            if (confirmed != null && !used.contains(confirmed.toLowerCase(Locale.ROOT)) && containsIgnoreCase(PLAYER_NAMES, confirmed)) {
                assigned[i] = confirmed;
                how[i] = "position(kept)";
                used.add(confirmed.toLowerCase(Locale.ROOT));
            }
        }
        if (selfIndex >= 0) matchByPosition(icons, keys, selfIndex, assigned, how, used);

        List<String> remainingNames = new ArrayList<>();
        for (String name : PLAYER_NAMES) {
            if (!used.contains(name.toLowerCase(Locale.ROOT))) remainingNames.add(name);
        }
        List<Integer> remainingIcons = new ArrayList<>();
        for (int i = 0; i < icons.size(); i++) {
            if (assigned[i] == null) remainingIcons.add(i);
        }
        if (!remainingIcons.isEmpty() && remainingIcons.size() == remainingNames.size()) {
            for (int i = 0; i < remainingIcons.size(); i++) {
                int icon = remainingIcons.get(i);
                assigned[icon] = remainingNames.get(i);
                how[icon] = "order";
            }
        }

        MAP_POSITIONS.clear();
        KEYS_BY_NAME.clear();
        for (int i = 0; i < icons.size(); i++) {
            if (assigned[i] == null) continue;
            Vec4b deco = icons.get(i);
            float mapX = (float) deco.func_176112_b() / 2.0F + 64.0F;
            float mapZ = (float) deco.func_176113_c() / 2.0F + 64.0F;
            MAP_POSITIONS.put(assigned[i], new float[]{mapX, mapZ});
            KEYS_BY_NAME.put(assigned[i], keys.get(i));
        }

        logAssignment(keys, assigned, how);
        publishSnapshot();
    }

    private void matchByPosition(List<Vec4b> icons, List<String> keys, int selfIndex, String[] assigned, String[] how, Set<String> used) {
        Vec4b self = icons.get(selfIndex);
        double selfMapX = self.func_176112_b() / 2.0 + 64.0;
        double selfMapZ = self.func_176113_c() / 2.0 + 64.0;

        List<String> names = new ArrayList<>();
        List<double[]> offsets = new ArrayList<>();
        for (String name : PLAYER_NAMES) {
            if (used.contains(name.toLowerCase(Locale.ROOT))) continue;
            EntityPlayer player = MC.theWorld.getPlayerEntityByName(name);
            if (player == null || player == MC.thePlayer) continue;
            names.add(name);
            offsets.add(new double[]{player.posX - MC.thePlayer.posX, player.posZ - MC.thePlayer.posZ});
        }
        if (names.isEmpty()) return;

        double bestScale = MAP_SCALES[0];
        double bestError = Double.MAX_VALUE;
        for (double scale : MAP_SCALES) {
            double total = 0;
            for (double[] offset : offsets) {
                total += nearestDistance(icons, assigned, selfMapX + offset[0] * scale, selfMapZ + offset[1] * scale, null);
            }
            if (total < bestError) {
                bestError = total;
                bestScale = scale;
            }
        }

        for (int n = 0; n < names.size(); n++) {
            double px = selfMapX + offsets.get(n)[0] * bestScale;
            double pz = selfMapZ + offsets.get(n)[1] * bestScale;

            int[] bestIndex = {-1};
            double best = nearestDistance(icons, assigned, px, pz, bestIndex);
            if (bestIndex[0] < 0 || best > GEOMETRY_TOLERANCE) continue;

            double second = Double.MAX_VALUE;
            for (int i = 0; i < icons.size(); i++) {
                if (i == bestIndex[0] || assigned[i] != null) continue;
                second = Math.min(second, distance(icons.get(i), px, pz));
            }
            if (second != Double.MAX_VALUE && second < best * GEOMETRY_MARGIN && second <= GEOMETRY_TOLERANCE * 2) continue;

            boolean contested = false;
            for (int other = 0; other < names.size(); other++) {
                if (other == n) continue;
                double ox = selfMapX + offsets.get(other)[0] * bestScale;
                double oz = selfMapZ + offsets.get(other)[1] * bestScale;
                if (distance(icons.get(bestIndex[0]), ox, oz) <= best * GEOMETRY_MARGIN) {
                    contested = true;
                    break;
                }
            }
            if (contested) continue;

            String name = names.get(n);
            assigned[bestIndex[0]] = name;
            how[bestIndex[0]] = "position";
            used.add(name.toLowerCase(Locale.ROOT));
            CONFIRMED.put(keys.get(bestIndex[0]), name);
        }
    }

    private static double nearestDistance(List<Vec4b> icons, String[] assigned, double px, double pz, int[] indexOut) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < icons.size(); i++) {
            if (assigned[i] != null) continue;
            double d = distance(icons.get(i), px, pz);
            if (d < best) {
                best = d;
                if (indexOut != null) indexOut[0] = i;
            }
        }
        return best;
    }

    private static double distance(Vec4b icon, double px, double pz) {
        double dx = icon.func_176112_b() / 2.0 + 64.0 - px;
        double dz = icon.func_176113_c() / 2.0 + 64.0 - pz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void logAssignment(List<String> keys, String[] assigned, String[] how) {
        StringBuilder sb = new StringBuilder("[NEF Map] markers=").append(keys.size()).append(" party=").append(PLAYER_NAMES);
        for (int i = 0; i < keys.size(); i++) {
            sb.append(' ').append(keys.get(i)).append('=').append(assigned[i] == null ? "?" : assigned[i])
                    .append('(').append(how[i] == null ? "-" : how[i]).append(')');
        }
        String line = sb.toString();
        if (line.equals(lastLoggedAssignment)) return;
        lastLoggedAssignment = line;
        Logger.logConsole(line);
    }

    private static void publishSnapshot() {
        List<PlayerMarker> next = new ArrayList<>(MAP_POSITIONS.size());

        for (String name : PLAYER_NAMES) {
            float[] map = MAP_POSITIONS.get(name);
            if (map == null) continue;
            next.add(new PlayerMarker(name, KEYS_BY_NAME.get(name), map[0], map[1]));
        }

        SNAPSHOT = Collections.unmodifiableList(next);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            DungeonParty.clear();
            clearStatic();
            ticksSincePopulate = 0;
        }
    }
}
