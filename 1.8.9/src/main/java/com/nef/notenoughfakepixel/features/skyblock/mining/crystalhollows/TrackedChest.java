package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows;

import net.minecraft.util.BlockPos;

public class TrackedChest {

    public final BlockPos pos;
    public final long spawnTime;

    public TrackedChest(BlockPos pos) {
        this.pos = pos;
        this.spawnTime = System.currentTimeMillis();
    }

    public float getLifeRatio() {
        long elapsed = System.currentTimeMillis() - spawnTime;
        return Math.max(0.0f, 1.0f - (elapsed / 30000.0f));
    }
}
