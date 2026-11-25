package com.nef.notenoughfakepixel;

import com.nef.notenoughfakepixel.env.registers.ModEventRegistrar;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NotEnoughFakepixelClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("notenoughfakepixel");

    @Override
    public void onInitializeClient() {
        ModEventRegistrar.registerAll();
        LOGGER.info("NotEnoughFakepixel client initialized");
    }
}
