/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.mining.crystalhollows.waypoints;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CrystalWaypoints {

    private static final CrystalWaypoints INSTANCE = new CrystalWaypoints();
    private final List<ChWaypoint> waypoints = new ArrayList<>(128);
    private volatile List<ChWaypoint> publishedWaypoints = Collections.emptyList();
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ChWaypoint.class, new ChWaypointAdapter())
            .setPrettyPrinting()
            .create();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private File file;

    private CrystalWaypoints() {}

    public static CrystalWaypoints getInstance() { return INSTANCE; }

    public void initFile() {
        if (file != null) return;
        File cfgDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        cfgDir.mkdirs();
        file = new File(cfgDir, "chwaypoints.json");
    }

    public synchronized void load() {
        initFile();
        waypoints.clear();

        boolean migrated = false;

        if (file.exists()) {
            try (Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                Type listType = new TypeToken<List<ChWaypoint>>(){}.getType();
                List<ChWaypoint> loaded = gson.fromJson(r, listType);

                if (loaded != null) {
                    LinkedHashMap<String, ChWaypoint> map = new LinkedHashMap<>(loaded.size() * 2);
                    for (ChWaypoint w : loaded) {
                        if (w == null) { migrated = true; continue; }

                        String id = w.getId();
                        if (id == null || id.isEmpty()) {
                            
                            w = new ChWaypoint(w.getX(), w.getY(), w.getZ(),
                                    UUID.randomUUID().toString(),
                                    w.getName() != null ? w.getName() : "Waypoint",
                                    w.isTemporarySafe(), w.getColorRgbOrDefault(), w.isToggledSafe() );
                            migrated = true;
                        }

                        boolean needsRewrite = false;
                        Boolean tmp = w.isTemporarySafe();
                        int color = w.getColorRgbOrDefault();
                        
                        if (w.getClass() == ChWaypoint.class) {}
                        map.put(w.getId(), new ChWaypoint(
                                w.getX(), w.getY(), w.getZ(),
                                w.getId(), w.getName() != null ? w.getName() : "Waypoint",
                                tmp, color, w.isToggledSafe()
                        ));
                        if (needsRewrite) migrated = true;
                    }
                    waypoints.addAll(map.values());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        publishWaypointsSnapshot();
        dirty.set(migrated);
        if (migrated) saveIfDirty();
        else dirty.set(false);
    }

    public synchronized void addWaypoint(ChWaypoint wp) {
        if (wp == null) return;
        waypoints.add(wp);
        publishWaypointsSnapshot();
        dirty.set(true);
    }

    /** Returns the last published immutable list snapshot. Waypoint objects retain their existing mutable compatibility API. */
    public List<ChWaypoint> getAll() {
        return publishedWaypoints;
    }

    public synchronized boolean removeById(String id) {
        if (id == null || id.isEmpty()) return false;
        boolean removed = waypoints.removeIf(w -> w != null && w.id != null && w.id.equalsIgnoreCase(id));
        if (removed) {
            publishWaypointsSnapshot();
            dirty.set(true);
        }
        return removed;
    }

    public synchronized void saveIfDirty() {
        if (!dirty.get()) return;
        saveForce();
    }

    public synchronized void saveForce() {
        initFile();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer w = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(tmp.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                StandardCharsets.UTF_8))) {
            gson.toJson(waypoints, w);
            w.flush();
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            dirty.set(false);
        } catch (Exception e) {
            e.printStackTrace();
            try { Files.deleteIfExists(tmp.toPath()); } catch (Exception ignore) {}
        }
    }

    public synchronized ChWaypoint findById(String id) {
        if (id == null || id.isEmpty()) return null;
        for (ChWaypoint w : waypoints) {
            if (w != null && w.getId() != null && w.getId().equalsIgnoreCase(id)) return w;
        }
        return null;
    }

    public synchronized boolean updateWaypoint(ChWaypoint updated) {
        if (updated == null || updated.id == null) return false;
        for (int i = 0; i < waypoints.size(); i++) {
            ChWaypoint w = waypoints.get(i);
            if (w != null && w.id != null && w.id.equalsIgnoreCase(updated.id)) {
                waypoints.set(i, updated);
                publishWaypointsSnapshot();
                markDirty();
                return true;
            }
        }
        return false;
    }

    public synchronized void removeAllTemp() {
        boolean removed = waypoints.removeIf(w -> w != null && w.isTemporarySafe());
        if (removed) {
            publishWaypointsSnapshot();
            dirty.set(true);
        }
    }

    public synchronized boolean hasTemp() {
        for (ChWaypoint w : waypoints) {
            if (w.isTemporarySafe()) return true;
        }
        return false;
    }

    private void publishWaypointsSnapshot() {
        publishedWaypoints = Collections.unmodifiableList(new ArrayList<>(waypoints));
    }

    public void markDirty() { dirty.set(true); }

}
