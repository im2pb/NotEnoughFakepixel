package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.serverdata.SkyblockData;

final class DungeonContext {
    private DungeonContext() {
    }

    static boolean active() {
        return SkyblockData.isSkyblock() && SkyblockData.currentLocation().isDungeon();
    }
}
