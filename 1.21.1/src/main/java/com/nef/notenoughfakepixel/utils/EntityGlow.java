package com.nef.notenoughfakepixel.utils;

import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Client-side colors for Minecraft's glowing entity outline. */
public final class EntityGlow {
    private static final List<Function<Entity, Integer>> PROVIDERS = new ArrayList<>();

    private EntityGlow() {
    }

    /**
     * Register a provider during client initialization. Return a color for a matching entity or null to pass.
     * The first matching provider wins. Use a client-side snapshot for targets that need a world scan.
     */
    public static void register(Function<Entity, Integer> provider) {
        PROVIDERS.add(provider);
    }

    /** Minecraft accepts RGB for this render pass. */
    public static Integer color(Entity entity) {
        if (entity.isRemoved()) return null;

        for (var provider : PROVIDERS) {
            Integer color = provider.apply(entity);
            if (color != null) return color & 0xFFFFFF;
        }
        return null;
    }
}
