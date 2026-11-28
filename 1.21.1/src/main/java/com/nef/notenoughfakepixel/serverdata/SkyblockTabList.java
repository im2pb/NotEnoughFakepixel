package com.nef.notenoughfakepixel.serverdata;

import com.nef.notenoughfakepixel.variables.Area;
import com.nef.notenoughfakepixel.variables.Location;
import com.nef.notenoughfakepixel.variables.Mayor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record SkyblockTabList(
        Location location, Area area, boolean sandbox, boolean hasArea, Mayor mayor,
        int mithril, int gemstone, List<String> commissions, HashMap<Integer, Integer> pests,
        List<String> visitors, List<String> milestones, DungeonRunStats dungeonStats
) {
    private static final Comparator<PlayerListEntry> TAB_ORDER = Comparator
            .comparing((PlayerListEntry entry) -> entry.getGameMode() == GameMode.SPECTATOR)
            .thenComparing(entry -> {
                Team team = entry.getScoreboardTeam();
                return team == null ? "" : team.getName();
            })
            .thenComparing(entry -> entry.getProfile().getName(), String.CASE_INSENSITIVE_ORDER);
    private static final Pattern PEST_PLOT = Pattern.compile("(\\d+)(?:\\s*[xX]\\s*(\\d+))?");

    private enum GardenSection {
        NONE, VISITORS, MILESTONES
    }

    static List<Text> displayedRows(MinecraftClient client, ClientPlayNetworkHandler handler) {
        PlayerListHud hud = client.inGameHud.getPlayerListHud();
        return handler.getListedPlayerListEntries().stream().sorted(TAB_ORDER).map(hud::getPlayerName).toList();
    }

    static SkyblockTabList read(MinecraftClient client, ClientPlayNetworkHandler handler) {
        return parse(displayedRows(client, handler));
    }

    static SkyblockTabList parse(List<Text> rows) {
        String serverId = "";
        Area area = Area.NONE;
        boolean dungeon = false;
        boolean hasArea = false;
        Mayor mayor = null;
        int mithril = 0;
        int gemstone = 0;
        double secretPercent = -1;
        int deaths = -1;
        int elapsed = -1;
        int crypts = -1;
        List<String> commissions = new ArrayList<>();
        HashMap<Integer, Integer> pests = new HashMap<>();
        List<String> visitors = new ArrayList<>();
        List<String> milestones = new ArrayList<>();
        boolean serverSection = false;
        boolean readingCommissions = false;
        GardenSection gardenSection = GardenSection.NONE;

        for (Text row : rows) {
            String line = SkyblockText.normalize(row.getString());
            String header = line.endsWith(":") ? line.substring(0, line.length() - 1) : line;
            switch (header) {
                case "Server Info", "Account Info", "Player Stats" -> {
                    serverSection = header.equals("Server Info");
                    gardenSection = GardenSection.NONE;
                    readingCommissions = false;
                    continue;
                }
                case "" -> {
                    if (gardenSection == GardenSection.MILESTONES) gardenSection = GardenSection.NONE;
                    readingCommissions = false;
                    continue;
                }
            }

            int colon = line.indexOf(':');
            if (colon >= 0) {
                String label = line.substring(0, colon);
                String value = line.substring(colon + 1).strip();
                switch (label) {
                    case "Secrets Found" -> {
                        double parsed = DungeonRunStats.parseSecrets(value);
                        if (parsed >= 0) secretPercent = parsed;
                    }
                    case "Deaths" -> {
                        int parsed = DungeonRunStats.parseDeaths(value);
                        if (parsed >= 0) deaths = parsed;
                    }
                    case "Time", "Time Elapsed" -> {
                        int parsed = DungeonRunStats.parseElapsed(value);
                        if (parsed >= 0) elapsed = parsed;
                    }
                    case "Crypts" -> {
                        int parsed = DungeonRunStats.parseCrypts(value);
                        if (parsed >= 0) crypts = parsed;
                    }
                }
                if (label.equals("Winner") || label.equals("Current Mayor")) {
                    mayor = Mayor.fromName(value);
                    gardenSection = GardenSection.NONE;
                    readingCommissions = false;
                    continue;
                }
                if (label.equals("Plots")) {
                    parsePests(value, pests);
                    gardenSection = GardenSection.NONE;
                    readingCommissions = false;
                    continue;
                }
                if (serverSection) {
                    boolean serverField = true;
                    switch (label) {
                        case "Server" -> serverId = value;
                        case "Area" -> {
                            area = Area.fromName(value);
                            hasArea = !value.isEmpty();
                        }
                        case "Dungeon" -> dungeon = !value.isEmpty();
                        case "Mithril Powder" -> mithril = Math.max(0, SkyblockText.parseNonnegativeInt(value));
                        case "Gemstone Powder" -> gemstone = Math.max(0, SkyblockText.parseNonnegativeInt(value));
                        default -> serverField = false;
                    }
                    if (serverField) {
                        gardenSection = GardenSection.NONE;
                        readingCommissions = false;
                        continue;
                    }
                }
            }

            if (serverSection && header.equals("Commissions")) {
                readingCommissions = true;
                gardenSection = GardenSection.NONE;
            } else if (line.startsWith("Next Visitor")) {
                readingCommissions = false;
                gardenSection = GardenSection.NONE;
            } else if (line.startsWith("Visitors")) {
                readingCommissions = false;
                gardenSection = GardenSection.VISITORS;
            } else if (line.startsWith("Crop Milestone")) {
                readingCommissions = false;
                gardenSection = GardenSection.MILESTONES;
                milestones.add(legacyFormat(row));
            } else if (readingCommissions) {
                if (legacyFormat(row).contains("\u00a79\u00a7l")) readingCommissions = false;
                else commissions.add(line);
            } else if (gardenSection == GardenSection.VISITORS) {
                visitors.add(legacyFormat(row));
            } else if (gardenSection == GardenSection.MILESTONES) {
                milestones.add(legacyFormat(row));
            }
        }
        Location location = dungeon ? Location.DUNGEON : Location.fromServerId(serverId);
        boolean sandbox = serverId.toLowerCase(Locale.ROOT).contains("_sandbox");
        return new SkyblockTabList(location, area, sandbox, hasArea, mayor, mithril, gemstone,
                List.copyOf(commissions), pests, visitors, milestones,
                location.isDungeon() ? new DungeonRunStats(secretPercent, deaths, elapsed, crypts) : DungeonRunStats.EMPTY);
    }

    boolean indicatesSkyblock() {
        return location != Location.NONE || hasArea;
    }

    private static void parsePests(String value, HashMap<Integer, Integer> pests) {
        for (String entry : value.split(",")) {
            Matcher matcher = PEST_PLOT.matcher(entry.strip());
            if (!matcher.matches()) continue;

            int plot = SkyblockText.parseNonnegativeInt(matcher.group(1));
            int count = matcher.group(2) == null ? 1 : SkyblockText.parseNonnegativeInt(matcher.group(2));
            if (plot >= 1 && plot <= 24 && count > 0) pests.put(plot, count);
        }
    }

    private static String legacyFormat(Text text) {
        StringBuilder result = new StringBuilder();
        text.visit((style, value) -> {
            if (value.isEmpty()) return Optional.empty();

            int start = result.length();
            Formatting color = style.getColor() == null ? null : Formatting.byName(style.getColor().getName());
            if (color != null) result.append(color);
            if (style.isObfuscated()) result.append(Formatting.OBFUSCATED);
            if (style.isBold()) result.append(Formatting.BOLD);
            if (style.isStrikethrough()) result.append(Formatting.STRIKETHROUGH);
            if (style.isUnderlined()) result.append(Formatting.UNDERLINE);
            if (style.isItalic()) result.append(Formatting.ITALIC);
            boolean styled = result.length() != start;
            result.append(value);
            if (styled) result.append(Formatting.RESET);
            return Optional.empty();
        }, Style.EMPTY);
        return result.toString().strip();
    }
}
