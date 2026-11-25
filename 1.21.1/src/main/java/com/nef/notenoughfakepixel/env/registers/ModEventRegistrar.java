package com.nef.notenoughfakepixel.env.registers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ModEventRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger("notenoughfakepixel");
    private static boolean registered;

    private ModEventRegistrar() {
    }

    public static void registerAll() {
        if (registered) {
            return;
        }
        var mod = FabricLoader.getInstance().getModContainer("notenoughfakepixel").orElseThrow();
        registered = true;

        List<Class<?>> types = new ArrayList<>();
        for (String name : RegistrationScanner.findAnnotatedClasses(mod)) {
            try {
                types.add(Class.forName(name, false, ModEventRegistrar.class.getClassLoader()));
            } catch (ClassNotFoundException | LinkageError error) {
                LOGGER.error("Could not load registered class {}", name, error);
            }
        }

        // Register key bindings before features attach callbacks that use them.
        Set<KeyBinding> keybinds = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> seenInstances = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Object> instances = new ArrayList<>();
        for (Class<?> type : types) {
            try {
                for (Field field : type.getDeclaredFields()) {
                    registerField(field, keybinds, seenInstances, instances);
                }
            } catch (LinkageError error) {
                LOGGER.error("Could not inspect registration fields in {}", type.getName(), error);
            }
        }

        Set<Class<?>> singletonTypes = new HashSet<>();
        for (Object instance : instances) {
            singletonTypes.add(instance.getClass());
            registerFeature(instance, instance instanceof ClientModInitializer,
                    instance instanceof ClientCommandRegistrationCallback);
        }
        for (Class<?> type : types) {
            if (!singletonTypes.contains(type)) {
                registerType(type);
            }
        }
    }

    private static void registerField(Field field, Set<KeyBinding> keybinds,
                                      Set<Object> seenInstances, List<Object> instances) {
        boolean keybind = field.isAnnotationPresent(RegisterKeybind.class);
        boolean instance = field.isAnnotationPresent(RegisterInstance.class);
        if (!keybind && !instance) {
            return;
        }
        try {
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new IllegalArgumentException("Registration fields must be static");
            }
            field.setAccessible(true);
            Object value = field.get(null);
            if (keybind) {
                if (!(value instanceof KeyBinding binding)) {
                    throw new IllegalArgumentException("@RegisterKeybind requires an initialized KeyBinding");
                }
                if (keybinds.add(binding)) {
                    KeyBindingHelper.registerKeyBinding(binding);
                }
            }
            if (instance) {
                if (!(value instanceof ClientModInitializer || value instanceof ClientCommandRegistrationCallback)) {
                    throw new IllegalArgumentException("@RegisterInstance requires an initialized client feature");
                }
                if (seenInstances.add(value)) {
                    instances.add(value);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.error("Could not register {}", field, error);
        }
    }

    private static void registerType(Class<?> type) {
        boolean events = type.isAnnotationPresent(RegisterEvents.class);
        boolean commands = type.isAnnotationPresent(RegisterCommand.class);
        if (!events && !commands) {
            return;
        }
        try {
            if (events && !ClientModInitializer.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("@RegisterEvents requires ClientModInitializer");
            }
            if (commands && !ClientCommandRegistrationCallback.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException("@RegisterCommand requires ClientCommandRegistrationCallback");
            }
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            registerFeature(constructor.newInstance(), events, commands);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.error("Could not register {}", type.getName(), error);
        }
    }

    private static void registerFeature(Object feature, boolean events, boolean commands) {
        if (events) {
            try {
                ((ClientModInitializer) feature).onInitializeClient();
            } catch (RuntimeException | LinkageError error) {
                LOGGER.error("Could not register event listeners for {}", feature.getClass().getName(), error);
            }
        }
        if (commands) {
            try {
                ClientCommandRegistrationCallback.EVENT.register((ClientCommandRegistrationCallback) feature);
            } catch (RuntimeException | LinkageError error) {
                LOGGER.error("Could not register client commands for {}", feature.getClass().getName(), error);
            }
        }
    }
}
