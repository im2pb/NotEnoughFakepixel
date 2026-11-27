package com.nef.notenoughfakepixel.features.skyblock.accessories;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.nefrepo.NefRepo;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Catalog;
import com.nef.notenoughfakepixel.serverdata.AccessoriesData.Estimate;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;

import java.util.List;
import java.util.Objects;

public final class MissingAccessories implements ClientModInitializer {
    private static final int QUIET_TICKS = 2;
    @RegisterInstance
    public static final MissingAccessories INSTANCE = new MissingAccessories();

    private final AccessoryBagScan scan = new AccessoryBagScan();
    private final AccessoriesPanels panels = new AccessoriesPanels();
    private GenericContainerScreen screen;
    private ClientWorld world;
    private String profile;
    private ItemStack[] slots;
    private int unseen;
    private int quietTicks;
    private boolean replacement;
    private boolean fullUpdate;
    private Catalog lastCatalog;
    private long lastRevision = -1;
    private Estimate estimate;

    private MissingAccessories() {
    }

    @Override
    public void onInitializeClient() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newWorld) -> closeBag());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::closeBag));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ScreenEvents.AFTER_INIT.register((client, current, width, height) -> {
            if (!(current instanceof GenericContainerScreen bag) || !isBag(bag)) return;

            ScreenEvents.afterRender(bag).register((ignored, context, x, y, delta) -> render(client, bag, context, x, y));
            ScreenMouseEvents.allowMouseScroll(bag).register((ignored, x, y, horizontal, vertical) ->
                    !active(client, bag) || !panels.scrollAt(x, y, vertical));
            ScreenKeyboardEvents.allowKeyPress(bag).register((ignored, key, scanCode, modifiers) -> {
                if (!active(client, bag)) return true;
                var settings = ClientConfig.configuration().accessories;
                if (key == settings.accessoriesDataScrollUpKey && panels.scroll(-1)) return false;
                return key != settings.accessoriesDataScrollDownKey || !panels.scroll(1);
            });
        });
    }

    public void receiveContents(MinecraftClient client, int syncId, List<ItemStack> contents) {
        if (!accepts(client, syncId) || contents.size() < slots.length) return;
        for (int slot = 0; slot < slots.length; slot++) slots[slot] = contents.get(slot).copy();
        unseen = 0;
        fullUpdate = true;
        quietTicks = QUIET_TICKS;
    }

    public void receiveSlot(MinecraftClient client, int syncId, int slot, ItemStack stack) {
        if (syncId <= 0 || slot < 0 || !accepts(client, syncId) || slot >= slots.length) return;
        if (slots[slot] == null) unseen--;
        slots[slot] = stack.copy();
        quietTicks = QUIET_TICKS;
    }

    public void clickedSlot(MinecraftClient client, int syncId, int slot) {
        if (slot < 0 || !accepts(client, syncId) || slot >= slots.length || unseen != 0) return;
        var direction = AccessoryBagScan.navigation(slots[slot]);
        if (direction != null) {
            if (quietTicks > 0 && (fullUpdate || !scan.awaitingPage())) readPage();
            scan.navigate(direction, Util.getMeasuringTimeMs());
            fullUpdate = false;
            quietTicks = 0;
        }
    }

    public void closeBag() {
        screen = null;
        world = null;
        profile = null;
        slots = null;
        quietTicks = 0;
        replacement = false;
        fullUpdate = false;
        scan.clear();
        panels.reset();
        lastCatalog = null;
        estimate = null;
    }

    private void tick(MinecraftClient client) {
        boolean inBag = isBag(client.currentScreen);
        if (screen == null && !inBag) return;

        updateContext(client);
        scan.expireNavigation(Util.getMeasuringTimeMs());
        boolean enabled = ClientConfig.configuration().accessories.enable;
        if (!enabled || client.world == null || client.player == null || !inBag) {
            if (screen != null && (client.currentScreen != null || !scan.awaitingPage() || !enabled)) closeBag();
            return;
        }

        if (screen != client.currentScreen) return;
        if (quietTicks > 0 && --quietTicks == 0 && unseen == 0) {
            readPage();
        }
    }

    private void readPage() {
        scan.receive(AccessoryBagScan.Page.read(slots), replacement);
        replacement = false;
        fullUpdate = false;
    }

    private boolean accepts(MinecraftClient client, int syncId) {
        if (syncId <= 0 || client.player == null || client.world == null
                || !(client.currentScreen instanceof GenericContainerScreen bag) || !isBag(bag)
                || bag.getScreenHandler().syncId != syncId || bag.getScreenHandler() != client.player.currentScreenHandler
                || !ClientConfig.configuration().accessories.enable) return false;

        updateContext(client);
        if (screen != bag) {
            replacement = screen != null;
            screen = bag;
            slots = new ItemStack[bag.getScreenHandler().getInventory().size()];
            unseen = slots.length;
            quietTicks = 0;
            fullUpdate = false;
        }
        return true;
    }

    private void updateContext(MinecraftClient client) {
        String currentProfile = SkyblockData.currentProfile();
        if (world != client.world || !Objects.equals(profile, currentProfile)) {
            closeBag();
            world = client.world;
            profile = currentProfile;
        }
    }

    private static boolean isBag(Screen screen) {
        return screen instanceof GenericContainerScreen bag
                && AccessoriesData.clean(bag.getTitle().getString()).equals("Accessory Bag");
    }

    private boolean active(MinecraftClient client, GenericContainerScreen bag) {
        return client.currentScreen == bag && SkyblockData.isSkyblock()
                && world == client.world && Objects.equals(profile, SkyblockData.currentProfile())
                && ClientConfig.configuration().accessories.enable;
    }

    private void render(MinecraftClient client, GenericContainerScreen bag, DrawContext context, int mouseX, int mouseY) {
        panels.clearBounds();
        if (!active(client, bag)) return;

        var settings = ClientConfig.configuration().accessories;
        var snapshot = NefRepo.accessories().snapshot();
        Catalog catalog = snapshot.data();
        boolean scanned = screen == bag && quietTicks == 0 && scan.complete();
        if (scanned && catalog != null && (estimate == null || lastCatalog != catalog || lastRevision != scan.revision())) {
            estimate = AccessoriesData.estimate(catalog, scan.accessories());
            lastCatalog = catalog;
            lastRevision = scan.revision();
        }

        String status = switch (snapshot.status()) {
            case LOADING -> "Loading accessory catalog…";
            case UNAVAILABLE -> "Accessory catalog unavailable.";
            case READY -> quietTicks > 0 ? "Reading accessory bag…" : "Visit every page to see your results.";
        };
        panels.draw(context, client.textRenderer, bag, settings, scanned && catalog != null ? estimate : null,
                status, mouseX, mouseY);
    }
}
