/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import com.nef.notenoughfakepixel.events.handlers.RepoHandler;
import com.nef.notenoughfakepixel.variables.Rarity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pet catalogue backed by the existing NEF repository cache, with a safe offline fallback. */
public final class PetData {
    private static final Map<String, String> FALLBACK;
    private static final Map<String, Rarity> EMPTY = Collections.emptyMap();
    private static volatile PetMap cachedSource;
    private static volatile Map<String, Rarity> cachedPets = EMPTY;

    static {
        Map<String, String> pets = new LinkedHashMap<>();
        String[] legendary = {
                "Ammonite", "Armadillo", "Baby Yeti", "Bal", "Bat", "Bee", "Black Cat", "Blaze",
                "Blue Whale", "Chicken", "Dolphin", "Developer's Pet", "Duck", "Frozen Bubble",
                "Relic Keeper", "Droplet Wisp", "Elephant", "Ender Dragon", "Enderman", "Endermite",
                "Flying Fish", "Ghoul", "Giraffe", "Golem", "Golden Dragon", "Griffin", "Guardian",
                "Horse", "Jerry", "Lion", "Megalodon", "Monkey", "Mooshroom Cow", "Ocelot", "Parrot",
                "Phoenix", "Pigman", "Rabbit", "Rock", "Sheep", "Silverfish", "Skeleton Horse",
                "Snowman", "Spider", "Squid", "Tarantula", "Tiger", "Wither Skeleton", "Wolf", "Zombie"
        };
        for (String name : legendary) pets.put(name, "LEGENDARY");
        pets.put("Mithril Golem", "MYTHIC");
        FALLBACK = Collections.unmodifiableMap(pets);
    }

    private PetData() { }

    public static Map<String, Rarity> getAllPets() {
        // RepoHandler returns the same parsed object until the remote cache changes.
        // Cache the converted rarity map too, avoiding allocations on every throttled GUI scan.
        PetMap source = RepoHandler.getData("pets", PetMap.class, PetMap.EMPTY);
        Map<String, String> raw = source == null ? null : source.pets;
        if (raw == null || raw.isEmpty()) return fallbackPets();
        Map<String, Rarity> snapshot = cachedPets;
        if (source == cachedSource && !snapshot.isEmpty()) return snapshot;
        synchronized (PetData.class) {
            if (source == cachedSource && !cachedPets.isEmpty()) return cachedPets;
            Map<String, Rarity> result = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : raw.entrySet()) {
                Rarity rarity = Rarity.fromString(entry.getValue());
                if (rarity != Rarity.NONE) result.put(entry.getKey(), rarity);
            }
            cachedSource = source;
            cachedPets = result.isEmpty() ? fallbackPets() : Collections.unmodifiableMap(result);
            return cachedPets;
        }
    }

    private static Map<String, Rarity> fallbackPets() {
        Map<String, Rarity> snapshot = cachedPets;
        if (!snapshot.isEmpty() && cachedSource == PetMap.EMPTY) return snapshot;
        synchronized (PetData.class) {
            if (cachedSource == PetMap.EMPTY && !cachedPets.isEmpty()) return cachedPets;
            Map<String, Rarity> result = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : FALLBACK.entrySet()) {
                Rarity rarity = Rarity.fromString(entry.getValue());
                if (rarity != Rarity.NONE) result.put(entry.getKey(), rarity);
            }
            cachedSource = PetMap.EMPTY;
            cachedPets = Collections.unmodifiableMap(result);
            return cachedPets;
        }
    }

    public static final class PetMap {
        static final PetMap EMPTY = new PetMap();
        public Map<String, String> pets = Collections.emptyMap();
    }
}
