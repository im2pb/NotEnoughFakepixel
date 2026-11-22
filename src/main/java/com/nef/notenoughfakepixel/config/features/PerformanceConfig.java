/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorBoolean;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigEditorSlider;
import com.nef.notenoughfakepixel.config.gui.core.config.annotations.ConfigOption;

public class PerformanceConfig {

    @Expose
    @ConfigOption(name = "Enable Low-End Optimizations", desc = "Enable the optional low-end rendering and update optimizations.")
    @ConfigEditorBoolean
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "SkyBlock Only", desc = "Apply these optimizations only while in SkyBlock.")
    @ConfigEditorBoolean
    public boolean skyblockOnly = true;

    @Expose
    @ConfigOption(name = "Disable Custom Entity Highlights", desc = "Disable NEF's custom mob brightness/highlight pass to reduce render-state work.")
    @ConfigEditorBoolean
    public boolean disableCustomEntityHighlights = true;

    @Expose
    @ConfigOption(name = "Maximum FPS Dungeon Entity Culling", desc = "Hides regular mobs and player-model NPCs in dungeons. Bosses, players, items, and important markers stay visible.")
    @ConfigEditorBoolean
    public boolean maximumFpsDungeonEntityCulling = true;

    @Expose
    @ConfigOption(name = "Limit Particles", desc = "Cap client particle creation to reduce particle spikes.")
    @ConfigEditorBoolean
    public boolean limitParticles = true;

    @Expose
    @ConfigOption(name = "Particles Per Tick", desc = "Maximum particles NEF allows per client tick.")
    @ConfigEditorSlider(minValue = 8f, maxValue = 256f, minStep = 8f)
    public int particlesPerTick = 80;

    @Expose
    @ConfigOption(name = "Reduce Block Render Detail", desc = "Turns off smooth lighting and fancy block rendering while low-end mode is on.")
    @ConfigEditorBoolean
    public boolean reduceBlockRenderDetail = true;

    @Expose
    @ConfigOption(name = "Cull Decorative Entities", desc = "Stop rendering distant armor stands, item frames and paintings.")
    @ConfigEditorBoolean
    public boolean cullDecorativeEntities = false;

    @Expose
    @ConfigOption(name = "Decorative Entity Distance", desc = "Maximum render distance for decorative entities while culling is enabled.")
    @ConfigEditorSlider(minValue = 8f, maxValue = 128f, minStep = 4f)
    public int decorativeEntityDistance = 32;

    @Expose
    @ConfigOption(name = "Reduce Block Entity Detail", desc = "Hides distant decorative skulls while keeping containers and important blocks visible.")
    @ConfigEditorBoolean
    public boolean reduceBlockEntityDetail = false;

    @Expose
    @ConfigOption(name = "Block Entity Distance", desc = "Maximum render distance for selected block entities while detail reduction is enabled.")
    @ConfigEditorSlider(minValue = 16f, maxValue = 128f, minStep = 8f)
    public int blockEntityDistance = 64;

    @Expose
    @ConfigOption(name = "Disable Entity Shadows", desc = "Skip vanilla entity shadow geometry generation.")
    @ConfigEditorBoolean
    public boolean disableEntityShadows = false;

    @Expose
    @ConfigOption(name = "Force Fast Graphics", desc = "Use vanilla fast graphics and minimal particles while low-end mode is active.")
    @ConfigEditorBoolean
    public boolean forceFastGraphics = false;

}
