package com.nef.notenoughfakepixel.serverdata;

import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;

public final class SkyblockDiagnostics {
    private SkyblockDiagnostics() {
    }

    public static MutableText location() {
        return field("Location", SkyblockData.currentLocation())
                .append(" | ").append(field("Area", SkyblockData.currentArea()))
                .append(" | ").append(field("Sandbox", SkyblockData.sandbox()));
    }

    public static MutableText gamemode() {
        return field("Gamemode", SkyblockData.currentGamemode())
                .append(" | ").append(field("In SkyBlock", SkyblockData.isSkyblock()));
    }

    public static Text context() {
        return gamemode().append(" | ").append(location())
                .append(" | ").append(field("Profile", profile()))
                .append(" | ").append(slayerActivity());
    }

    public static Text scoreboard(MinecraftClient client) {
        if (client.world == null || client.player == null) return warning("No client world is loaded.");

        ScoreboardObjective objective = SkyblockSidebar.displayedObjective(client);
        if (objective == null) return warning("No active scoreboard sidebar.");

        MutableText report = Text.empty().append(heading("Scoreboard", Formatting.AQUA)).append(" [")
                .append(displayText(objective.getDisplayName())).append("]");
        SkyblockSidebar.displayedLines(objective)
                .forEach(line -> report.append("\n").append(displayText(line)));
        return report;
    }

    public static Text tabList(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) return warning("No client connection is active.");

        List<Text> rows = SkyblockTabList.displayedRows(client, handler);
        if (rows.isEmpty()) return warning("The tab list has no entries.");

        MutableText report = Text.empty().append(heading("Tab list", Formatting.AQUA))
                .append(" [" + rows.size() + " entries]");
        for (Text row : rows) {
            boolean blank = SkyblockText.normalize(row.getString()).isEmpty();
            report.append("\n").append(blank ? Text.literal("(blank)").formatted(Formatting.GRAY) : displayText(row));
        }
        return report;
    }

    public static Text heldItem(MinecraftClient client) {
        if (client.player == null || client.player.getMainHandStack().isEmpty()) return warning("No item in the main hand.");

        String id = SkyblockItemData.id(client.player.getMainHandStack());
        return id.isEmpty() ? warning("The held item has no SkyBlock ID.") : field("Held item SkyBlock ID", id);
    }

    public static Text snapshot() {
        return lines(
                heading("Loaded SkyBlock data", Formatting.AQUA),
                gamemode(),
                field("Profile", profile()),
                heading("Location", Formatting.GREEN),
                location(),
                field("Mayor", SkyblockData.currentMayor())
                        .append(" | ").append(field("Last held ID",
                                SkyblockData.lastHandItemId().isEmpty() ? "(none)" : SkyblockData.lastHandItemId())),
                heading("Slayer", Formatting.RED),
                slayerActivity(),
                field("Level", SkyblockData.slayerLevel())
                        .append(" | ").append(field("XP", SkyblockData.slayerXp() + " / " + SkyblockData.nextLevelXp()))
                        .append(" | ").append(field("XP remaining", SkyblockData.xpToNextLevel())),
                field("RNGesus meter", String.format(Locale.ROOT, "%.2f%%", SkyblockData.rngesusMeter()))
                        .append(" | ").append(field("Session bosses", SkyblockData.sessionBosses()))
                        .append(" | ").append(field("Total boss time", String.format(Locale.ROOT, "%.3f s", SkyblockData.totalSeconds()))),
                heading("Mining", Formatting.DARK_AQUA),
                field("Mithril powder", SkyblockData.mithrilPowder())
                        .append(" | ").append(field("Gemstone powder", SkyblockData.gemstonePowder()))
                        .append(" | ").append(field("Heat", SkyblockData.heat())),
                heading("Time", Formatting.LIGHT_PURPLE),
                field("Clock", String.format(Locale.ROOT, "%d:%02d %s",
                        SkyblockData.sbHour(), SkyblockData.sbMinute(), SkyblockData.am() ? "AM" : "PM"))
                        .append(" | ").append(field("Season", SkyblockData.season())),
                heading("Dungeons", Formatting.GOLD),
                field("Floor", SkyblockData.currentFloor())
                        .append(" | ").append(field("Cleared", SkyblockData.clearedPercentage() < 0
                                ? "(unknown)" : SkyblockData.clearedPercentage() + "%")),
                heading("Garden", Formatting.DARK_GREEN),
                field("Pests", new TreeMap<>(SkyblockData.activePests())),
                field("Visitors", unformatted(SkyblockData.activeVisitors())),
                field("Crop milestones", unformatted(SkyblockData.cropMilestone()))
        );
    }

    private static MutableText slayerActivity() {
        return field("Slayer", SkyblockData.currentSlayer())
                .append(" | ").append(field("Quest active", SkyblockData.slayerActive()))
                .append(" | ").append(field("Boss active", SkyblockData.bossActive()));
    }

    private static String profile() {
        return SkyblockData.currentProfile() == null ? "(unknown)" : SkyblockData.currentProfile();
    }

    private static Text heading(String title, Formatting color) {
        return Text.literal(title).formatted(color, Formatting.BOLD);
    }

    private static Text warning(String message) {
        return Text.literal(message).formatted(Formatting.YELLOW);
    }

    private static MutableText field(String label, Object value) {
        return Text.literal(label + " ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(value)).formatted(Formatting.WHITE));
    }

    private static Text lines(Text... rows) {
        MutableText report = Text.empty();
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) report.append("\n");
            report.append(rows[i]);
        }
        return report;
    }

    private static Text displayText(Text original) {
        MutableText line = Text.empty();
        original.visit((style, value) -> {
            // Copy the visible style without making server-supplied scoreboard and tab text interactive in chat.
            line.append(Text.literal(value).setStyle(style.withClickEvent(null).withHoverEvent(null).withInsertion(null)));
            return Optional.empty();
        }, Style.EMPTY);
        return line;
    }

    private static List<String> unformatted(List<String> lines) {
        return lines.stream().map(Formatting::strip).toList();
    }
}
