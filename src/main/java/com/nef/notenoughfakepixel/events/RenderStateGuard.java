/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.events;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

@RegisterEvents
public final class RenderStateGuard {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeGui(GuiScreenEvent.DrawScreenEvent.Pre event) {
        reset2DState();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterGui(GuiScreenEvent.DrawScreenEvent.Post event) {
        reset2DState();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeHud(RenderGameOverlayEvent.Pre event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            reset2DState();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterHud(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            reset2DState();
        }
    }

    private static void reset2DState() {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ZERO);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glLineWidth(1F);
    }
}
