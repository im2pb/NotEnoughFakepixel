/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.core.feature;

import com.nef.notenoughfakepixel.features.inventory.InventoryFeatureModule;
import com.nef.notenoughfakepixel.features.skyblock.diana.DianaFeatureModule;
import net.minecraftforge.common.MinecraftForge;

/** Central composition root. All Forge listener packages are assigned to one lifecycle-owning module. */
public final class FeatureBootstrap {
    private static final String BASE = "com.nef.notenoughfakepixel";
    private static FeatureRegistry registry;

    private FeatureBootstrap() { }

    public static synchronized void initialize() {
        if (registry != null && registry.isStarted()) return;

        FeatureContext context = new FeatureContext(MinecraftForge.EVENT_BUS);
        registry = new FeatureRegistry(context);

        // Core infrastructure listeners (alerts, shared events, config lifecycle and shared parsers).
        registry.register(module("core-runtime", "Core Runtime", "Shared runtime listeners and cross-feature services.",
                FeatureCategory.INTERNAL,
                BASE + ".alerts",
                BASE + ".events",
                BASE + ".config.gui",
                BASE + ".utils"));

        // User-facing top-level domains.
        registry.register(new InventoryFeatureModule(
                BASE + ".features.inventory",
                BASE + ".features.skyblock.inventory"));
        registry.register(module("chat", "Chat", "Chat copy and chat interaction features.",
                FeatureCategory.QUALITY_OF_LIFE, BASE + ".features.chat"));
        registry.register(module("duels", "Duels", "Duels counters and match support.",
                FeatureCategory.QUALITY_OF_LIFE, BASE + ".features.duels"));
        registry.register(module("performance", "Performance", "Client performance and frame-time helpers.",
                FeatureCategory.INTERNAL, BASE + ".features.performance"));
        registry.register(module("protection", "Item Protection", "Protected inventory actions and safeguards.",
                FeatureCategory.INVENTORY, BASE + ".features.protection"));

        // SkyBlock domains. The roots are intentionally non-overlapping to prevent double registration.
        registry.register(module("dungeons", "Dungeons", "Dungeon solvers, map, score, rewards and mob helpers.",
                FeatureCategory.DUNGEONS, BASE + ".features.skyblock.dungeons"));
        registry.register(module("mining", "Mining", "Dwarven Mines and Crystal Hollows features.",
                FeatureCategory.MINING, BASE + ".features.skyblock.mining"));
        registry.register(module("skyblock-qol", "SkyBlock Quality of Life", "SkyBlock interaction, utility and shortcut features.",
                FeatureCategory.QUALITY_OF_LIFE, BASE + ".features.skyblock.qol"));
        // Diana has an explicit lifecycle owner and shared runtime services instead of package-scanned ownership.
        registry.register(new DianaFeatureModule());
        registry.register(module("skyblock-overlays", "SkyBlock Overlays", "HUD and visual overlay listeners.",
                FeatureCategory.HUD, BASE + ".features.skyblock.overlays"));
        registry.register(module("skyblock-progression", "SkyBlock Progression", "Accessories, chocolate, crimson, enchanting, fishing and Slayer features.",
                FeatureCategory.SKYBLOCK,
                BASE + ".features.skyblock.accessories",
                BASE + ".features.skyblock.chocolate",
                BASE + ".features.skyblock.crimson",
                BASE + ".features.skyblock.enchanting",
                BASE + ".features.skyblock.fishing",
                BASE + ".features.skyblock.slayers"));
        registry.register(module("slot-locking", "Slot Locking", "Inventory slot locking and binding.",
                FeatureCategory.INVENTORY, BASE + ".features.skyblock.slotlocking"));

        registry.initializeAll();
    }

    public static synchronized void shutdown() {
        if (registry != null) registry.shutdownAll();
    }

    public static synchronized FeatureRegistry getRegistry() {
        return registry;
    }

    private static AnnotatedPackageFeatureModule module(String id, String name, String description,
                                                         FeatureCategory category, String... packageRoots) {
        return new AnnotatedPackageFeatureModule(
                new FeatureDescriptor(id, name, description, category), packageRoots);
    }
}
