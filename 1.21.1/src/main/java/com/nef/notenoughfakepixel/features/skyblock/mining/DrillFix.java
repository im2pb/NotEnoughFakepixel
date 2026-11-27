package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterInstance;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;

public final class DrillFix implements ClientModInitializer {
    @RegisterInstance
    public static final DrillFix INSTANCE = new DrillFix();

    private PendingFuel pending;

    private DrillFix() {
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(this::releaseIfReady);
        ScreenEvents.BEFORE_INIT.register((client, screen, width, height) -> releaseIfReady(client));
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> pending = null);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> pending = null));
    }

    public ItemStack screenStack(MinecraftClient client, ScreenHandler handler, int slot, ItemStack incoming) {
        if (client.player == null || slot < 0 || slot >= handler.slots.size()) return incoming;
        var target = handler.getSlot(slot);
        return target.inventory instanceof PlayerInventory inventory
                ? inventoryStack(client, inventory, target.getIndex(), incoming) : incoming;
    }

    public List<ItemStack> screenContents(MinecraftClient client, ScreenHandler handler, List<ItemStack> contents) {
        if (client.player == null || (pending == null && !holdingAttack(client))) return contents;

        PlayerInventory inventory = client.player.getInventory();
        int selected = inventory.selectedSlot;
        int slot = handler.getSlotIndex(inventory, selected).orElse(-1);
        if (slot < 0 || slot >= contents.size()) return contents;

        ItemStack incoming = contents.get(slot);
        ItemStack replacement = inventoryStack(client, inventory, selected, incoming);
        if (replacement == incoming) return contents;

        var filtered = new ArrayList<>(contents);
        filtered.set(slot, replacement);
        return filtered;
    }

    public ItemStack inventoryStack(MinecraftClient client, PlayerInventory inventory, int slot, ItemStack incoming) {
        if (client.player == null || inventory != client.player.getInventory() || !PlayerInventory.isValidHotbarIndex(slot)) {
            return incoming;
        }

        releaseIfReady(client);
        if (inventory.selectedSlot != slot || !holdingAttack(client)) return incoming;

        ItemStack current = inventory.getStack(slot);
        if (!onlyFuelChanged(current, incoming)) {
            pending = null;
            return incoming;
        }

        ItemStack original = pending == null ? current.copy() : pending.original();
        pending = new PendingFuel(client.player, client.world, slot, current, original, incoming);
        return current;
    }

    ItemStack latestHeldStack(MinecraftClient client) {
        ItemStack held = client.player.getMainHandStack();
        PendingFuel update = pending;
        if (update != null && update.player() == client.player && update.world() == client.world
                && update.slot() == client.player.getInventory().selectedSlot && update.matches(held)) {
            return update.latest();
        }
        return held;
    }

    private void releaseIfReady(MinecraftClient client) {
        PendingFuel update = pending;
        if (update == null) return;
        if (update.player() != client.player || update.world() != client.world) {
            pending = null;
            return;
        }

        PlayerInventory inventory = update.player().getInventory();
        ItemStack current = inventory.getStack(update.slot());
        if (!update.matches(current)) {
            pending = null;
            return;
        }
        if (inventory.selectedSlot == update.slot() && holdingAttack(client)) return;

        pending = null;
        inventory.setStack(update.slot(), update.latest());
    }

    private static boolean holdingAttack(MinecraftClient client) {
        return client.world != null && client.currentScreen == null && client.options.attackKey.isPressed()
                && SkyblockData.isSkyblock() && ClientConfig.configuration().mining.miningDrillFix;
    }

    static boolean onlyFuelChanged(ItemStack current, ItemStack incoming) {
        if (current.isEmpty() || incoming.isEmpty() || !ItemStack.areItemsEqual(current, incoming)
                || current.getCount() != incoming.getCount() || ItemStack.areEqual(current, incoming)) return false;

        if (!DrillFuel.isDrill(current) || !DrillFuel.isDrill(incoming)) return false;

        return ItemStack.areEqual(withoutFuel(current), withoutFuel(incoming));
    }

    private static ItemStack withoutFuel(ItemStack stack) {
        ItemStack comparison = stack.copy();
        var lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            var lines = lore.lines().stream().filter(line -> !DrillFuel.isFuelLine(line)).toList();
            if (lines.size() != lore.lines().size()) {
                if (lines.isEmpty()) comparison.remove(DataComponentTypes.LORE);
                else comparison.set(DataComponentTypes.LORE, new LoreComponent(lines));
            }
        }

        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) {
            var nbt = data.copyNbt();
            boolean changed = nbt.contains("fuel");
            nbt.remove("fuel");
            if (nbt.contains("ExtraAttributes", NbtElement.COMPOUND_TYPE)) {
                var attributes = nbt.getCompound("ExtraAttributes");
                if (attributes.contains("fuel")) {
                    changed = true;
                    attributes.remove("fuel");
                    if (attributes.isEmpty()) nbt.remove("ExtraAttributes");
                }
            }
            if (changed) {
                if (nbt.isEmpty()) comparison.remove(DataComponentTypes.CUSTOM_DATA);
                else comparison.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            }
        }
        return comparison;
    }

    private record PendingFuel(ClientPlayerEntity player, ClientWorld world, int slot, ItemStack held,
                               ItemStack original, ItemStack latest) {
        boolean matches(ItemStack stack) {
            return stack == held && ItemStack.areEqual(stack, original);
        }
    }
}
