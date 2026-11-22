/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.inventory.equipment;

import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persisted equipment snapshot. The JSON field remains named "items" so existing equipments.json files
 * written by v34 remain readable. Keys 0..3 are stable display positions, not live container slot ids.
 */
public class EquipmentData {
    private static final int FIRST_EQUIPMENT_CONTAINER_SLOT = 10;
    private static final int EQUIPMENT_CONTAINER_SLOT_STEP = 9;
    private static final int EQUIPMENT_SLOT_COUNT = 4;

    public Map<Integer, String> items = new LinkedHashMap<>();

    /** Gson constructor. */
    public EquipmentData() { }

    /** Compatibility constructor used by callers that already have the live container slot map. */
    public EquipmentData(Map<Integer, ItemStack> map) {
        items = new LinkedHashMap<>();
        if (map == null || map.isEmpty()) return;

        TreeMap<Integer, ItemStack> ordered = new TreeMap<>(map);
        int displayIndex = 0;
        for (Map.Entry<Integer, ItemStack> entry : ordered.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack != null) items.put(displayIndex, itemStackToJson(stack));
            displayIndex++;
        }
    }

    /** Capture the four equipment slots in a fixed, repeatable order. */
    public static EquipmentData fromContainer(ContainerChest container) {
        EquipmentData snapshot = new EquipmentData();
        if (container == null || container.inventorySlots == null) return snapshot;

        for (int displayIndex = 0; displayIndex < EQUIPMENT_SLOT_COUNT; displayIndex++) {
            int containerSlot = FIRST_EQUIPMENT_CONTAINER_SLOT + displayIndex * EQUIPMENT_CONTAINER_SLOT_STEP;
            if (containerSlot < 0 || containerSlot >= container.inventorySlots.size()) continue;
            ItemStack stack = container.getSlot(containerSlot).getStack();
            if (stack != null) snapshot.items.put(displayIndex, itemStackToJson(stack));
        }
        return snapshot;
    }

    public static String itemStackToJson(ItemStack stack) {
        if (stack == null) return null;
        NBTTagCompound nbt = new NBTTagCompound();
        stack.writeToNBT(nbt);
        return nbt.toString();
    }

    public static ItemStack jsonToItemStack(String json) throws NBTException {
        if (json == null || json.trim().isEmpty()) return null;
        NBTTagCompound nbt = net.minecraft.nbt.JsonToNBT.getTagFromJson(json);
        return ItemStack.loadItemStackFromNBT(nbt);
    }

    /** Returns the current snapshot in display order, tolerating malformed legacy entries. */
    public Map<Integer, ItemStack> getItemStacks() {
        Map<Integer, ItemStack> result = new LinkedHashMap<>();
        if (items == null || items.isEmpty()) return result;

        TreeMap<Integer, String> ordered = new TreeMap<>(items);
        for (Map.Entry<Integer, String> entry : ordered.entrySet()) {
            try {
                ItemStack stack = jsonToItemStack(entry.getValue());
                if (stack != null) result.put(entry.getKey(), stack);
            } catch (NBTException ignored) {
                // One damaged entry should not hide the remaining equipment slots.
            }
        }
        return result;
    }

    public void normalize() {
        if (items == null) {
            items = new LinkedHashMap<>();
            return;
        }
        TreeMap<Integer, String> ordered = new TreeMap<>(items);
        items = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : ordered.entrySet()) {
            if (entry.getKey() != null && entry.getKey() >= 0 && entry.getKey() < EQUIPMENT_SLOT_COUNT
                    && entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                items.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public boolean hasSameItems(EquipmentData other) {
        if (other == null) return false;
        normalize();
        other.normalize();
        return items.equals(other.items);
    }
}
