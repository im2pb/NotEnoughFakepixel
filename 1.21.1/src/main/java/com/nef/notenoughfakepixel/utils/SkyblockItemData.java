package com.nef.notenoughfakepixel.utils;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class SkyblockItemData {
    private SkyblockItemData() {
    }

    public static String id(ItemStack stack) {
        return id(stack.get(DataComponentTypes.CUSTOM_DATA));
    }

    public static boolean isAspect(String id) {
        return "ASPECT_OF_THE_END".equals(id) || "ASPECT_OF_THE_VOID".equals(id);
    }

    public static String id(NbtComponent customData) {
        if (customData == null) return "";

        NbtCompound nbt = customData.copyNbt();
        String id = nbt.getCompound("ExtraAttributes").getString("id");
        return id.isEmpty() ? nbt.getString("id") : id;
    }
}
