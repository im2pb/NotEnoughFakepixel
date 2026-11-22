/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.core.feature;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.envcheck.registers.ListenerGateManager;
import net.minecraftforge.fml.common.eventhandler.EventBus;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Runtime services intentionally shared with modules. Keep this small and explicit. */
public final class FeatureContext {
    private final EventBus eventBus;
    private final Map<Object, Boolean> registered = new IdentityHashMap<>();
    private final List<Object> registrationOrder = new ArrayList<>();

    public FeatureContext(EventBus eventBus) {
        if (eventBus == null) throw new IllegalArgumentException("Event bus cannot be null");
        this.eventBus = eventBus;
    }

    /** Registers a listener once by object identity. Gated listeners are owned by the gate manager. */
    public synchronized void registerEventListener(Object listener) {
        if (listener == null || registered.containsKey(listener)) return;
        if (listener instanceof GatedListener) {
            ListenerGateManager.register((GatedListener) listener);
        } else {
            eventBus.register(listener);
        }
        registered.put(listener, Boolean.TRUE);
        registrationOrder.add(listener);
    }

    /** Unregisters a listener and releases any gated-listener bookkeeping. */
    public synchronized void unregisterEventListener(Object listener) {
        if (listener == null || !registered.containsKey(listener)) return;
        if (listener instanceof GatedListener) {
            ListenerGateManager.unregister((GatedListener) listener);
        } else {
            eventBus.unregister(listener);
        }
        registered.remove(listener);
        removeByIdentity(listener);
    }

    /** Last-resort cleanup used during full mod shutdown after individual modules have stopped. */
    public synchronized void unregisterAll() {
        Throwable firstError = null;
        for (int i = registrationOrder.size() - 1; i >= 0; i--) {
            Object listener = registrationOrder.get(i);
            try {
                unregisterEventListener(listener);
            } catch (Throwable error) {
                if (firstError == null) firstError = error;
                else firstError.addSuppressed(error);
            }
        }
        if (firstError != null) throw new IllegalStateException("One or more feature listeners could not be unregistered", firstError);
    }

    public synchronized int getRegisteredListenerCount() {
        return registered.size();
    }

    private void removeByIdentity(Object listener) {
        for (int i = registrationOrder.size() - 1; i >= 0; i--) {
            if (registrationOrder.get(i) == listener) {
                registrationOrder.remove(i);
                return;
            }
        }
    }
}
