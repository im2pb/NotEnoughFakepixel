package com.nef.notenoughfakepixel.features.skyblock.slotlocking;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.mixin.HandledScreenAccessor;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.Logger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.nef.notenoughfakepixel.features.skyblock.slotlocking.SlotLockStore.HOTBAR_SIZE;

public final class SlotLocking implements ClientModInitializer {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("notenoughfakepixel.slot_locking");
    private static final Identifier LOCK = Identifier.of("notenoughfakepixel", "slotlocking/lock.png");
    private static final Identifier BOUND = Identifier.of("notenoughfakepixel", "slotlocking/bound.png");
    private static final Pattern STORAGE = Pattern.compile(
            "Storage|.+ Backpack (?:✦ )?\\(Slot #\\d+\\)|Ender Chest \\(Page \\d+\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRADE = Pattern.compile(
            "Trade(?: Menu)?|(?:Trade|Trading) with .+|Trade: .+", Pattern.CASE_INSENSITIVE);
    private static final int LINK_COLOR = 0xFF33EEDD;

    @RegisterInstance
    public static final SlotLocking INSTANCE = new SlotLocking(new SlotLockStore(
            FabricLoader.getInstance().getConfigDir().resolve("Notenoughfakepixel/slotlocking.json")));

    private final SlotLockStore store;
    private Edit edit;
    private RecentSelection recentSelection;

    private SlotLocking(SlotLockStore store) {
        this.store = store;
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                store.load();
            } catch (IOException exception) {
                LOG.warn("Could not read slot locking data.", exception);
            }
        });
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            edit = null;
            recentSelection = null;
        });
        ScreenEvents.AFTER_INIT.register((client, current, width, height) -> {
            if (!(current instanceof HandledScreen<?> screen)) return;

            if (edit != null && edit.screen() == screen) edit = null;
            ScreenEvents.remove(screen).register(removed -> {
                if (edit != null && edit.screen() == screen) edit = null;
            });
            ScreenKeyboardEvents.allowKeyPress(screen).register((ignored, key, scanCode, modifiers) ->
                    !beginEdit(client, screen, key));
            ScreenKeyboardEvents.allowKeyRelease(screen).register((ignored, key, scanCode, modifiers) ->
                    !finishEdit(client, screen, key));
        });
    }

    private boolean beginEdit(MinecraftClient client, HandledScreen<?> screen, int key) {
        var settings = settings(client);
        if (settings == null || key == GLFW.GLFW_KEY_UNKNOWN || key != settings.slotLockKey) return false;
        if (edit != null) return true;
        if (!screen.getScreenHandler().getCursorStack().isEmpty()) return false;

        Slot slot = hovered(client, screen);
        int index = inventoryIndex(slot, client.player.getInventory());
        if (index < 0) return false;

        edit = new Edit(screen, slot, index, key, profile());
        return true;
    }

    private boolean finishEdit(MinecraftClient client, HandledScreen<?> screen, int key) {
        Edit gesture = edit;
        if (gesture == null || gesture.screen() != screen || gesture.key() != key) return false;
        edit = null;

        var settings = settings(client);
        if (settings == null || !gesture.profile().equals(profile())
                || !screen.getScreenHandler().getCursorStack().isEmpty()) return true;

        int target = inventoryIndex(hovered(client, screen), client.player.getInventory());
        try {
            if (settings.enableSlotBinding && SlotLockStore.canBind(gesture.index(), target)) {
                if (store.bind(gesture.profile(), gesture.index(), target)) playSound(client, settings, true);
            } else {
                playSound(client, settings, store.toggle(gesture.profile(), gesture.index()));
            }
        } catch (IOException exception) {
            saveFailed(exception);
        }
        return true;
    }

    public Click checkClick(int syncId, int slotId, int button, SlotActionType action, PlayerEntity player) {
        var original = new Click(slotId, button, action);
        var client = MinecraftClient.getInstance();
        if (client.player != player) return original;
        var settings = settings(client);
        ScreenHandler handler = player.currentScreenHandler;
        if (settings == null || handler.syncId != syncId) return original;

        PlayerInventory inventory = player.getInventory();
        Slot slot = slotId >= 0 && slotId < handler.slots.size() ? handler.getSlot(slotId) : null;
        int index = inventoryIndex(slot, inventory);
        String profile = profile();
        var entry = store.entry(profile, index);
        if (entry.locked()) return null;
        if (action == SlotActionType.QUICK_MOVE && settings.enableSlotBinding && entry.partner() >= 0) {
            var swap = boundSwap(handler, inventory, index, entry.partner());
            if (swap != null) return swap;
        }
        if (protectedSlot(entry, settings)) return null;
        if (action == SlotActionType.SWAP && button >= 0 && button < HOTBAR_SIZE
                && protectedSlot(store.entry(profile, button), settings)) return null;
        if (action == SlotActionType.PICKUP_ALL && collectsLockedStack(handler, player, profile, settings)) return null;
        return original;
    }

    private static Click boundSwap(ScreenHandler handler, PlayerInventory inventory, int index, int partner) {
        int hotbar = Math.min(index, partner);
        int storage = Math.max(index, partner);
        if (handler.getSlotIndex(inventory, hotbar).isEmpty()) return null;
        var slotId = handler.getSlotIndex(inventory, storage);
        if (slotId.isEmpty() || !handler.getSlot(slotId.getAsInt()).isEnabled()) return null;
        return new Click(slotId.getAsInt(), hotbar, SlotActionType.SWAP);
    }

    private boolean collectsLockedStack(ScreenHandler handler, PlayerEntity player, String profile,
                                        com.nef.notenoughfakepixel.config.features.SlotLocking settings) {
        ItemStack cursor = handler.getCursorStack();
        if (cursor.isEmpty() || cursor.getCount() >= cursor.getMaxCount()) return false;

        for (Slot slot : handler.slots) {
            int index = inventoryIndex(slot, player.getInventory());
            if (index >= 0 && protectedSlot(store.entry(profile, index), settings) && slot.hasStack()
                    && ItemStack.areItemsAndComponentsEqual(cursor, slot.getStack()) && slot.canTakeItems(player)
                    && handler.canInsertIntoSlot(cursor, slot)) return true;
        }
        return false;
    }

    public void selectingHotbarSlot(PlayerInventory inventory, int next) {
        var client = MinecraftClient.getInstance();
        if (next == inventory.selectedSlot || client == null || client.player == null
                || inventory != client.player.getInventory()) return;

        var settings = settings(client);
        String profile = profile();
        int index = inventory.selectedSlot;
        if (settings != null && settings.slotLockSwapDelay > 0 && protectedSlot(store.entry(profile, index), settings)) {
            recentSelection = new RecentSelection(profile, index, System.nanoTime());
        }
    }

    public boolean blocksDrop(PlayerInventory inventory) {
        var settings = settings(MinecraftClient.getInstance());
        if (settings == null) return false;
        String profile = profile();
        if (protectedSlot(store.entry(profile, inventory.selectedSlot), settings)) return true;
        return recentSelection != null && recentSelection.profile().equals(profile)
                && System.nanoTime() - recentSelection.time() < TimeUnit.MILLISECONDS.toNanos(settings.slotLockSwapDelay)
                && protectedSlot(store.entry(profile, recentSelection.index()), settings);
    }

    public boolean protectsInventoryIndex(int index) {
        var settings = settings(MinecraftClient.getInstance());
        return settings != null && protectedSlot(store.entry(profile(), index), settings);
    }

    public boolean blocksRecipeFill(PlayerInventory inventory, RecipeEntry<?> recipe) {
        var settings = settings(MinecraftClient.getInstance());
        if (settings == null) return false;

        String profile = profile();
        var ingredients = recipe.value().getIngredients();
        for (int index = 0; index < 36; index++) {
            if (!protectedSlot(store.entry(profile, index), settings)) continue;

            ItemStack stack = inventory.getStack(index);
            if (!stack.isEmpty() && ingredients.stream().anyMatch(ingredient -> ingredient.test(stack))) return true;
        }
        return false;
    }

    private static boolean protectedSlot(SlotLockStore.Entry entry, com.nef.notenoughfakepixel.config.features.SlotLocking settings) {
        return entry.locked() || (settings.enableSlotBinding && settings.bindingAlsoLocks && entry.partner() >= 0);
    }

    private static com.nef.notenoughfakepixel.config.features.SlotLocking settings(MinecraftClient client) {
        if (client == null || client.player == null || !SkyblockData.isSkyblock()
                || client.currentScreen instanceof CreativeInventoryScreen) return null;

        var settings = ClientConfig.configuration().sl;
        if (!settings.enableSlotLocking) return null;
        if (client.currentScreen instanceof GenericContainerScreen screen) {
            String title = Formatting.strip(screen.getTitle().getString()).strip();
            if ((settings.disableInStorage && STORAGE.matcher(title).matches())
                    || (!settings.lockSlotsInTrade && TRADE.matcher(title).matches())) return null;
        }
        return settings;
    }

    private static String profile() {
        String name = SkyblockData.currentProfile();
        return name == null || name.isBlank() ? "generic" : name;
    }

    private static int inventoryIndex(Slot slot, PlayerInventory inventory) {
        if (slot == null || slot.inventory != inventory || !slot.isEnabled()) return -1;
        int index = slot.getIndex();
        return SlotLockStore.valid(index) ? index : -1;
    }

    private static Slot hovered(MinecraftClient client, HandledScreen<?> screen) {
        var window = client.getWindow();
        double x = client.mouse.getX() * window.getScaledWidth() / window.getWidth();
        double y = client.mouse.getY() * window.getScaledHeight() / window.getHeight();
        return ((HandledScreenAccessor) screen).nef$slotAt(x, y);
    }

    public void draw(DrawContext context, HandledScreen<?> screen, int mouseX, int mouseY) {
        var client = MinecraftClient.getInstance();
        var settings = settings(client);
        if (settings == null) return;

        var accessor = (HandledScreenAccessor) screen;
        int left = accessor.nef$left();
        int top = accessor.nef$top();
        Slot hovered = accessor.nef$slotAt(mouseX, mouseY);
        PlayerInventory inventory = client.player.getInventory();
        String profile = profile();
        Edit gesture = edit != null && edit.screen() == screen && edit.profile().equals(profile) ? edit : null;
        boolean preview = settings.enableSlotBinding && gesture != null && gesture.index() >= HOTBAR_SIZE;

        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(left, top, 250);
            if (preview) {
                drawLink(context, gesture.slot().x + 8, gesture.slot().y + 8, mouseX - left, mouseY - top);
            } else if (settings.enableSlotBinding && hovered != null) {
                int partner = store.entry(profile, inventoryIndex(hovered, inventory)).partner();
                if (partner >= 0) {
                    screen.getScreenHandler().getSlotIndex(inventory, partner).ifPresent(id -> {
                        Slot paired = screen.getScreenHandler().getSlot(id);
                        if (paired.isEnabled()) drawLink(context, hovered.x + 8, hovered.y + 8, paired.x + 8, paired.y + 8);
                    });
                }
            }

            for (Slot slot : screen.getScreenHandler().slots) {
                int index = inventoryIndex(slot, inventory);
                if (index < 0) continue;
                var state = store.entry(profile, index);
                if (preview && slot != gesture.slot()) {
                    boolean destination = index < HOTBAR_SIZE && !state.locked();
                    int tint = destination ? slot == hovered ? 0x7733EEDD : 0x33FFFFFF : 0x88000000;
                    context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, tint);
                } else if (slot == hovered && protectedSlot(state, settings)) {
                    context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x70FF8080);
                }
                if (state.locked()) drawIcon(context, LOCK, slot, 0.5F);
                else if (settings.enableSlotBinding && state.partner() >= 0) drawIcon(context, BOUND, slot, 0.7F);
            }
        } finally {
            matrices.pop();
        }
    }

    private static void drawLink(DrawContext context, int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int length = (int) Math.hypot(dx, dy);
        if (length < 9) return;

        var matrices = context.getMatrices();
        matrices.push();
        try {
            matrices.translate(x1, y1, 0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.atan2(dy, dx)));
            context.fill(8, 0, length, 1, LINK_COLOR);
        } finally {
            matrices.pop();
        }
    }

    private static void drawIcon(DrawContext context, Identifier texture, Slot slot, float alpha) {
        context.drawTexturedQuad(texture, slot.x, slot.x + 16, slot.y, slot.y + 16, 0,
                0, 1, 0, 1, 1, 1, 1, alpha);
    }

    private static void playSound(MinecraftClient client, com.nef.notenoughfakepixel.config.features.SlotLocking settings, boolean locked) {
        float volume = Math.clamp(settings.slotLockSoundVol / 100.0F, 0, 1);
        if (settings.slotLockSound && volume > 0) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    locked ? 0.943F : 0.1F, volume));
        }
    }

    public static void confirmReset() {
        var client = MinecraftClient.getInstance();
        var parent = client.currentScreen;
        String profile = profile();
        client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                try {
                    INSTANCE.store.reset(profile);
                    Logger.feedback("Slot locks and bindings reset for " + profile + ".");
                } catch (IOException exception) {
                    saveFailed(exception);
                }
            }
            client.setScreen(parent);
        }, Text.literal("Reset locked slots?"), Text.literal("Clear all slot locks and bindings for " + profile + "?"),
                Text.literal("Reset"), Text.translatable("gui.cancel")));
    }

    private static void saveFailed(IOException exception) {
        LOG.warn("Could not save slot locking data.", exception);
        Logger.feedbackError("Could not save slot locks. See the client log.");
    }

    public record Click(int slotId, int button, SlotActionType action) {
    }

    private record Edit(HandledScreen<?> screen, Slot slot, int index, int key, String profile) {
    }

    private record RecentSelection(String profile, int index, long time) {
    }
}
