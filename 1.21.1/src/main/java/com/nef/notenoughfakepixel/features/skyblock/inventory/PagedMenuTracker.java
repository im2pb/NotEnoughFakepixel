package com.nef.notenoughfakepixel.features.skyblock.inventory;

import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PagedMenuTracker<T> {
    private static final int QUIET_TICKS = 2;
    private static final List<PagedMenuTracker<?>> TRACKERS = new ArrayList<>();

    private final PagedMenuScan<T> scan = new PagedMenuScan<>();
    private final Predicate<String> title;
    private final BooleanSupplier enabled;
    private final Function<ItemStack, T> parser;
    private GenericContainerScreen screen;
    private ClientWorld world;
    private String profile;
    private ItemStack[] slots;
    private int unseen;
    private int quietTicks;
    private boolean replacement;
    private boolean fullUpdate;

    public PagedMenuTracker(Predicate<String> title, BooleanSupplier enabled, Function<ItemStack, T> parser) {
        this.title = title;
        this.enabled = enabled;
        this.parser = parser;
    }

    public void register() {
        TRACKERS.add(this);
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newWorld) -> close());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(this::close));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public boolean matches(Screen candidate) {
        return candidate instanceof GenericContainerScreen menu
                && title.test(Formatting.strip(menu.getTitle().getString()).strip());
    }

    public boolean active(MinecraftClient client, GenericContainerScreen menu) {
        return client.currentScreen == menu && SkyblockData.isSkyblock() && world == client.world
                && Objects.equals(profile, SkyblockData.currentProfile()) && enabled.getAsBoolean();
    }

    public boolean complete(GenericContainerScreen menu) {
        return screen == menu && quietTicks == 0 && scan.complete();
    }

    public boolean reading() {
        return quietTicks > 0;
    }

    public boolean navigating() {
        return scan.awaitingPage();
    }

    public long revision() {
        return scan.revision();
    }

    public List<T> items() {
        return scan.items();
    }

    public static void contentsReceived(MinecraftClient client, int syncId, List<ItemStack> contents) {
        if (syncId <= 0) return;
        for (var tracker : TRACKERS) tracker.receiveContents(client, syncId, contents);
    }

    public static void slotReceived(MinecraftClient client, int syncId, int slot, ItemStack stack) {
        if (syncId <= 0 || slot < 0) return;
        for (var tracker : TRACKERS) tracker.receiveSlot(client, syncId, slot, stack);
    }

    public static void slotClicked(MinecraftClient client, int syncId, int slot) {
        if (syncId <= 0 || slot < 0) return;
        for (var tracker : TRACKERS) tracker.clickedSlot(client, syncId, slot);
    }

    public static void menuClosed() {
        for (var tracker : TRACKERS) tracker.close();
    }

    private void receiveContents(MinecraftClient client, int syncId, List<ItemStack> contents) {
        if (!accepts(client, syncId) || contents.size() < slots.length) return;
        for (int slot = 0; slot < slots.length; slot++) slots[slot] = contents.get(slot).copy();
        unseen = 0;
        fullUpdate = true;
        quietTicks = QUIET_TICKS;
    }

    private void receiveSlot(MinecraftClient client, int syncId, int slot, ItemStack stack) {
        if (!accepts(client, syncId) || slot >= slots.length) return;
        if (slots[slot] == null) unseen--;
        slots[slot] = stack.copy();
        quietTicks = QUIET_TICKS;
    }

    private void clickedSlot(MinecraftClient client, int syncId, int slot) {
        if (!accepts(client, syncId) || slot >= slots.length || unseen != 0) return;
        var direction = PagedMenuScan.navigation(slots[slot]);
        if (direction != null) {
            if (quietTicks > 0 && (fullUpdate || !scan.awaitingPage())) readPage();
            scan.navigate(direction, Util.getMeasuringTimeMs());
            fullUpdate = false;
            quietTicks = 0;
        }
    }

    private void close() {
        screen = null;
        world = null;
        profile = null;
        slots = null;
        quietTicks = 0;
        replacement = false;
        fullUpdate = false;
        scan.clear();
    }

    private void tick(MinecraftClient client) {
        boolean inMenu = matches(client.currentScreen);
        if (screen == null && !inMenu) return;

        updateContext(client);
        scan.expireNavigation(Util.getMeasuringTimeMs());
        boolean tracking = enabled.getAsBoolean();
        if (!tracking || client.world == null || client.player == null || !inMenu) {
            if (screen != null && (client.currentScreen != null || !scan.awaitingPage() || !tracking)) close();
            return;
        }

        if (screen == client.currentScreen && quietTicks > 0 && --quietTicks == 0 && unseen == 0) readPage();
    }

    private void readPage() {
        scan.receive(PagedMenuScan.Page.read(slots, parser), replacement);
        replacement = false;
        fullUpdate = false;
    }

    private boolean accepts(MinecraftClient client, int syncId) {
        if (client.player == null || client.world == null || !(client.currentScreen instanceof GenericContainerScreen menu)
                || !matches(menu) || menu.getScreenHandler().syncId != syncId
                || menu.getScreenHandler() != client.player.currentScreenHandler || !enabled.getAsBoolean()) return false;

        updateContext(client);
        if (screen != menu) {
            replacement = screen != null;
            screen = menu;
            slots = new ItemStack[menu.getScreenHandler().getInventory().size()];
            unseen = slots.length;
            quietTicks = 0;
            fullUpdate = false;
        }
        return true;
    }

    private void updateContext(MinecraftClient client) {
        String currentProfile = SkyblockData.currentProfile();
        if (world != client.world || !Objects.equals(profile, currentProfile)) {
            close();
            world = client.world;
            profile = currentProfile;
        }
    }
}
