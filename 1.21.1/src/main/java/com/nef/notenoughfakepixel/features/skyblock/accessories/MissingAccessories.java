package com.nef.notenoughfakepixel.features.skyblock.accessories;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.inventory.PagedMenuTracker;
import com.nef.notenoughfakepixel.nefrepo.NefRepo;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Accessory;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Catalog;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Estimate;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;

@RegisterEvents
public final class MissingAccessories implements ClientModInitializer {
    private final PagedMenuTracker<Accessory> scan = new PagedMenuTracker<>(title -> AccessoriesData.clean(title).equals("Accessory Bag"),
            () -> ClientConfig.configuration().accessories.enable, AccessoriesData::fromBag);
    private final AccessoriesPanels panels = new AccessoriesPanels();
    private Catalog lastCatalog;
    private long lastRevision = -1;
    private Estimate estimate;

    @Override
    public void onInitializeClient() {
        scan.register();
        ScreenEvents.AFTER_INIT.register((client, current, width, height) -> {
            if (!(current instanceof GenericContainerScreen bag) || !scan.matches(bag)) return;
            if (!scan.navigating()) panels.missing.reset();

            ScreenEvents.afterRender(bag).register((ignored, context, x, y, delta) -> render(client, bag, context, x, y));
            ScreenMouseEvents.allowMouseScroll(bag).register((ignored, x, y, horizontal, vertical) ->
                    !scan.active(client, bag) || !panels.missing.scrollAt(x, y, vertical));
            ScreenKeyboardEvents.allowKeyPress(bag).register((ignored, key, scanCode, modifiers) -> {
                if (!scan.active(client, bag)) return true;
                var settings = ClientConfig.configuration().accessories;
                if (key == settings.accessoriesDataScrollUpKey && panels.missing.scroll(-1)) return false;
                return key != settings.accessoriesDataScrollDownKey || !panels.missing.scroll(1);
            });
        });
    }

    private void render(MinecraftClient client, GenericContainerScreen bag, DrawContext context, int mouseX, int mouseY) {
        panels.missing.clearBounds();
        if (!scan.active(client, bag)) return;

        var settings = ClientConfig.configuration().accessories;
        var snapshot = NefRepo.accessories().snapshot();
        Catalog catalog = snapshot.data();
        boolean scanned = scan.complete(bag);
        if (scanned && catalog != null && (lastCatalog != catalog || lastRevision != scan.revision())) {
            estimate = AccessoriesData.estimate(catalog, scan.items());
            lastCatalog = catalog;
            lastRevision = scan.revision();
        }

        String status = switch (snapshot.status()) {
            case LOADING -> "Loading accessory catalog…";
            case UNAVAILABLE -> "Accessory catalog unavailable.";
            case READY -> scan.reading() ? "Reading accessory bag…" : "Visit every page to see your results.";
        };
        panels.draw(context, client.textRenderer, bag, settings, scanned && catalog != null ? estimate : null,
                status, mouseX, mouseY);
    }
}
