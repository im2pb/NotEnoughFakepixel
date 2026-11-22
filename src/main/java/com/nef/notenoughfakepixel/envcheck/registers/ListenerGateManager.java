/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.envcheck.registers;

import com.nef.notenoughfakepixel.config.gui.Config;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/** Owns conditional Forge listener registration and removes entries when their feature module stops. */
public final class ListenerGateManager {

    private static final class Entry {
        final GatedListener listener;
        boolean registered;
        boolean activationFailureLogged;

        Entry(GatedListener listener) {
            this.listener = listener;
        }
    }

    private static final ListenerGateManager INSTANCE = new ListenerGateManager();
    private static boolean installed;

    private final List<Entry> entries = new ArrayList<>();

    private ListenerGateManager() { }

    private static synchronized void install() {
        if (installed) return;
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        installed = true;
    }

    /** Adds a gated listener once. The first state evaluation happens immediately when config is ready. */
    public static synchronized void register(GatedListener listener) {
        if (listener == null) return;
        for (Entry entry : INSTANCE.entries) {
            if (entry.listener == listener) return;
        }
        install();
        Entry entry = new Entry(listener);
        INSTANCE.entries.add(entry);
        INSTANCE.update(entry);
    }

    /** Removes a gated listener, unregistering it from Forge and releasing its gate entry. */
    public static synchronized void unregister(GatedListener listener) {
        if (listener == null) return;
        for (int i = INSTANCE.entries.size() - 1; i >= 0; i--) {
            Entry entry = INSTANCE.entries.get(i);
            if (entry.listener != listener) continue;
            boolean wasRegistered = entry.registered;
            if (wasRegistered) {
                MinecraftForge.EVENT_BUS.unregister(listener);
                entry.registered = false;
            }
            INSTANCE.entries.remove(i);
            if (wasRegistered) INSTANCE.deactivate(listener);
        }
        if (INSTANCE.entries.isEmpty() && installed) {
            MinecraftForge.EVENT_BUS.unregister(INSTANCE);
            installed = false;
        }
    }

    public static synchronized int getRegisteredCount() {
        int count = 0;
        for (Entry entry : INSTANCE.entries) if (entry.registered) count++;
        return count;
    }

    public static synchronized int getGatedCount() {
        return INSTANCE.entries.size();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) updateAll();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) updateAll();
    }

    private void updateAll() {
        synchronized (ListenerGateManager.class) {
            if (Config.feature == null) return;
            for (Entry entry : new ArrayList<>(entries)) update(entry);
        }
    }

    private void update(Entry entry) {
        if (Config.feature == null) return;
        boolean active;
        try {
            active = entry.listener.isListenerActive();
            entry.activationFailureLogged = false;
        } catch (Throwable error) {
            // Fail closed: a broken feature predicate must not accidentally activate a listener.
            active = false;
            if (!entry.activationFailureLogged) {
                System.out.println("[NEF Gate] Activation check failed for " + entry.listener.getClass().getName());
                error.printStackTrace();
                entry.activationFailureLogged = true;
            }
        }

        if (active == entry.registered) return;
        if (active) {
            MinecraftForge.EVENT_BUS.register(entry.listener);
            entry.registered = true;
        } else {
            if (entry.registered) MinecraftForge.EVENT_BUS.unregister(entry.listener);
            entry.registered = false;
            deactivate(entry.listener);
        }
    }

    private void deactivate(GatedListener listener) {
        try {
            listener.onListenerDeactivated();
        } catch (Throwable error) {
            System.out.println("[NEF Gate] onListenerDeactivated failed for " + listener.getClass().getName());
            error.printStackTrace();
        }
    }
}
