/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.core.feature;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Migration adapter for a bounded feature package. It gives legacy annotated listeners a real module owner
 * without forcing every mature feature implementation to be rewritten in the same change.
 */
public final class AnnotatedPackageFeatureModule implements FeatureModule {
    private final FeatureDescriptor descriptor;
    private final String[] packageRoots;
    private final List<Object> registeredListeners = new ArrayList<>();
    private boolean initialized;

    public AnnotatedPackageFeatureModule(FeatureDescriptor descriptor, String... packageRoots) {
        if (descriptor == null) throw new IllegalArgumentException("Feature descriptor cannot be null");
        if (packageRoots == null || packageRoots.length == 0) {
            throw new IllegalArgumentException("At least one package root is required");
        }
        this.descriptor = descriptor;
        this.packageRoots = packageRoots.clone();
        for (String root : this.packageRoots) {
            if (root == null || root.trim().isEmpty()) throw new IllegalArgumentException("Package roots cannot be blank");
        }
    }

    @Override
    public FeatureDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public synchronized void initialize(FeatureContext context) {
        if (initialized) return;
        if (context == null) throw new IllegalArgumentException("Feature context cannot be null");

        try {
            for (Class<?> listenerType : FeatureDiscoveryIndex.findEventListenerTypes(packageRoots)) {
                registerDiscoveredType(context, listenerType);
            }
            for (Field instanceField : FeatureDiscoveryIndex.findInstanceFields(packageRoots)) {
                registerDiscoveredInstance(context, instanceField);
            }
            initialized = true;
        } catch (Throwable error) {
            try {
                shutdown(context);
            } catch (Throwable rollbackError) {
                error.addSuppressed(rollbackError);
            }
            throw new IllegalStateException("Could not initialize feature module '" + descriptor.getId() + "'", error);
        }
    }

    @Override
    public synchronized void shutdown(FeatureContext context) {
        if (context == null) return;
        Throwable firstError = null;
        for (int i = registeredListeners.size() - 1; i >= 0; i--) {
            Object listener = registeredListeners.get(i);
            try {
                context.unregisterEventListener(listener);
            } catch (Throwable error) {
                if (firstError == null) firstError = error;
                else firstError.addSuppressed(error);
            }
        }
        registeredListeners.clear();
        initialized = false;
        if (firstError != null) {
            throw new IllegalStateException("Could not fully stop feature module '" + descriptor.getId() + "'", firstError);
        }
    }

    public synchronized List<Object> getRegisteredListeners() {
        return Collections.unmodifiableList(new ArrayList<>(registeredListeners));
    }

    private void registerDiscoveredType(FeatureContext context, Class<?> listenerType) {
        if (listenerType == null || listenerType.isInterface() || Modifier.isAbstract(listenerType.getModifiers())) {
            System.out.println("[NEF FeatureModules] Skipping non-instantiable listener in '" + descriptor.getId() + "': "
                    + (listenerType == null ? "null" : listenerType.getName()));
            return;
        }
        try {
            Constructor<?> constructor = listenerType.getDeclaredConstructor();
            if (!constructor.isAccessible()) constructor.setAccessible(true);
            register(context, constructor.newInstance());
        } catch (Throwable error) {
            logDiscoveryFailure("listener", listenerType.getName(), error);
        }
    }

    private void registerDiscoveredInstance(FeatureContext context, Field field) {
        if (field == null || !Modifier.isStatic(field.getModifiers())) {
            logDiscoveryFailure("instance field", field == null ? "null" : field.toString(),
                    new IllegalArgumentException("@RegisterInstance fields must be static"));
            return;
        }
        try {
            if (!field.isAccessible()) field.setAccessible(true);
            Object instance = field.get(null);
            if (instance != null) register(context, instance);
            else logDiscoveryFailure("instance field", field.toString(),
                    new IllegalStateException("@RegisterInstance value is null"));
        } catch (Throwable error) {
            logDiscoveryFailure("instance field", field.toString(), error);
        }
    }

    private void register(FeatureContext context, Object listener) {
        if (listener == null) return;
        context.registerEventListener(listener);
        registeredListeners.add(listener);
    }

    private void logDiscoveryFailure(String kind, String owner, Throwable error) {
        System.out.println("[NEF FeatureModules] Failed to register " + kind + " in module '"
                + descriptor.getId() + "': " + owner);
        error.printStackTrace();
    }
}
