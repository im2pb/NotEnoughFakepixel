/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.utils.ScoreboardUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DungeonParty {

    public static final String UNKNOWN = "Unknown";

    private static final Pattern CLASS_TOKEN = Pattern.compile("\\[(B|A|H|M|T|Archer|Berserk|Healer|Mage|Tank)]");
    
    private static final Pattern TAB_CLASS = Pattern.compile("\\((Archer|Berserk|Healer|Mage|Tank)\\b", Pattern.CASE_INSENSITIVE);
    
    private static final Pattern LAST_WORD = Pattern.compile("(\\w{1,16})[^\\w]*$");
    
    private static final Pattern FIRST_NAME = Pattern.compile("^\\s*(?:\\[[^\\]]*]\\s*)*(\\w{1,16})");

    private static final int TRUNCATED_NAME_LENGTH = 10;

    private static final Map<String, String> CLASS_BY_NAME = new HashMap<>();
    
    private static final List<String> ORDERED = new ArrayList<>();

    private DungeonParty() {}

    public static void clear() {
        CLASS_BY_NAME.clear();
        ORDERED.clear();
    }

    public static void refresh() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        Collection<NetworkPlayerInfo> tab = mc.getNetHandler() == null ? null : mc.getNetHandler().getPlayerInfoMap();
        String selfName = mc.thePlayer.getName();

        List<String> realNames = new ArrayList<>();
        Map<String, String> tabClasses = new LinkedHashMap<>();
        if (tab != null) {
            for (NetworkPlayerInfo info : tab) {
                if (info == null) continue;
                if (info.getGameProfile() != null && isPlausibleUsername(info.getGameProfile().getName())) {
                    addIgnoreCase(realNames, info.getGameProfile().getName());
                }
                String display = displayText(info);
                if (display == null) continue;
                Matcher classMatcher = TAB_CLASS.matcher(display);
                if (!classMatcher.find()) continue;
                Matcher nameMatcher = LAST_WORD.matcher(display.substring(0, classMatcher.start()));
                if (!nameMatcher.find()) continue;
                String shown = nameMatcher.group(1);
                if (!isPlausibleUsername(shown)) continue;
                String real = findIgnoreCase(realNames, shown);
                String name = real != null ? real : shown;
                if (real == null) addIgnoreCase(realNames, shown);
                tabClasses.put(name, normalizeClass(classMatcher.group(1)));
            }
        }
        if (isPlausibleUsername(selfName)) addIgnoreCase(realNames, selfName);

        List<String> ordered = new ArrayList<>();
        boolean sawClassLine = false;
        List<String> lines = ScoreboardUtils.getScoreboardLines();
        for (String raw : lines) {
            if (raw == null || raw.isEmpty()) continue;
            String line = visibleText(raw);
            Matcher token = CLASS_TOKEN.matcher(line);
            if (!token.find()) continue;
            String after = line.substring(token.end());
            String found = null;
            Matcher first = FIRST_NAME.matcher(after);
            if (first.find()) found = first.group(1);
            String resolved = found == null ? null : resolveName(found, realNames);
            if (resolved == null) resolved = containedName(after, realNames);
            if (resolved == null) continue;

            sawClassLine = true;
            if (!containsIgnoreCase(ordered, resolved)) ordered.add(resolved);
            CLASS_BY_NAME.put(resolved.toLowerCase(Locale.ROOT), normalizeClass(token.group(1)));
        }

        if (!sawClassLine) {
            for (String raw : lines) {
                if (raw == null || raw.isEmpty()) continue;
                String line = visibleText(raw);
                for (String name : realNames) {
                    if (containsName(line, name) && !containsIgnoreCase(ordered, name)) ordered.add(name);
                }
            }
        }

        for (Map.Entry<String, String> entry : tabClasses.entrySet()) {
            CLASS_BY_NAME.putIfAbsent(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }

        ORDERED.clear();
        ORDERED.addAll(ordered);
    }

    public static List<String> orderedNames(String selfName) {
        refresh();
        List<String> result = new ArrayList<>(ORDERED);
        if (selfName != null && !selfName.isEmpty() && !containsIgnoreCase(result, selfName)) result.add(0, selfName);
        return result;
    }

    public static String classOf(String playerName) {
        if (playerName == null) return null;
        String key = playerName.toLowerCase(Locale.ROOT);
        String exact = CLASS_BY_NAME.get(key);
        if (exact != null) return exact;
        for (Map.Entry<String, String> entry : CLASS_BY_NAME.entrySet()) {
            String stored = entry.getKey();
            if (stored.length() >= TRUNCATED_NAME_LENGTH && key.length() > stored.length() && key.startsWith(stored)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static List<String> debugDump() {
        List<String> out = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            out.add("no world");
            return out;
        }
        refresh();
        out.add("self=" + mc.thePlayer.getName());
        out.add("scoreboard (bottom line first):");
        for (String raw : ScoreboardUtils.getScoreboardLines()) out.add("  " + raw.replace('§', '&'));
        out.add("tab entries with a display name:");
        Collection<NetworkPlayerInfo> tab = mc.getNetHandler() == null ? null : mc.getNetHandler().getPlayerInfoMap();
        int shown = 0;
        if (tab != null) {
            for (NetworkPlayerInfo info : tab) {
                if (info == null || info.getDisplayName() == null) continue;
                String profile = info.getGameProfile() == null ? "?" : info.getGameProfile().getName();
                out.add("  " + profile + " | " + info.getDisplayName().getUnformattedText().replace('§', '&'));
                if (++shown >= 40) break;
            }
        }
        out.add("parsed party=" + ORDERED);
        out.add("parsed classes=" + CLASS_BY_NAME);
        return out;
    }

    private static String displayText(NetworkPlayerInfo info) {
        if (info.getDisplayName() == null) return null;
        return visibleText(info.getDisplayName().getUnformattedText());
    }

    private static String visibleText(String line) {
        String stripped = StringUtils.stripControlCodes(line);
        StringBuilder out = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);
            if (c > 31 && c < 127) out.append(c);
        }
        return out.toString();
    }

    private static String normalizeClass(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "b": case "berserk": return "Berserk";
            case "a": case "archer": return "Archer";
            case "t": case "tank": return "Tank";
            case "h": case "healer": return "Healer";
            case "m": case "mage": return "Mage";
            default: return UNKNOWN;
        }
    }

    private static String resolveName(String token, List<String> realNames) {
        String exact = findIgnoreCase(realNames, token);
        if (exact != null) return exact;
        if (token.length() < TRUNCATED_NAME_LENGTH) return isPlausibleUsername(token) ? token : null;
        String match = null;
        int matches = 0;
        for (String name : realNames) {
            if (name.length() > token.length() && name.regionMatches(true, 0, token, 0, token.length())) {
                match = name;
                matches++;
            }
        }
        if (matches == 1) return match;
        return isPlausibleUsername(token) ? token : null;
    }

    private static String containedName(String text, List<String> realNames) {
        String best = null;
        for (String name : realNames) {
            if (containsName(text, name) && (best == null || name.length() > best.length())) best = name;
        }
        return best;
    }

    private static boolean containsName(String text, String name) {
        if (text.contains(name)) return true;
        return name.length() > 12 && text.contains(name.substring(0, 12));
    }

    private static boolean isPlausibleUsername(String name) {
        if (name == null || name.length() < 3 || name.length() > 16) return false;
        boolean hasLetter = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')) return false;
            if (Character.isLetter(c)) hasLetter = true;
        }
        return hasLetter;
    }

    private static String findIgnoreCase(List<String> list, String value) {
        for (String existing : list) {
            if (existing.equalsIgnoreCase(value)) return existing;
        }
        return null;
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        return findIgnoreCase(list, value) != null;
    }

    private static void addIgnoreCase(List<String> list, String value) {
        if (!containsIgnoreCase(list, value)) list.add(value);
    }
}
