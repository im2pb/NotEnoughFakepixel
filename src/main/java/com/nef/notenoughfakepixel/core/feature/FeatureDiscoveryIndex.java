/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.core.feature;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterCommand;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterInstance;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterKeybind;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * One immutable reflection index shared by all feature modules. Discovery is performed once; each module
 * receives only the listener types and singleton fields inside its explicitly-owned package roots.
 */
public final class FeatureDiscoveryIndex {
    private static final String BASE_PACKAGE = "com.nef.notenoughfakepixel";
    private static volatile Reflections reflections;
    private static volatile List<Class<?>> eventListenerTypes;
    private static volatile List<Class<?>> commandTypes;
    private static volatile List<Field> keybindFields;
    private static volatile List<Field> instanceFields;

    private FeatureDiscoveryIndex() { }

    public static List<Class<?>> findEventListenerTypes(String... packageRoots) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> type : getEventListenerTypes()) {
            if (isWithinRoots(type.getPackage() == null ? "" : type.getPackage().getName(), packageRoots)) {
                result.add(type);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Class<?>> findCommandTypes() {
        List<Class<?>> result = commandTypes;
        if (result != null) return result;
        synchronized (FeatureDiscoveryIndex.class) {
            if (commandTypes == null) {
                List<Class<?>> discovered = new ArrayList<>(getReflections().getTypesAnnotatedWith(RegisterCommand.class));
                Collections.sort(discovered, Comparator.comparing(Class::getName));
                commandTypes = Collections.unmodifiableList(discovered);
            }
            return commandTypes;
        }
    }

    public static List<Field> findKeybindFields() {
        List<Field> result = keybindFields;
        if (result != null) return result;
        synchronized (FeatureDiscoveryIndex.class) {
            if (keybindFields == null) {
                List<Field> discovered = new ArrayList<>(getReflections().getFieldsAnnotatedWith(RegisterKeybind.class));
                Collections.sort(discovered, Comparator
                        .comparing((Field field) -> field.getDeclaringClass().getName())
                        .thenComparing(Field::getName));
                keybindFields = Collections.unmodifiableList(discovered);
            }
            return keybindFields;
        }
    }

    public static List<Field> findInstanceFields(String... packageRoots) {
        List<Field> result = new ArrayList<>();
        for (Field field : getInstanceFields()) {
            Package ownerPackage = field.getDeclaringClass().getPackage();
            String packageName = ownerPackage == null ? "" : ownerPackage.getName();
            if (isWithinRoots(packageName, packageRoots)) result.add(field);
        }
        return Collections.unmodifiableList(result);
    }

    private static List<Class<?>> getEventListenerTypes() {
        List<Class<?>> result = eventListenerTypes;
        if (result != null) return result;
        synchronized (FeatureDiscoveryIndex.class) {
            if (eventListenerTypes == null) {
                List<Class<?>> discovered = new ArrayList<>(getReflections().getTypesAnnotatedWith(RegisterEvents.class));
                Collections.sort(discovered, Comparator.comparing(Class::getName));
                eventListenerTypes = Collections.unmodifiableList(discovered);
            }
            return eventListenerTypes;
        }
    }

    private static List<Field> getInstanceFields() {
        List<Field> result = instanceFields;
        if (result != null) return result;
        synchronized (FeatureDiscoveryIndex.class) {
            if (instanceFields == null) {
                List<Field> discovered = new ArrayList<>(getReflections().getFieldsAnnotatedWith(RegisterInstance.class));
                Collections.sort(discovered, Comparator
                        .comparing((Field field) -> field.getDeclaringClass().getName())
                        .thenComparing(Field::getName));
                instanceFields = Collections.unmodifiableList(discovered);
            }
            return instanceFields;
        }
    }

    public static boolean isWithinRoots(String packageName, String... packageRoots) {
        if (packageName == null || packageRoots == null) return false;
        for (String root : packageRoots) {
            if (root == null || root.trim().isEmpty()) continue;
            String normalized = root.trim();
            if (packageName.equals(normalized) || packageName.startsWith(normalized + ".")) return true;
        }
        return false;
    }

    private static Reflections getReflections() {
        Reflections value = reflections;
        if (value != null) return value;
        synchronized (FeatureDiscoveryIndex.class) {
            value = reflections;
            if (value == null) {
                value = new Reflections(new ConfigurationBuilder()
                        .forPackages(BASE_PACKAGE)
                        .addScanners(new TypeAnnotationsScanner(), new FieldAnnotationsScanner()));
                reflections = value;
            }
        }
        return value;
    }
}
