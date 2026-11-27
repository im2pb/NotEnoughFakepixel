package com.nef.notenoughfakepixel.features.skyblock.pets;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.inventory.ContainerListPanel;
import com.nef.notenoughfakepixel.features.skyblock.inventory.PagedMenuTracker;
import com.nef.notenoughfakepixel.nefrepo.NefRepo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RegisterEvents
public final class PetTracker implements ClientModInitializer {
    private static final Pattern TITLE = Pattern.compile("Pets(?:\\s*\\((?:Page\\s+)?\\d+(?:\\s*/\\s*\\d+)?\\))?");
    private static final Text MISSING = Text.literal("Missing Pets").formatted(Formatting.GOLD);
    private static final Text UPGRADEABLE = Text.literal("Upgradeable Pets").formatted(Formatting.GREEN);
    private static final Text SHORT_UPGRADEABLE = Text.literal("Upgrades").formatted(Formatting.GREEN);

    private final PagedMenuTracker<PetData.Owned> scan = new PagedMenuTracker<>(PetTracker::isPetsTitle,
            () -> ClientConfig.configuration().pets.petTracker, PetData::fromMenu);
    private final ContainerListPanel missing = new ContainerListPanel();
    private final ContainerListPanel upgrades = new ContainerListPanel();
    private long lastRevision = -1;
    private Map<String, String> lastCatalog;
    private List<Text> missingRows = List.of();
    private List<Text> upgradeRows = List.of();
    private Text scoreTitle = MISSING;
    private Text shortScoreTitle = MISSING;

    @Override
    public void onInitializeClient() {
        scan.register();
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof GenericContainerScreen menu) || !scan.matches(menu)) return;
            if (!scan.navigating()) {
                missing.reset();
                upgrades.reset();
            }
            ScreenEvents.afterRender(menu).register((ignored, context, x, y, delta) -> render(client, menu, context, x, y));
            ScreenMouseEvents.allowMouseScroll(menu).register((ignored, x, y, horizontal, vertical) ->
                    !scan.active(client, menu) || !(missing.scrollAt(x, y, vertical) || upgrades.scrollAt(x, y, vertical)));
        });
    }

    static boolean isPetsTitle(String title) {
        return TITLE.matcher(title).matches();
    }

    static boolean isPetsMenu(Screen screen) {
        return screen instanceof GenericContainerScreen
                && isPetsTitle(Formatting.strip(screen.getTitle().getString()).strip());
    }

    private void render(MinecraftClient client, GenericContainerScreen menu, DrawContext context, int mouseX, int mouseY) {
        missing.clearBounds();
        upgrades.clearBounds();
        if (!scan.active(client, menu)) return;

        var snapshot = NefRepo.pets().snapshot();
        Map<String, String> catalog = snapshot.data();
        boolean ready = catalog != null && scan.complete(menu);
        if (ready && (lastCatalog != catalog || lastRevision != scan.revision())) {
            updateRows(PetData.report(catalog, scan.items()));
            lastCatalog = catalog;
            lastRevision = scan.revision();
        }

        String status = switch (snapshot.status()) {
            case LOADING -> "Loading pet catalog…";
            case UNAVAILABLE -> "Pet catalog unavailable.";
            case READY -> scan.reading() ? "Reading pets…" : "Visit every page to see your results.";
        };
        var font = client.textRenderer;
        missing.draw(context, font, ContainerListPanel.beside(context, menu, true), ready ? scoreTitle : MISSING,
                ready ? shortScoreTitle : MISSING, ready ? missingRows : List.of(),
                ready ? "No missing pets." : status, mouseX, mouseY);
        upgrades.draw(context, font, ContainerListPanel.beside(context, menu, false), UPGRADEABLE,
                SHORT_UPGRADEABLE, ready ? upgradeRows : List.of(), ready ? "No upgradeable pets." : status, mouseX, mouseY);
    }

    private void updateRows(PetData.Report report) {
        scoreTitle = Text.literal("Missing Pets · " + report.score() + " pts").formatted(Formatting.GOLD);
        shortScoreTitle = Text.literal("Score " + report.score()).formatted(Formatting.GOLD);
        missingRows = report.missing().stream().<Text>map(pet -> Text.literal(pet).formatted(Formatting.RED)).toList();
        upgradeRows = report.upgrades().stream().<Text>map(pet -> Text.literal(pet.name()).formatted(PetData.scoreColor(pet.score()))
                .append(Text.literal(" +" + pet.gain() + " pts").formatted(Formatting.YELLOW))).toList();
    }
}
