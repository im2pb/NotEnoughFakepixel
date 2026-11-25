package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.variables.Resources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec4b;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.Map;

@RegisterEvents
public class DungeonsMap {

    private static final float PLAYER_MARKER_SCALE = 1.4F;
    private static final float OTHERS_MARKER_SCALE = 1.25F;
    private static final ResourceLocation MAP_ICONS_TEXTURE = Resources.MAP_ICONS.getResource();
    private static final Minecraft MC = Minecraft.getMinecraft();

    private final Tessellator tessellator = Tessellator.getInstance();
    private final WorldRenderer worldRenderer = tessellator.getWorldRenderer();
    private double playerPositionX;
    private double playerPositionY;
    private boolean finalScreen = false;
    private String cachedBorderColorConfig;
    private float cachedBorderAlpha = 1.0F;
    private float cachedBorderRed = 1.0F;
    private float cachedBorderGreen = 1.0F;
    private float cachedBorderBlue = 1.0F;

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!Config.feature.dungeons.map.dungeonsMap) return;
        if (!DungeonManager.checkEssentials()) return;
        if (MC.thePlayer == null || MC.theWorld == null) return;

        ItemStack map = MC.thePlayer.inventory.getStackInSlot(8);
        if (map == null || !(map.getItem() instanceof ItemMap)) return;

        MapData data = ((ItemMap) map.getItem()).getMapData(map, MC.theWorld);
        if (data == null) return;

        MapLayout layout = createLayout(new ScaledResolution(MC));
        applyScissor(layout);
        try {
            drawMap(data, layout);
            drawMarkers(data.mapDecorations, layout);
        } finally {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        drawBorderMap(layout);
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!DungeonManager.checkEssentials()) return;
        String message = event.message.getUnformattedText();
        if (message.contains("> EXTRA STATS <")) {
            finalScreen = true;
        } else if (message.equals("[NPC] Mort: Good luck.")) {
            finalScreen = false;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!DungeonManager.checkEssentials()) return;
        finalScreen = false;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (!DungeonManager.checkEssentials()) return;
        finalScreen = false;
    }

    private void drawMap(MapData data, MapLayout layout) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(layout.x, layout.y, 0.0F);
        GlStateManager.scale(layout.scale, layout.scale, layout.scale);

        if (Config.feature.dungeons.map.dungeonsRotateMap && !finalScreen) {
            float angle = -MathHelper.wrapAngleTo180_float(MC.thePlayer.rotationYaw);
            GlStateManager.translate(64.0F, 64.0F, 0.0F);
            GlStateManager.rotate(angle + 180.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(-64.0F, -64.0F, 0.0F);
            GlStateManager.translate(64.0F - (float) playerPositionX, 64.0F - (float) playerPositionY, 0.0F);
        }

        MC.entityRenderer.getMapItemRenderer().renderMap(data, false);
        GlStateManager.popMatrix();
    }

    private void drawBorderMap(MapLayout layout) {
        updateBorderColorCache();

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.color(cachedBorderRed, cachedBorderGreen, cachedBorderBlue, cachedBorderAlpha);
        GL11.glLineWidth(2.0F);

        worldRenderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);
        worldRenderer.pos(layout.x, layout.y, 0.0D).endVertex();
        worldRenderer.pos(layout.x + layout.mapWidth, layout.y, 0.0D).endVertex();
        worldRenderer.pos(layout.x + layout.mapWidth, layout.y + layout.mapHeight, 0.0D).endVertex();
        worldRenderer.pos(layout.x, layout.y + layout.mapHeight, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private void drawMarkers(Map<String, Vec4b> mapDecorations, MapLayout layout) {
        int colorIndex = 0;
        final boolean rotateMap = Config.feature.dungeons.map.dungeonsRotateMap && !finalScreen;
        final float mapScale = layout.scale;
        final float playerAngle = MathHelper.wrapAngleTo180_float(MC.thePlayer.rotationYaw);
        final int decorationCount = mapDecorations.size();

        for (Map.Entry<String, Vec4b> entry : mapDecorations.entrySet()) {
            Vec4b decoration = entry.getValue();
            byte iconType = decoration.func_176110_a();
            if (iconType == 3) iconType = 0;

            double markerX = decoration.func_176112_b() / 2.0F + 64.0F;
            double markerY = decoration.func_176113_c() / 2.0F + 64.0F;
            if (iconType == 1) {
                playerPositionX = markerX;
                playerPositionY = markerY;
            }

            GlStateManager.pushMatrix();
            translateMarker(layout, mapScale, rotateMap, iconType, markerX, markerY, playerAngle);

            float angle = 180.0F;
            if (!rotateMap && iconType == 1) {
                angle = playerAngle;
            }
            if (iconType == 0) {
                angle = decoration.func_176111_d() * 360.0F / 16.0F;
                if (rotateMap) {
                    angle = angle + 180.0F - playerAngle;
                }
            }

            GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
            if (iconType == 1) {
                GlStateManager.scale(mapScale * 4.0F * PLAYER_MARKER_SCALE, mapScale * 4.0F * PLAYER_MARKER_SCALE, 3.0F * PLAYER_MARKER_SCALE);
            } else if (iconType == 0) {
                GlStateManager.scale(mapScale * 4.0F * OTHERS_MARKER_SCALE, mapScale * 4.0F * OTHERS_MARKER_SCALE, 3.0F * OTHERS_MARKER_SCALE);
            }

            float u0 = (iconType % 4) / 4.0F;
            float v0 = (iconType / 4) / 4.0F;
            float u1 = (iconType % 4 + 1) / 4.0F;
            float v1 = (iconType / 4 + 1) / 4.0F;

            GlStateManager.translate(-0.125F, 0.125F, 0.0F);
            MC.getTextureManager().bindTexture(MAP_ICONS_TEXTURE);

            if (iconType == 0) {
                switch (colorIndex) {
                    case 0:
                        GlStateManager.color(0.0F, 0.0F, 1.0F, 1.0F);
                        break;
                    case 1:
                        GlStateManager.color(1.0F, 1.0F, 0.0F, 1.0F);
                        break;
                    case 2:
                        GlStateManager.color(1.0F, 0.5F, 0.0F, 1.0F);
                        break;
                    case 3:
                        GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);
                        break;
                    default:
                        break;
                }
                colorIndex = decorationCount > 1 ? (colorIndex + 1) % (decorationCount - 1) : 0;
            }

            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            worldRenderer.pos(-1.0D, 1.0D, -0.001D).tex(u0, v0).endVertex();
            worldRenderer.pos(1.0D, 1.0D, -0.001D).tex(u1, v0).endVertex();
            worldRenderer.pos(1.0D, -1.0D, -0.001D).tex(u1, v1).endVertex();
            worldRenderer.pos(-1.0D, -1.0D, -0.001D).tex(u0, v1).endVertex();
            tessellator.draw();

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
    }

    private void translateMarker(MapLayout layout, float mapScale, boolean rotateMap, byte iconType,
                                 double markerX, double markerY, float playerAngle) {
        if (!rotateMap) {
            GlStateManager.translate(layout.x + markerX * mapScale, layout.y + markerY * mapScale, 0.0D);
            return;
        }

        if (iconType == 1) {
            GlStateManager.translate(layout.x + 64.0F * mapScale, layout.y + 64.0F * mapScale, 0.0D);
            return;
        }

        if (iconType == 0) {
            float relativeX = (float) (markerX - 64.0D + (64.0D - playerPositionX));
            float relativeY = (float) (markerY - 64.0D + (64.0D - playerPositionY));
            float angleRad = (float) Math.toRadians(-playerAngle);
            float cos = MathHelper.cos(angleRad);
            float sin = MathHelper.sin(angleRad);
            float rotatedX = relativeX * cos - relativeY * sin;
            float rotatedY = relativeX * sin + relativeY * cos;
            GlStateManager.translate(layout.x + (64.0F - rotatedX) * mapScale, layout.y + (64.0F - rotatedY) * mapScale, 0.0D);
        }
    }

    private void updateBorderColorCache() {
        String borderColorConfig = Config.feature.dungeons.map.dungeonsMapBorderColor;
        if (borderColorConfig.equals(cachedBorderColorConfig)) {
            return;
        }

        String[] colorParts = borderColorConfig.split(":");
        cachedBorderAlpha = Float.parseFloat(colorParts[1]) / 255.0F;
        cachedBorderRed = Float.parseFloat(colorParts[2]) / 255.0F;
        cachedBorderGreen = Float.parseFloat(colorParts[3]) / 255.0F;
        cachedBorderBlue = Float.parseFloat(colorParts[4]) / 255.0F;
        cachedBorderColorConfig = borderColorConfig;
    }

    private static void applyScissor(MapLayout layout) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                layout.scissorX,
                MC.displayHeight - (layout.scissorY + layout.scissorHeight),
                layout.scissorWidth,
                layout.scissorHeight
        );
    }

    private static MapLayout createLayout(ScaledResolution sr) {
        float scale = Config.feature.dungeons.map.dungeonsMapScale;
        int mapWidth = (int) (128 * scale);
        int mapHeight = (int) (128 * scale);
        float x = Config.feature.dungeons.map.dungeonsMapPos.getAbsX(sr, mapWidth);
        float y = Config.feature.dungeons.map.dungeonsMapPos.getAbsY(sr, mapHeight);

        if (Config.feature.dungeons.map.dungeonsMapPos.isCenterX()) {
            x -= mapWidth / 2.0F;
        }
        if (Config.feature.dungeons.map.dungeonsMapPos.isCenterY()) {
            y -= mapHeight / 2.0F;
        }

        int scaleFactor = sr.getScaleFactor();
        return new MapLayout(
                x,
                y,
                scale,
                mapWidth,
                mapHeight,
                (int) (x * scaleFactor),
                (int) (y * scaleFactor),
                (int) (mapWidth * scaleFactor),
                (int) (mapHeight * scaleFactor)
        );
    }

    private static final class MapLayout {
        private final float x;
        private final float y;
        private final float scale;
        private final int mapWidth;
        private final int mapHeight;
        private final int scissorX;
        private final int scissorY;
        private final int scissorWidth;
        private final int scissorHeight;

        private MapLayout(float x, float y, float scale, int mapWidth, int mapHeight,
                          int scissorX, int scissorY, int scissorWidth, int scissorHeight) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.mapWidth = mapWidth;
            this.mapHeight = mapHeight;
            this.scissorX = scissorX;
            this.scissorY = scissorY;
            this.scissorWidth = scissorWidth;
            this.scissorHeight = scissorHeight;
        }
    }
}
