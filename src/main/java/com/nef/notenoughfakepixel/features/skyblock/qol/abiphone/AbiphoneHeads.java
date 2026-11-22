/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol.abiphone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

@RegisterEvents
public final class AbiphoneHeads {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Pattern HASH = Pattern.compile("[0-9a-fA-F]{40,64}");
    private static final String SKIN_URL = "http://textures.minecraft.net/texture/";
    private static final int SCAN_INTERVAL_TICKS = 10;
    private static final int MAX_ENTRIES = 500;

    private static final String DEFAULTS_RESOURCE = "/assets/notenoughfakepixel/data/abiphone/default_heads.json";

    private static final Map<String, String> DEFAULTS = new TreeMap<>();
    
    private static final Map<String, String> HEADS = new TreeMap<>();
    
    private static final Map<String, ItemStack> CACHE = new HashMap<>();
    private static final ItemStack PLAIN = new ItemStack(Items.skull, 1, 3);
    private static boolean loaded;
    private static int tick;

    public static ItemStack headFor(String contact) {
        load();
        ItemStack cached = CACHE.get(contact);
        if (cached != null) return cached;
        String value = find(normalize(contact));
        ItemStack head = value == null ? PLAIN : buildHead(value);
        CACHE.put(contact, head == null ? PLAIN : head);
        return head == null ? PLAIN : head;
    }

    static String normalize(String name) {
        if (name == null) return "";
        String stripped = EnumChatFormatting.getTextWithoutFormattingCodes(name);
        if (stripped == null) stripped = name;
        StringBuilder out = new StringBuilder(stripped.length());
        for (int i = 0; i < stripped.length(); i++) {
            char c = Character.toLowerCase(stripped.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) out.append(c);
        }
        return out.toString();
    }

    private static String find(String key) {
        if (key.isEmpty()) return null;
        String own = lookup(HEADS, key);
        return own != null ? own : lookup(DEFAULTS, key);
    }

    private static String lookup(Map<String, String> heads, String key) {
        String exact = heads.get(key);
        if (exact != null) return exact;

        String best = null;
        int bestGap = Integer.MAX_VALUE;
        for (Map.Entry<String, String> entry : heads.entrySet()) {
            String other = entry.getKey();
            boolean related = (key.length() >= 4 && (other.startsWith(key) || other.endsWith(key)))
                    || (other.length() >= 4 && (key.startsWith(other) || key.endsWith(other)));
            if (!related) continue;
            int gap = Math.abs(other.length() - key.length());
            if (gap < bestGap) {
                bestGap = gap;
                best = entry.getValue();
            }
        }
        return best;
    }

    private static ItemStack buildHead(String skin) {
        String value = toTextureValue(skin);
        if (value == null) return null;

        NBTTagCompound owner = new NBTTagCompound();
        
        owner.setString("Id", UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString());
        NBTTagCompound texture = new NBTTagCompound();
        texture.setString("Value", value);
        NBTTagList list = new NBTTagList();
        list.appendTag(texture);
        NBTTagCompound properties = new NBTTagCompound();
        properties.setTag("textures", list);
        owner.setTag("Properties", properties);

        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("SkullOwner", owner);
        ItemStack head = new ItemStack(Items.skull, 1, 3);
        head.setTagCompound(tag);
        return head;
    }

    private static String toTextureValue(String skin) {
        if (skin == null) return null;
        String trimmed = skin.trim();
        if (trimmed.isEmpty()) return null;
        String url = null;
        if (HASH.matcher(trimmed).matches()) url = SKIN_URL + trimmed;
        else if (trimmed.startsWith("http")) url = trimmed;
        if (url == null) return trimmed; 
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url.replace("\"", "") + "\"}}}";
        return java.util.Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tick % SCAN_INTERVAL_TICKS != 0) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChest) || mc.thePlayer == null) return;
        if (Config.feature == null || !Config.feature.qol.qolAbiphone) return;

        Container container = mc.thePlayer.openContainer;
        if (!(container instanceof ContainerChest)) return;
        IInventory inventory = ((ContainerChest) container).getLowerChestInventory();
        if (inventory == null || inventory.getDisplayName() == null) return;
        String title = normalize(inventory.getDisplayName().getUnformattedText());
        if (!title.contains("abiphone") && !title.contains("contacts")) return;

        load();
        boolean changed = false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.getItem() != Items.skull) continue;
            String texture = ItemUtils.getSkullTexture(stack);
            if (texture == null || texture.isEmpty()) continue;
            String key = normalize(stack.getDisplayName());
            if (key.length() < 2 || key.length() > 40) continue;
            if (!HEADS.containsKey(key) && HEADS.size() >= MAX_ENTRIES) continue;
            if (!texture.equals(HEADS.put(key, texture))) changed = true;
        }
        if (changed) {
            CACHE.clear();
            save();
        }
    }

    private static File file() {
        return new File(Config.configDirectory, "abiphone-heads.json");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        loadDefaults();
        File file = file();
        if (!file.isFile()) return;
        try {
            String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Map<String, String> map = GSON.fromJson(text, MAP_TYPE);
            if (map == null) return;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = normalize(entry.getKey());
                if (key.isEmpty() || entry.getValue() == null || entry.getValue().trim().isEmpty()) continue;
                if (HEADS.size() >= MAX_ENTRIES) break;
                HEADS.put(key, entry.getValue().trim());
            }
        } catch (Exception ignored) {
            
        }
    }

    private static void loadDefaults() {
        try (java.io.InputStream in = AbiphoneHeads.class.getResourceAsStream(DEFAULTS_RESOURCE)) {
            if (in == null) return;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            Map<String, String> map = GSON.fromJson(new String(out.toByteArray(), StandardCharsets.UTF_8), MAP_TYPE);
            if (map == null) return;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = normalize(entry.getKey());
                if (!key.isEmpty() && entry.getValue() != null && !entry.getValue().trim().isEmpty()) {
                    DEFAULTS.put(key, entry.getValue().trim());
                }
            }
        } catch (Exception ignored) {
            
        }
    }

    private static void save() {
        File file = file();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return;
        File temp = new File(file.getPath() + ".tmp");
        try {
            Files.write(temp.toPath(), GSON.toJson(HEADS, MAP_TYPE).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception atomicFailed) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            temp.delete();
        }
    }
}
