/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import com.nef.notenoughfakepixel.variables.Rarity;
import com.nef.notenoughfakepixel.variables.Skins;
import com.nef.notenoughfakepixel.variables.StackingEnchant;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class ItemUtils {

    public static String[] getLoreFromNBT(NBTTagCompound tag) {
        return ItemUtils.getLore(tag).toArray(new String[0]);
    }

    public static @NotNull List<@NotNull String> getLore(@Nullable NBTTagCompound tagCompound) {
        if (tagCompound == null) {
            return Collections.emptyList();
        }
        NBTTagList tagList = tagCompound.getCompoundTag("display").getTagList("Lore", 8);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < tagList.tagCount(); i++) {
            list.add(tagList.getStringTagAt(i));
        }
        return list;
    }

    public static String getInternalName(@Nullable ItemStack item) {
        if (item == null || item.stackSize <= 0) return "";
        if (!item.hasTagCompound()) return "";
        if (!item.getTagCompound().hasKey("ExtraAttributes")) return "";

        NBTTagCompound extraAttributes = item.getTagCompound().getCompoundTag("ExtraAttributes");
        if (!extraAttributes.hasKey("id")) return "";

        return extraAttributes.getString("id");
    }

    public static @NotNull NBTTagCompound getExtraAttributes(ItemStack itemStack) {
        NBTTagCompound tag = getOrCreateTag(itemStack);
        NBTTagCompound extraAttributes = tag.getCompoundTag("ExtraAttributes");
        tag.setTag("ExtraAttributes", extraAttributes);
        return extraAttributes;
    }

    public static int getExtraAttributesIntTag(ItemStack item, String tag) {
        NBTTagCompound extraAttributes = getExtraAttributes(item);
        if (!extraAttributes.hasKey(tag)) return -1;
        return extraAttributes.getInteger(tag);
    }

    public static String getExtraAttributesStringTag(ItemStack item, String tag) {
        NBTTagCompound extraAttributes = getExtraAttributes(item);
        if (!extraAttributes.hasKey(tag)) return "";
        return extraAttributes.getString(tag);
    }

    public static List<ItemStack> getAllCustomSkulls(Map<String, String> skullIcons) {
        List<ItemStack> skulls = new ArrayList<>();
        for (Map.Entry<String, String> entry : skullIcons.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue().replace("skull:", "").trim();
            skulls.add(createSkullWithTexture(name, value));
        }
        return skulls;
    }

    public static String getSkullTexture(ItemStack itemStack) {
        if (!itemStack.hasTagCompound()) return "";
        if (!itemStack.getTagCompound().hasKey("SkullOwner")) return "";

        NBTTagCompound skullOwner = itemStack.getTagCompound().getCompoundTag("SkullOwner");
        if (!skullOwner.hasKey("Properties")) return "";

        NBTTagCompound properties = skullOwner.getCompoundTag("Properties");
        if (!properties.hasKey("textures")) return "";

        NBTTagList textures = properties.getTagList("textures", 10);
        if (textures.tagCount() == 0) return "";

        NBTTagCompound texture = textures.getCompoundTagAt(0);
        if (!texture.hasKey("Value")) return "";

        return texture.getString("Value");
    }

    public static ItemStack createSkullWithTexture(String name, String textureHash) {
        ItemStack skull = new ItemStack(Items.skull, 1, 3); 

        NBTTagCompound skullTag = new NBTTagCompound();
        NBTTagCompound skullOwner = new NBTTagCompound();

        skullOwner.setString("Id", UUID.randomUUID().toString());

        NBTTagCompound textures = new NBTTagCompound();
        NBTTagList texturesList = new NBTTagList();
        NBTTagCompound valueTag = new NBTTagCompound();

        String fullTexture = "eyJ0aW1lc3RhbXAiOjAsInByb2ZpbGVJZCI6IiIsInByb2ZpbGVOYW1lIjoiIiwidGV4dHVyZXMiOns" +
                "iU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL" +
                "y" + textureHash + "\"}}}";

        valueTag.setString("Value", Base64.getEncoder().encodeToString(fullTexture.getBytes()));
        texturesList.appendTag(valueTag);
        textures.setTag("textures", texturesList);

        skullOwner.setTag("Properties", textures);
        skullTag.setTag("SkullOwner", skullOwner);
        skull.setTagCompound(skullTag);
        skull.setStackDisplayName(name);
        return skull;
    }

    public static NBTTagCompound getOrCreateTag(ItemStack is) {
        if (is.hasTagCompound()) return is.getTagCompound();
        NBTTagCompound nbtTagCompound = new NBTTagCompound();
        is.setTagCompound(nbtTagCompound);
        return nbtTagCompound;
    }

    public static void addLoreLine(ItemStack item, String line) {
        if (!item.hasTagCompound()) return;
        if (!item.getTagCompound().hasKey("display")) return;
        if (!item.getTagCompound().getCompoundTag("display").hasKey("Lore")) return;

        NBTTagCompound displayTag = item.getTagCompound().getCompoundTag("display");
        NBTTagList lore = displayTag.getTagList("Lore", 8);

        lore.appendTag(new NBTTagString(line));
    }

    public static String getLoreLine(ItemStack item, String matcher) {
        if (!item.hasTagCompound()) return null;
        if (!item.getTagCompound().hasKey("display")) return null;
        if (!item.getTagCompound().getCompoundTag("display").hasKey("Lore")) return null;

        NBTTagCompound displayTag = item.getTagCompound().getCompoundTag("display");
        NBTTagList lore = displayTag.getTagList("Lore", 8);

        for (int i = 0; i < lore.tagCount(); i++) {
            String line = lore.getStringTagAt(i);
            if (line.contains(matcher)) return line;
        }

        return null;
    }

    public static Rarity getRarity(ItemStack item) {
        if (item == null || !item.hasTagCompound()) return Rarity.NONE;

        List<String> loreLines = getLoreLines(item);
        if (loreLines.isEmpty()) return Rarity.NONE;

        String lastLine = loreLines.get(loreLines.size() - 1);

        lastLine = StringUtils.stripFormattingFastRarity(lastLine);
        return Rarity.fromString(lastLine);
    }

    public static List<String> getLoreLines(ItemStack item) {
        if (item == null || !item.hasTagCompound()) return Collections.emptyList();

        NBTTagCompound displayTag = item.getTagCompound().getCompoundTag("display");
        if (displayTag == null || !displayTag.hasKey("Lore")) return Collections.emptyList();

        NBTTagList lore = displayTag.getTagList("Lore", 8);
        List<String> loreLines = new ArrayList<>();

        for (int i = 0; i < lore.tagCount(); i++) {
            loreLines.add(lore.getStringTagAt(i));
        }

        return loreLines;
    }

    public static String getLoreLine(ItemStack item, Pattern matcher) {
        List<String> lore = getLoreLines(item);
        if (lore.isEmpty()) return null;

        return lore.stream()
                .filter(line -> matcher.matcher(line).matches())
                .findFirst()
                .orElse(null);
    }

    public static boolean hasSkinValue(String value, ItemStack item) {
        if (item == null) return false;
        if (!item.hasTagCompound()) return false;
        if (!item.getTagCompound().hasKey("SkullOwner")) return false;
        NBTTagCompound skullOwner = item.getTagCompound().getCompoundTag("SkullOwner");
        if (!skullOwner.hasKey("Properties")) return false;
        NBTTagCompound properties = skullOwner.getCompoundTag("Properties");
        if (!properties.hasKey("textures")) return false;
        NBTTagList textures = properties.getTagList("textures", 10);
        for (int i = 0; i < textures.tagCount(); i++) {
            NBTTagCompound texture = textures.getCompoundTagAt(i);
            if (texture.hasKey("Value") && texture.getString("Value").equals(value)) return true;
        }
        return false;
    }

    public static boolean hasSkinValue(Skins skin, ItemStack item) {
        if (item == null) return false;
        if (!item.hasTagCompound()) return false;
        if (!item.getTagCompound().hasKey("SkullOwner")) return false;
        NBTTagCompound skullOwner = item.getTagCompound().getCompoundTag("SkullOwner");
        if (!skullOwner.hasKey("Properties")) return false;
        NBTTagCompound properties = skullOwner.getCompoundTag("Properties");
        if (!properties.hasKey("textures")) return false;
        NBTTagList textures = properties.getTagList("textures", 10);
        for (int i = 0; i < textures.tagCount(); i++) {
            NBTTagCompound texture = textures.getCompoundTagAt(i);
            if (texture.hasKey("Value")) {
                if (texture.getString("Value").equals(skin.getHttp()) || texture.getString("Value").equals(skin.getHttps())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSkyblockItem(ItemStack item) {
        if (item == null) return false;
        if (!item.hasTagCompound()) return false;
        if (!item.getTagCompound().hasKey("ExtraAttributes")) return false;
        NBTTagCompound extraAttributes = item.getTagCompound().getCompoundTag("ExtraAttributes");
        return extraAttributes.hasKey("id");
    }

    public static boolean isMenuItem(ItemStack item) {
        return item.getDisplayName().trim().isEmpty() && Item.getItemFromBlock(Blocks.stained_glass_pane) == item.getItem() && item.getItemDamage() == 15;
    }
    public static String getItemUUID(ItemStack item) {
        if (item == null || !item.hasTagCompound()) return "";
        NBTTagCompound attributes = item.getTagCompound().getCompoundTag("ExtraAttributes");
        return attributes.hasKey("uuid") ? attributes.getString("uuid") : "";
    }

    public static final class AdminStatus {
        private final boolean admin;
        private final String reason;
        private final String detail;
        public AdminStatus() { this(false, "", ""); }
        public AdminStatus(String reason, String detail) { this(true, reason, detail); }
        private AdminStatus(boolean admin, String reason, String detail) {
            this.admin = admin; this.reason = reason == null ? "" : reason; this.detail = detail == null ? "" : detail;
        }
        public boolean isAdmin() { return admin; }
        public String getReason() { return reason; }
        public String getDetail() { return detail; }
    }

    /** Conservative client-side heuristic for visibly flagging unusual item metadata. */
    public static AdminStatus isAdminItem(ItemStack item) {
        if (item == null) return new AdminStatus();
        String internalName = getInternalName(item);
        if ("CUSTOM_ITEM".equals(internalName)) return new AdminStatus("Custom Item", "");
        if (internalName.contains("PET_CUSTOM")) return new AdminStatus("Custom Pet", "");
        if (!item.hasTagCompound() || !item.getTagCompound().hasKey("ExtraAttributes")) return new AdminStatus();
        NBTTagCompound attributes = item.getTagCompound().getCompoundTag("ExtraAttributes");
        if (attributes.hasKey("origin")) {
            String origin = attributes.getString("origin");
            if (origin.contains("ALL_ITEMS_GUI_ACTOR")) return new AdminStatus("", "");
            if (origin.toLowerCase(java.util.Locale.ROOT).contains("random")) return new AdminStatus("Gambled item", "");
        }
        if (attributes.hasKey("candy")) {
            int candy = attributes.getInteger("candy");
            if (candy < 0 || candy > 10) return new AdminStatus("Candies", "");
        }
        if (attributes.hasKey("stars")) {
            int stars = attributes.getInteger("stars");
            if (stars < 0 || stars > 5) return new AdminStatus("Stars", "");
        }
        if (attributes.hasKey("master_stars")) {
            int stars = attributes.getInteger("master_stars");
            if (stars < 0 || stars > 5) return new AdminStatus("Master Stars", "");
        }
        if (attributes.hasKey("bid")) {
            int bid = attributes.getInteger("bid");
            if (bid < 0 || bid > 10) return new AdminStatus("Edited tags", "");
        }
        if (attributes.hasKey("timestamp") && attributes.getLong("timestamp") < 1640995200L) return new AdminStatus("Invalid date", "");
        if (attributes.hasKey("enchantments")) {
            NBTTagCompound enchantments = attributes.getCompoundTag("enchantments");
            for (String key : enchantments.getKeySet()) {
                int level = enchantments.getInteger(key);
                if (level < 0 || level > 10) return new AdminStatus(StringUtils.capitalizeName(key), String.valueOf(level));
            }
        }
        if (attributes.hasKey("exp") && attributes.getDouble("exp") > 1.5E9) return new AdminStatus("Leveled up", "");
        return new AdminStatus();
    }

    public static String getAdminName(ItemStack item) {
        if (item == null || !item.hasTagCompound() || !item.getTagCompound().hasKey("ExtraAttributes")) return "";
        NBTTagCompound attributes = item.getTagCompound().getCompoundTag("ExtraAttributes");
        if (attributes.hasKey("origin")) {
            String origin = attributes.getString("origin");
            if (origin.contains("ALL_ITEMS_GUI_ACTOR")) return origin.replace("ALL_ITEMS_GUI_ACTOR_", "");
        }
        return "";
    }

    public static boolean hasStackingCounter(ItemStack item) {
        return item != null && item.hasTagCompound()
                && item.getTagCompound().hasKey("ExtraAttributes")
                && item.getTagCompound().getCompoundTag("ExtraAttributes").hasKey("stacking_enchant_counter");
    }

    public static int getStackingCounter(ItemStack item) {
        if (!hasStackingCounter(item)) return 0;
        return item.getTagCompound().getCompoundTag("ExtraAttributes").getInteger("stacking_enchant_counter");
    }

    public static StackingEnchant getStackingEnchant(ItemStack item) {
        for (String line : getLoreLines(item)) {
            String clean = line == null ? "" : net.minecraft.util.StringUtils.stripControlCodes(line).trim();
            for (StackingEnchant enchant : StackingEnchant.values()) {
                if (enchant != StackingEnchant.NONE && clean.toLowerCase(java.util.Locale.ROOT).contains(enchant.getDisplayName().toLowerCase(java.util.Locale.ROOT))) return enchant;
            }
        }
        return StackingEnchant.NONE;
    }

}
