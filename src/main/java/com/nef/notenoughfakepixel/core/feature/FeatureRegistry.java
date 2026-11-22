/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.core.feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic, explicit registry for feature modules. One failing module does not block other modules. */
public final class FeatureRegistry {
    private final FeatureContext context;
    private final Map<String, FeatureModule> modules = new LinkedHashMap<>();
    private final List<FeatureModule> initialized = new ArrayList<>();
    private boolean started;

    public FeatureRegistry(FeatureContext context) {
        if (context == null) throw new IllegalArgumentException("Feature context cannot be null");
        this.context = context;
    }

    public synchronized void register(FeatureModule module) {
        if (started) throw new IllegalStateException("Cannot register modules after startup");
        if (module == null || module.getDescriptor() == null) throw new IllegalArgumentException("Invalid feature module");
        String id = module.getDescriptor().getId();
        if (modules.containsKey(id)) throw new IllegalArgumentException("Duplicate feature module id: " + id);
        modules.put(id, module);
    }

    public synchronized void initializeAll() {
        if (started) return;
        started = true;
        for (String id : modules.keySet()) startModule(id);
    }

    /** Starts one registered module after the registry has entered its running state. */
    public synchronized boolean startModule(String id) {
        if (!started) return false;
        FeatureModule module = modules.get(id);
        if (module == null) return false;
        if (initialized.contains(module)) return true;
        try {
            module.initialize(context);
            initialized.add(module);
            log("Initialized", module, null);
            return true;
        } catch (Throwable error) {
            // A module may have registered one listener before a later initialization step failed.
            // Give it a rollback opportunity before continuing with the remaining modules.
            try {
                module.shutdown(context);
            } catch (Throwable rollbackError) {
                log("Rollback failed", module, rollbackError);
            }
            log("Failed to initialize", module, error);
            return false;
        }
    }

    /** Stops one module without taking down unrelated modules. */
    public synchronized boolean stopModule(String id) {
        FeatureModule module = modules.get(id);
        if (module == null || !initialized.contains(module)) return false;
        try {
            module.shutdown(context);
            initialized.remove(module);
            log("Stopped", module, null);
            return true;
        } catch (Throwable error) {
            log("Failed to stop", module, error);
            return false;
        }
    }

    public synchronized void shutdownAll() {
        for (int i = initialized.size() - 1; i >= 0; i--) {
            FeatureModule module = initialized.get(i);
            try {
                module.shutdown(context);
                log("Stopped", module, null);
            } catch (Throwable error) {
                log("Failed to stop", module, error);
            }
        }
        initialized.clear();
        try {
            context.unregisterAll();
        } catch (Throwable error) {
            System.out.println("[NEF FeatureModules] Final listener cleanup failed");
            error.printStackTrace();
        }
        started = false;
    }

    public synchronized FeatureModule get(String id) {
        return modules.get(id);
    }

    public synchronized Collection<FeatureModule> getModules() {
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    public synchronized Collection<FeatureModule> getModules(FeatureCategory category) {
        List<FeatureModule> result = new ArrayList<>();
        for (FeatureModule module : modules.values()) {
            if (category == null || module.getDescriptor().getCategory() == category) result.add(module);
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized boolean isInitialized(String id) {
        FeatureModule module = modules.get(id);
        return module != null && initialized.contains(module);
    }

    public synchronized boolean isStarted() {
        return started;
    }

    private static void log(String action, FeatureModule module, Throwable error) {
        String id = module.getDescriptor().getId();
        System.out.println("[NEF FeatureModules] " + action + " module '" + id + "'");
        if (error != null) error.printStackTrace();
    }
}
