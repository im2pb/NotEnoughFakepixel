/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.envcheck.registers;

import com.nef.notenoughfakepixel.core.feature.FeatureDiscoveryIndex;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommand;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Registers non-event entry points that still use annotations (commands and key bindings).
 * Forge event listeners are exclusively owned by FeatureBootstrap modules.
 */
public final class ModEventRegistrar {

    private ModEventRegistrar() { }

    /** @deprecated Event listeners are registered by FeatureBootstrap. */
    @Deprecated
    public static void registerModEvents() {
        System.out.println("[NEF AutoRegister] Forge listeners are owned by FeatureBootstrap modules.");
    }

    public static void registerCommands() {
        for (Class<?> type : FeatureDiscoveryIndex.findCommandTypes()) {
            if (!ICommand.class.isAssignableFrom(type)) {
                System.out.println("[NEF AutoRegister] " + type.getName()
                        + " does not implement " + ICommand.class.getName() + " for @RegisterCommand");
                continue;
            }
            try {
                Constructor<?> constructor = type.getDeclaredConstructor();
                if (!constructor.isAccessible()) constructor.setAccessible(true);
                ClientCommandHandler.instance.registerCommand((ICommand) constructor.newInstance());
            } catch (Throwable error) {
                System.out.println("[NEF AutoRegister] Failed command register: " + type.getName());
                error.printStackTrace();
            }
        }
    }

    public static void registerKeybinds() {
        for (Field field : FeatureDiscoveryIndex.findKeybindFields()) {
            try {
                if (!Modifier.isStatic(field.getModifiers())) {
                    System.out.println("[NEF Keybind] " + field.getDeclaringClass().getName() + "." + field.getName()
                            + " should be static to register as @RegisterKeybind.");
                    continue;
                }
                if (!KeyBinding.class.isAssignableFrom(field.getType())) {
                    System.out.println("[NEF Keybind] " + field.getName() + " is not a KeyBind.");
                    continue;
                }
                if (!field.isAccessible()) field.setAccessible(true);
                KeyBinding key = (KeyBinding) field.get(null);
                if (key == null) {
                    System.out.println("[NEF Keybind] " + field.getName() + " is null (not initialized).");
                    continue;
                }
                ClientRegistry.registerKeyBinding(key);
            } catch (Throwable error) {
                System.out.println("[NEF Keybind] Failed field register: " + field.getName());
                error.printStackTrace();
            }
        }
    }
}
