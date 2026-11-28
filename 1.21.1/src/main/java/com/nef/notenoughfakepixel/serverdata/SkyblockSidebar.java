package com.nef.notenoughfakepixel.serverdata;

import com.nef.notenoughfakepixel.serverdata.SkyblockData.Season;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import com.nef.notenoughfakepixel.variables.Gamemode;
import com.nef.notenoughfakepixel.variables.Slayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public record SkyblockSidebar(String title, List<String> lines) {
    static final SkyblockSidebar EMPTY = new SkyblockSidebar("", List.of());

    private static final Comparator<ScoreboardEntry> SCORE_ORDER = Comparator
            .comparingInt(ScoreboardEntry::value).reversed()
            .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);
    private static final Pattern FLOOR = Pattern.compile("The Catacombs\\s*\\(([EFM][0-9])\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLEARED = Pattern.compile("(?:Dungeon )?Cleared:\\s*(\\d{1,3})\\s*%?(?:\\s*\\([^)]*\\))?");
    private static final Pattern SEASON = Pattern.compile("\\b(Spring|Summer|Autumn|Winter)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME = Pattern.compile("\\b(1[0-2]|0?[1-9]):([0-5][0-9])\\s*(am|pm)\\b", Pattern.CASE_INSENSITIVE);

    enum SlayerPhase {
        NONE, SPAWNING, BOSS, SLAIN, FAILED
    }

    static SkyblockSidebar read(MinecraftClient client) {
        ScoreboardObjective objective = displayedObjective(client);
        if (objective == null) return EMPTY;

        List<String> lines = displayedLines(objective)
                .map(text -> SkyblockText.normalize(text.getString()))
                .toList();
        return new SkyblockSidebar(SkyblockText.normalize(objective.getDisplayName().getString()), lines);
    }

    static ScoreboardObjective displayedObjective(MinecraftClient client) {
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = null;
        Team team = client.player.getScoreboardTeam();
        if (team != null) {
            ScoreboardDisplaySlot teamSlot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
            if (teamSlot != null) objective = scoreboard.getObjectiveForSlot(teamSlot);
        }
        return objective != null ? objective : scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    static Stream<Text> displayedLines(ScoreboardObjective objective) {
        Scoreboard scoreboard = objective.getScoreboard();
        return scoreboard.getScoreboardEntries(objective).stream()
                .filter(entry -> !entry.hidden())
                .sorted(SCORE_ORDER)
                .limit(15)
                .map(entry -> Team.decorateName(scoreboard.getScoreHolderTeam(entry.owner()), entry.name()));
    }

    public static Stream<Text> displayedLines(MinecraftClient client) {
        var objective = displayedObjective(client);
        return objective == null ? Stream.empty() : displayedLines(objective);
    }

    Gamemode gamemode() {
        Gamemode mode = Gamemode.fromTitle(title);
        return mode == Gamemode.CATACOMBS ? Gamemode.SKYBLOCK : mode;
    }

    boolean hasLobbyTitle() {
        return title.toUpperCase(Locale.ROOT).contains("FAKEPIXEL");
    }

    void updateWorldData() {
        DungeonFloor floor = DungeonFloor.NONE;
        int cleared = -1;
        int heat = 0;
        Season season = Season.SPRING;
        int hour = 0;
        int minute = 0;
        boolean am = false;
        for (String line : lines) {
            Matcher floorMatch = FLOOR.matcher(line);
            if (floorMatch.find()) {
                floor = DungeonFloor.fromCode(floorMatch.group(1).toUpperCase(Locale.ROOT));
            } else if (line.startsWith("Heat:")) {
                int value = SkyblockText.parseNonnegativeInt(line.substring("Heat:".length()).replace("♨", "").replace("%", ""));
                if (value >= 0) heat = value;
            }

            Matcher clearedMatch = CLEARED.matcher(line);
            if (clearedMatch.matches()) {
                int percent = Integer.parseInt(clearedMatch.group(1));
                if (percent <= 100) cleared = percent;
            }

            Matcher seasonMatch = SEASON.matcher(line);
            if (seasonMatch.find()) season = Season.fromName(seasonMatch.group(1));
            Matcher timeMatch = TIME.matcher(line);
            if (timeMatch.find()) {
                hour = Integer.parseInt(timeMatch.group(1));
                minute = Integer.parseInt(timeMatch.group(2));
                am = timeMatch.group(3).equalsIgnoreCase("am");
            }
        }
        SkyblockData.applySidebar(floor, cleared, heat, season, hour, minute, am);
    }

    Slayer slayer() {
        for (String line : lines) {
            if (line.startsWith("Voidgloom Seraph")) return Slayer.VOIDGLOOM;
            if (line.startsWith("Inferno Demonlord")) return Slayer.INFERNO;
            if (line.startsWith("Sven Packmaster")) return Slayer.SVEN;
            if (line.startsWith("Revenant Horror")) return Slayer.REVENANT;
            if (line.startsWith("Tarantula Broodfather")) return Slayer.TARANTULA;
        }
        return Slayer.NONE;
    }

    SlayerPhase slayerPhase() {
        SlayerPhase phase = SlayerPhase.NONE;
        for (String line : lines) {
            if (line.contains("Quest Failed")) return SlayerPhase.FAILED;
            if (line.contains("Boss slain!")) return SlayerPhase.SLAIN;
            if (line.contains("Slay the boss!")) phase = SlayerPhase.BOSS;
            if (line.contains(") Kills") && phase == SlayerPhase.NONE) phase = SlayerPhase.SPAWNING;
        }
        return phase;
    }
}
