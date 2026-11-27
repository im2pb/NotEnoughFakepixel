package com.nef.notenoughfakepixel.features.skyblock.mining;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.variables.Location;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@RegisterEvents
public final class DonEspressoMessages implements ClientModInitializer {
    private static final String NPC_PREFIX = "[NPC] Don Espresso:";

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(DonEspressoMessages::allowMessage);
    }

    private static boolean allowMessage(Text message, boolean overlay) {
        if (overlay || !SkyblockData.isSkyblock() || SkyblockData.currentLocation() != Location.DWARVEN
                || !ClientConfig.configuration().mining.dwarvenMines.miningDisableDonEspresso) return true;

        return !Formatting.strip(message.getString()).stripLeading().startsWith(NPC_PREFIX);
    }
}
