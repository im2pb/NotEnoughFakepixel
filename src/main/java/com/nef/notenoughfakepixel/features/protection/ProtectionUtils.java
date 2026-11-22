/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.protection;

import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

public final class ProtectionUtils {
    private ProtectionUtils() {}

    public static UUID getItemUuid(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !stack.hasTagCompound()) return null;
        if (!stack.getTagCompound().hasKey("ExtraAttributes", 10)) return null;
        net.minecraft.nbt.NBTTagCompound extra = stack.getTagCompound().getCompoundTag("ExtraAttributes");
        String value = extra.hasKey("uuid", 8) ? extra.getString("uuid") : extra.hasKey("UUID", 8) ? extra.getString("UUID") : "";
        if (value.isEmpty()) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; }
    }

    public static String cleanTitle(String title) {
        return StringUtils.stripControlCodes(title == null ? "" : title).trim().toLowerCase(Locale.ROOT);
    }

    public static String displayName(ItemStack stack) {
        if (stack == null) return "";
        return StringUtils.stripControlCodes(stack.getDisplayName() == null ? "" : stack.getDisplayName()).trim().toLowerCase(Locale.ROOT);
    }

    public static boolean protectedItem(ItemStack stack, ProtectedItemStore store) {
        UUID uuid = getItemUuid(stack);
        return uuid != null && store != null && store.contains(uuid);
    }
}
