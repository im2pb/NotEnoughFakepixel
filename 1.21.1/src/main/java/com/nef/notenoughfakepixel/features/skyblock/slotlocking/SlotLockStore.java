package com.nef.notenoughfakepixel.features.skyblock.slotlocking;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

final class SlotLockStore {
    static final int HOTBAR_SIZE = 9;
    static final int SLOT_COUNT = 40;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Entry[] EMPTY_PROFILE = new Entry[SLOT_COUNT];

    private final Path file;
    private Map<String, Entry[]> profiles = Map.of();
    private boolean readFailed;

    SlotLockStore(Path file) {
        this.file = file;
    }

    void load() throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            SaveFile data = GSON.fromJson(reader, SaveFile.class);
            if (data == null || data.profileData() == null) throw new JsonParseException("Missing profileData");

            var loaded = new LinkedHashMap<String, Entry[]>();
            data.profileData().forEach((profile, saved) -> {
                if (saved != null && saved.slotLockData() != null && saved.slotLockData().length > 0
                        && saved.slotLockData()[0] != null) {
                    loaded.put(profile, normalize(saved.slotLockData()[0].lockedSlots()));
                }
            });
            profiles = loaded;
            readFailed = false;
        } catch (NoSuchFileException ignored) {
            profiles = Map.of();
            readFailed = false;
        } catch (IOException | JsonParseException exception) {
            readFailed = true;
            throw new IOException("Could not load " + file, exception);
        }
    }

    Entry entry(String profile, int index) {
        return entry(profiles.getOrDefault(profile, EMPTY_PROFILE), index);
    }

    boolean toggle(String profile, int index) throws IOException {
        Entry[] entries = profiles.getOrDefault(profile, EMPTY_PROFILE).clone();
        boolean locked = !entry(entries, index).locked();
        unbind(entries, index);
        entries[index] = locked ? new Entry(true, -1) : null;
        save(profile, entries);
        return locked;
    }

    boolean bind(String profile, int inventoryIndex, int hotbarIndex) throws IOException {
        if (!canBind(inventoryIndex, hotbarIndex) || entry(profile, hotbarIndex).locked()) return false;
        if (entry(profile, inventoryIndex).partner() == hotbarIndex) return false;

        Entry[] entries = profiles.getOrDefault(profile, EMPTY_PROFILE).clone();
        unbind(entries, inventoryIndex);
        unbind(entries, hotbarIndex);
        entries[inventoryIndex] = new Entry(false, hotbarIndex);
        entries[hotbarIndex] = new Entry(false, inventoryIndex);
        save(profile, entries);
        return true;
    }

    void reset(String profile) throws IOException {
        save(profile, EMPTY_PROFILE);
    }

    static boolean valid(int index) {
        return index >= 0 && index < SLOT_COUNT;
    }

    static boolean canBind(int inventoryIndex, int hotbarIndex) {
        return inventoryIndex >= HOTBAR_SIZE && inventoryIndex < SLOT_COUNT && hotbarIndex >= 0 && hotbarIndex < HOTBAR_SIZE;
    }

    private static Entry entry(Entry[] entries, int index) {
        return valid(index) && entries[index] != null ? entries[index] : Entry.EMPTY;
    }

    private static void unbind(Entry[] entries, int index) {
        int partner = entry(entries, index).partner();
        if (valid(partner) && entry(entries, partner).partner() == index) entries[partner] = null;
        entries[index] = null;
    }

    private static Entry[] normalize(SavedSlot[] saved) {
        Entry[] entries = new Entry[SLOT_COUNT];
        if (saved == null) return entries;

        for (int index = 0; index < Math.min(SLOT_COUNT, saved.length); index++) {
            if (saved[index] != null && saved[index].locked()) entries[index] = new Entry(true, -1);
        }
        for (int index = HOTBAR_SIZE; index < Math.min(SLOT_COUNT, saved.length); index++) {
            SavedSlot slot = saved[index];
            if (slot == null || slot.locked() || slot.boundTo() == null) continue;

            int partner = slot.boundTo();
            if (!canBind(index, partner)) continue;
            SavedSlot hotbar = saved[partner];
            if (hotbar != null && !hotbar.locked() && hotbar.boundTo() != null && hotbar.boundTo() == index) {
                entries[index] = new Entry(false, partner);
                entries[partner] = new Entry(false, index);
            }
        }
        return entries;
    }

    private void save(String profile, Entry[] entries) throws IOException {
        if (readFailed) throw new IOException("Existing slot locking data could not be read");

        var updated = new LinkedHashMap<>(profiles);
        if (Arrays.stream(entries).allMatch(entry -> entry == null)) updated.remove(profile);
        else updated.put(profile, entries);

        var writeData = new LinkedHashMap<String, SavedProfile>();
        updated.forEach((name, slots) -> {
            SavedSlot[] saved = Arrays.stream(slots)
                    .map(entry -> entry == null ? null : new SavedSlot(entry.locked(), entry.partner()))
                    .toArray(SavedSlot[]::new);
            writeData.put(name, new SavedProfile(new SavedInventory[]{new SavedInventory(saved)}));
        });

        Path directory = file.toAbsolutePath().getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "slotlocking-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(new SaveFile(writeData)) + "\n");
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            profiles = updated;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    record Entry(boolean locked, int partner) {
        private static final Entry EMPTY = new Entry(false, -1);
    }

    private record SaveFile(Map<String, SavedProfile> profileData) {
    }

    private record SavedProfile(SavedInventory[] slotLockData) {
    }

    private record SavedInventory(SavedSlot[] lockedSlots) {
    }

    private record SavedSlot(boolean locked, Integer boundTo) {
    }
}
