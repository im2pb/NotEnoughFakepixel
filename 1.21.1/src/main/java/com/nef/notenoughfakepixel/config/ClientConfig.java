package com.nef.notenoughfakepixel.config;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.nef.notenoughfakepixel.Configuration;
import io.github.notenoughupdates.moulconfig.gui.GuiElementWrapper;
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor;
import io.github.notenoughupdates.moulconfig.managed.GsonMapper;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig;
import io.github.notenoughupdates.moulconfig.managed.ManagedConfigBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("notenoughfakepixel");
    private ManagedConfig<Configuration> config;
    private String pendingSearch;

    public ClientConfig() {
        var openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.notenoughfakepixel.open_config", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P, "key.categories.notenoughfakepixel"
        ));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var command = dispatcher.register(ClientCommandManager.literal("nef")
                    .executes(context -> queueScreen(""))
                    .then(ClientCommandManager.argument("search", StringArgumentType.greedyString())
                            .executes(context -> queueScreen(StringArgumentType.getString(context, "search")))));
            dispatcher.register(ClientCommandManager.literal("notenoughfakepixel")
                    .executes(context -> queueScreen(""))
                    .redirect(command));
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    queueScreen("");
                }
            }
            if (pendingSearch == null || client.player == null) {
                return;
            }
            var editor = getConfig().getEditor();
            if (!pendingSearch.isBlank()) {
                editor.search(pendingSearch.trim());
            }
            pendingSearch = null;
            client.setScreen(new GuiElementWrapper(editor));
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (config != null && screen instanceof GuiElementWrapper wrapper
                    && wrapper.getGuiElement() instanceof MoulConfigEditor<?> editor
                    && editor.getProcessedConfig().getConfigObject() == config.getInstance()) {
                ScreenEvents.remove(screen).register(removed -> config.saveToFile());
            }
        });
    }

    private ManagedConfig<Configuration> getConfig() {
        if (config == null) {
            var file = FabricLoader.getInstance().getConfigDir().resolve("notenoughfakepixel.json").toFile();
            var builder = new ManagedConfigBuilder<>(file, Configuration.class);
            var mapper = new GsonMapper<>(Configuration.class);
            mapper.getGsonBuilder().setPrettyPrinting();
            builder.setMapper(mapper);
            builder.setLoadFailed((data, error) ->
                    LOGGER.warn("Could not read {}. Using default settings.", data.getFile(), error));
            builder.setSaveFailed((data, error) ->
                    LOGGER.error("Could not save {}", data.getFile(), error));
            MoulConfigEditors.register(builder);
            config = new ManagedConfig<>(builder);
        }
        return config;
    }

    private int queueScreen(String search) {
        pendingSearch = search;
        return 1;
    }
}
