/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.render;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.events.RenderEntityModelEvent;
import com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.detection.InquisitorOutlineTracker;
import com.nef.notenoughfakepixel.utils.ColorUtils;
import com.nef.notenoughfakepixel.utils.EntityHighlightUtils;
import com.nef.notenoughfakepixel.utils.OutlineUtils;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

import java.awt.Color;

/** Renders only the entities discovered by InquisitorOutlineTracker. */
public final class InquisitorOutlineRenderer {
    private final InquisitorOutlineTracker tracker;

    public InquisitorOutlineRenderer(InquisitorOutlineTracker tracker) {
        if (tracker == null) throw new IllegalArgumentException("Inquisitor tracker cannot be null");
        this.tracker = tracker;
    }

    public void onRenderEntityModel(RenderEntityModelEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || mc.theWorld == null || event == null) return;
        if (!SkyblockData.getCurrentLocation().isHub()) return;
        if (!Config.feature.diana.dianaMinosInquisitorOutline) return;
        EntityLivingBase entity = event.getEntity();
        if (!tracker.contains(entity) || entity.isInvisible()) return;

        // Keep the outline tied to the existing Dungeons starred-box color setting.
        Color color = new Color(ColorUtils.getColor(Config.feature.dungeons.dungeonsStarredBoxColor).getRGB());
        if (Configuration.isPojav()) {
            EntityHighlightUtils.renderEntityOutline(event, color);
        } else {
            OutlineUtils.outlineEntity(event, 5.0f, color, true);
        }
    }
}
