/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.RenderEntityModelEvent;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.*;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import com.nef.notenoughfakepixel.variables.Location;
import com.nef.notenoughfakepixel.variables.Skins;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.init.Items;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.lwjgl.opengl.GL11;

@RegisterEvents
public class DragonCloseAlert {

    @Getter
    public static DragonCloseAlert INSTANCE;

    public DragonCloseAlert() {
        INSTANCE = this;
    }

    @AllArgsConstructor
    @Data
    private static class Orb {
        private final BlockPos pos;
        private final Skins skin;
        private final Color color;
    }

    private static final List<Orb> ORBS = Arrays.asList(
            new Orb(new BlockPos(43,6,64), Skins.RED_RELIC, Color.RED),
            new Orb(new BlockPos(43,6,102), Skins.GREEN_RELIC, Color.GREEN),
            new Orb(new BlockPos(85, 6, 102), Skins.BLUE_RELIC, Color.CYAN),
            new Orb(new BlockPos(85, 6, 64), Skins.ORANGE_RELIC, Color.ORANGE),
            new Orb(new BlockPos(64, 6, 125), Skins.PURPLE_RELIC, Color.PINK)
    );

    private static final Map<EntityDragon, Color> DRAGON_COLOR_MAP = new HashMap<>();
    private static final Map<EntityDragon, String> DRAGON_HEALTH_MAP = new HashMap<>();
    private static final Map<EntityArmorStand, Color> DRAGON_STATUE_COLOR_MAP = new HashMap<>();

    public static final Map<String, Color> DRAGON_COLORS = MapUtils.mapOf(
            new MapUtils.Pair<>("Apex Dragon", Color.GREEN),
            new MapUtils.Pair<>("Flame Dragon", Color.ORANGE),
            new MapUtils.Pair<>("Power Dragon", Color.RED),
            new MapUtils.Pair<>("Soul Dragon", Color.PINK),
            new MapUtils.Pair<>("Ice Dragon", Color.CYAN)
    );

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onUnLoad(WorldEvent.Unload e){
        DRAGON_COLOR_MAP.clear();
        DRAGON_HEALTH_MAP.clear();
        DRAGON_STATUE_COLOR_MAP.clear();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null
                || SkyblockData.getCurrentLocation() != Location.DUNGEON
                || SkyblockData.getCurrentFloor() != DungeonFloor.M7) {
            DRAGON_COLOR_MAP.clear();
            DRAGON_STATUE_COLOR_MAP.clear();
            return;
        }

        refreshDragonAssociations();
    }

    private void refreshDragonAssociations() {
        Map<EntityDragon, Color> dragons = new HashMap<>();
        Map<EntityArmorStand, Color> statues = new HashMap<>();

        List<Entity> loadedEntities = new java.util.ArrayList<>(mc.theWorld.loadedEntityList);
        for (Entity entity : loadedEntities) {
            if (!(entity instanceof EntityArmorStand)) continue;

            EntityArmorStand statue = (EntityArmorStand) entity;
            Color color = getDragonStatueColor(statue);
            if (color == null) continue;

            statues.put(statue, color);

            EntityDragon nearest = findNearestLivingDragon(statue);
            if (nearest != null) {
                dragons.put(nearest, color);
            }
        }

        DRAGON_STATUE_COLOR_MAP.clear();
        DRAGON_STATUE_COLOR_MAP.putAll(statues);
        DRAGON_COLOR_MAP.clear();
        DRAGON_COLOR_MAP.putAll(dragons);
    }

    private EntityDragon findNearestLivingDragon(EntityArmorStand statue) {
        AxisAlignedBB searchBox = statue.getEntityBoundingBox().expand(2.5D, 3.5D, 2.5D);
        EntityDragon nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (EntityDragon dragon : mc.theWorld.getEntitiesWithinAABB(EntityDragon.class, searchBox)) {
            if (dragon == null || dragon.isDead || dragon.getHealth() <= 0.1F) continue;
            double distance = dragon.getDistanceSqToEntity(statue);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = dragon;
            }
        }

        return nearest;
    }

    private Color getDragonStatueColor(EntityArmorStand stand) {
        ItemStack skull = stand.getCurrentArmor(3);
        if (skull == null || skull.getItem() != Items.skull || skull.getMetadata() != 3) return null;

        String texture = ItemUtils.getSkullTexture(skull);
        if (texture == null || texture.isEmpty()) return null;

        Skins skin = Skins.getSkinByValue(texture);
        if (skin == null) return null;

        for (Orb orb : ORBS) {
            if (orb.getSkin() == skin) return orb.getColor();
        }

        return null;
    }

    private Color resolveDragonColor(EntityDragon dragon) {
        if (dragon == null || mc.theWorld == null) return null;

        AxisAlignedBB searchBox = dragon.getEntityBoundingBox().expand(2.5D, 3.5D, 2.5D);
        EntityArmorStand nearestStatue = null;
        double nearestDistance = Double.MAX_VALUE;

        for (EntityArmorStand statue : mc.theWorld.getEntitiesWithinAABB(EntityArmorStand.class, searchBox)) {
            Color color = getDragonStatueColor(statue);
            if (color == null) continue;

            double distance = dragon.getDistanceSqToEntity(statue);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestStatue = statue;
            }
        }

        if (nearestStatue == null) return null;
        Color color = DRAGON_STATUE_COLOR_MAP.get(nearestStatue);
        if (color == null) color = getDragonStatueColor(nearestStatue);
        if (color != null) {
            DRAGON_STATUE_COLOR_MAP.put(nearestStatue, color);
            DRAGON_COLOR_MAP.put(dragon, color);
        }
        return color;
    }

    public List<EntityDragon> getDragonsByColor(Color color) {
        return DRAGON_COLOR_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().equals(color))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public void registerDragon(EntityDragon dragon, String health) {
        DRAGON_HEALTH_MAP.put(dragon, health);
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent e) {
        if (SkyblockData.getCurrentLocation() != Location.DUNGEON ||
                SkyblockData.getCurrentFloor() != DungeonFloor.M7) return;

        DRAGON_COLOR_MAP.keySet().removeIf(this::isDying);
        DRAGON_HEALTH_MAP.keySet().removeIf(this::isDying);

        renderBoxes(e);
        renderDragonDistance(e);
    }

    private void renderDragonDistance(RenderWorldLastEvent e) {
        ORBS.forEach(orb -> {
            BlockPos pos = orb.getPos().add(0, 20, 0);

            for (EntityDragon dragon : DRAGON_COLOR_MAP.keySet()) {
                if (isDying(dragon)) continue;

                Color color = DRAGON_COLOR_MAP.get(dragon);
                if (color == null || !color.equals(orb.getColor())) continue;

                String health = DRAGON_HEALTH_MAP.get(dragon);
                if (health != null && !health.isEmpty()) {
                    RenderUtils.renderWaypointText(health, dragon.getPosition(), e.partialTicks, false);
                }

                double distance = new Vec3(pos.getX(), pos.getY(), pos.getZ())
                        .distanceTo(new Vec3(dragon.posX, dragon.posY, dragon.posZ));

                if (distance < 20) {
                    String dragonName = DRAGON_COLORS.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(color))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse("Dragon");

                }
            }
        });
    }

    private void renderBoxes(RenderWorldLastEvent e) {
        if (Config.feature.dungeons.distBox) {
            renderDragonStatueBoxes(e);
        }

        if (!Config.feature.dungeons.m7Relics) return;
        for (Orb orb : ORBS) {
            Color color = orb.getColor();
            BlockPos position = orb.getPos().add(0, 20, 0);
            if (orb.getSkin().equals(Skins.BLUE_RELIC) || orb.getSkin().equals(Skins.ORANGE_RELIC)) {
                RenderUtils.renderBoxAtCoords(
                        position.getX() - 21, position.getY() - 11, position.getZ() - 11,
                        position.getX() + 11, position.getY() + 11, position.getZ() + 13,
                        e.partialTicks, color, false
                );
            }

            if (orb.getSkin().equals(Skins.GREEN_RELIC) || orb.getSkin().equals(Skins.RED_RELIC)) {
                RenderUtils.renderBoxAtCoords(
                        position.getX() - 11, position.getY() - 11, position.getZ() - 11,
                        position.getX() + 21, position.getY() + 11, position.getZ() + 13,
                        e.partialTicks, color, false
                );
            }

            if (orb.getSkin().equals(Skins.PURPLE_RELIC)) {
                RenderUtils.renderBoxAtCoords(
                        position.getX() - 11, position.getY() - 11, position.getZ() - 21,
                        position.getX() + 13, position.getY() + 11, position.getZ() + 11,
                        e.partialTicks, color, false
                );
            }
        }
    }

    private void renderDragonStatueBoxes(RenderWorldLastEvent e) {
        for (Map.Entry<EntityArmorStand, Color> entry : DRAGON_STATUE_COLOR_MAP.entrySet()) {
            EntityArmorStand statue = entry.getKey();
            if (statue == null || statue.isDead) continue;

            AxisAlignedBB box = statue.getEntityBoundingBox().expand(0.7D, 0.15D, 0.7D);
            RenderUtils.renderBoxAtCoords(
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    e.partialTicks, entry.getValue(), true
            );
        }
    }

    @SubscribeEvent
    public void render(RenderEntityModelEvent e) {
        if (!Config.feature.dungeons.dragOutline) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (SkyblockData.getCurrentLocation() != Location.DUNGEON
                || SkyblockData.getCurrentFloor() != DungeonFloor.M7) return;

        EntityLivingBase entity = e.getEntity();
        if (!(entity instanceof EntityDragon) || entity.isInvisible() || isDying(entity)) return;

        EntityDragon dragon = (EntityDragon) entity;
        Color color = DRAGON_COLOR_MAP.get(dragon);
        if (color == null) {
            color = resolveDragonColor(dragon);
        }
        if (color == null) return;

        renderDragonOutline(e, color);
    }

    private void renderDragonOutline(RenderEntityModelEvent e, Color color) {
        EntityLivingBase entity = e.getEntity();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = color.getAlpha() / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GlStateManager.color(r, g, b, a * 0.45F);
        GlStateManager.scale(1.045F, 1.045F, 1.045F);
        e.getModel().render(entity, e.getLimbSwing(), e.getLimbSwingAmount(), e.getAgeInTicks(),
                e.getHeadYaw(), e.getHeadPitch(), e.getScaleFactor());

        GlStateManager.color(r, g, b, a);
        GlStateManager.scale(1.02F / 1.045F, 1.02F / 1.045F, 1.02F / 1.045F);
        e.getModel().render(entity, e.getLimbSwing(), e.getLimbSwingAmount(), e.getAgeInTicks(),
                e.getHeadYaw(), e.getHeadPitch(), e.getScaleFactor());

        GL11.glEnable(GL11.GL_CULL_FACE);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private boolean isDying(EntityLivingBase entity) {
        return entity == null || entity.isDead || entity.getHealth() <= 0.1f;
    }
}
