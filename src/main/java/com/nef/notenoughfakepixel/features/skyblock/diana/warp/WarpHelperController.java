/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana.warp;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.config.gui.core.config.KeybindHelper;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.opengl.GL11;

/** Owns warp destination selection, key activation, and the warp-helper HUD presentation. */
public final class WarpHelperController {
    private static final WarpLocation[] WARP_LOCATIONS = {
            new WarpLocation("Crypts", new Vec3(-187, 74, -86), "/warp crypts"),
            new WarpLocation("Hub", new Vec3(-2, 70, -68), "/warp hub"),
            new WarpLocation("Castle", new Vec3(-248, 130, 44), "/warp castle"),
            new WarpLocation("Dark Auction", new Vec3(91, 74, 173), "/warp da"),
            new WarpLocation("Museum", new Vec3(-69, 76, 80), "/warp museum")
    };

    private String displayText;
    private String warpCommand;
    private long cooldownEndTime;
    private boolean wasKeyPressed;

    /** Called on the client thread after Guess Burrow updates its current prediction. */
    public void update(Minecraft mc, Vec3 guessPoint) {
        if (mc == null) {
            return;
        }

        double playerDist = Double.NaN;
        WarpLocation bestWarp = null;
        double minWarpDist = Double.MAX_VALUE;

        boolean hub = SkyblockData.getCurrentLocation() != null && SkyblockData.getCurrentLocation().isHub();
        boolean featureEnabled = Config.feature.diana.dianaWarpHelper;
        boolean worldReady = mc.theWorld != null && mc.thePlayer != null;
        if (guessPoint != null && featureEnabled && hub && worldReady) {
            Vec3 playerPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
            playerDist = distance(playerPos, guessPoint);

            for (WarpLocation warp : WARP_LOCATIONS) {
                boolean isEnabled = true;
                if (warp.command.equals("/warp da")) {
                    isEnabled = Config.feature.diana.dianaWarpDa;
                } else if (warp.command.equals("/warp museum")) {
                    isEnabled = Config.feature.diana.dianaWarpMuseum;
                } else if (warp.command.equals("/warp crypts")) {
                    isEnabled = Config.feature.diana.dianaWarpCrypts;
                } else if (warp.command.equals("/warp castle")) {
                    isEnabled = Config.feature.diana.dianaWarpCastle;
                }

                double warpDist = distance(warp.pos, guessPoint);
                if (!isEnabled) continue;
                if (warpDist < minWarpDist) {
                    minWarpDist = warpDist;
                    bestWarp = warp;
                }
            }

            if (bestWarp != null && minWarpDist < playerDist) {
                String keyName = KeybindHelper.getKeyName(Config.feature.diana.warpKeybind);
                displayText = "Warp to " + bestWarp.name + " (" + (keyName != null ? keyName : "None") + ")";
                warpCommand = bestWarp.command;
            } else {
                displayText = null;
                warpCommand = null;
            }
        } else {
            displayText = null;
            warpCommand = null;
        }


        // Track the key edge even while there is no target or the chat screen is open.
        // Otherwise a held key can leave a stale edge state and block the next valid target.
        boolean keyPressed = KeybindHelper.isKeyDown(Config.feature.diana.warpKeybind);
        boolean chatOpen = mc.currentScreen instanceof GuiChat;
        if (chatOpen) {
            wasKeyPressed = keyPressed;
            return;
        }

        if (Config.feature.diana.dianaWarpHelper && warpCommand != null
                && keyPressed && !wasKeyPressed && System.currentTimeMillis() > cooldownEndTime) {
            mc.thePlayer.sendChatMessage(warpCommand);
            cooldownEndTime = System.currentTimeMillis() + 5000;
            resetPresentation();
        }
        wasKeyPressed = keyPressed;
    }

    public void renderOverlay(RenderGameOverlayEvent.Post event) {
        if (event == null || event.type != RenderGameOverlayEvent.ElementType.ALL || displayText == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.fontRendererObj == null) return;
        ScaledResolution sr = new ScaledResolution(mc);

        int textWidth = mc.fontRendererObj.getStringWidth(displayText);
        int textHeight = mc.fontRendererObj.FONT_HEIGHT;
        int x = Config.feature.diana.warpHelperPos.getAbsX(sr, textWidth);
        int y = Config.feature.diana.warpHelperPos.getAbsY(sr, textHeight);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(Config.feature.diana.warpHelperScale,
                Config.feature.diana.warpHelperScale, 1f);
        mc.fontRendererObj.drawStringWithShadow(displayText, 0, 0, 0xFFFFFF);
        GL11.glPopMatrix();
    }

    /** Clears visible state and the key edge; preserves the existing activation cooldown. */
    private void resetPresentation() {
        displayText = null;
        warpCommand = null;
        wasKeyPressed = false;
    }

    /** Clears a stale destination when the current Guess session is invalidated. */
    public void clearPrediction() {
        resetPresentation();
    }

    public void resetForLifecycle() {
        cooldownEndTime = 0L;
        resetPresentation();
    }

    private static double distance(Vec3 first, Vec3 second) {
        return first.distanceTo(second);
    }

    private static final class WarpLocation {
        private final String name;
        private final Vec3 pos;
        private final String command;

        private WarpLocation(String name, Vec3 pos, String command) {
            this.name = name;
            this.pos = pos;
            this.command = command;
        }
    }
}
