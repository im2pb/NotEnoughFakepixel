/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.crimson;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.utils.*;
import com.nef.notenoughfakepixel.variables.MobDisplayTypes;
import com.nef.notenoughfakepixel.variables.Skins;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class AshfangHelper {

    private final Waypoint[] waypoints = new Waypoint[2];

    @Getter
    private static int blazingSoulsCounter = 0;
    private final String underling = "§c§cAshfang Underling";
    private final String follower = "§c§8Ashfang Follower";
    private final String acolyte = "§c§9Ashfang Acolyte";
    private final String ashfangName = "ASHFANG";

    private static Entity currentAshfang;
    private static Entity currentGravityOrb;

    private Waypoint waypointAshfang;
    private Waypoint waypointGravityOrb;

    private static final Pattern ashfangHPPattern = Pattern.compile("([0-9]*[.,]?[0-9]*)([Mk])");
    private static final Pattern DAMAGE_RESTRICTION_PATTERN = Pattern.compile("can only be damaged by");
    private static final Pattern HIT_YOU_PATTERN = Pattern.compile("hit you for");
    private static final Pattern BLAZING_SOUL_PATTERN = Pattern.compile("The Blazing Soul dealt");
    private static final Color UNDERLING_COLOR = new Color(255, 0, 0, 150);
    private static final Color ACOLYTE_COLOR = new Color(0, 0, 255, 150);
    private static final Color FOLLOWER_COLOR = new Color(255, 255, 255, 255);
    private static final Color BLAZING_SOUL_COLOR = new Color(255, 255, 0, 255);
    private float lastHP = 50_000_000;
    /** Published on the client thread; PacketReadEvent is delivered on Netty. */
    private volatile boolean inCrimsonSkyBlock;

    public AshfangHelper() {
        this.waypoints[0] = new Waypoint("ASHFANG", new int[]{-484, 141, -1015});
        this.waypoints[1] = new Waypoint("GRAVITYORB", new int[]{-490, -200, -1015});
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        inCrimsonSkyBlock = !Crimson.checkEssentials();
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null || Crimson.checkEssentials()) {
            clearTrackedEntities();
            return;
        }
        if (!Crimson.checkAshfangArea(MathHelper.floor_double(mc.thePlayer.posX),
                MathHelper.floor_double(mc.thePlayer.posY), MathHelper.floor_double(mc.thePlayer.posZ))) {
            clearTrackedEntities();
            return;
        }
        // Update entity/waypoint state before rendering so a despawned boss is not
        // drawn for an extra frame and a newly found orb is visible immediately.
        renderEntities(event.partialTicks);
        if (Config.feature.crimson.crimsonAshfangWaypoint || Config.feature.crimson.crimsonGravityOrbWaypoint)
            drawWaypoints(event.partialTicks);
        checkHPForSound();
    }

    private void clearTrackedEntities() {
        currentAshfang = null;
        currentGravityOrb = null;
        waypointAshfang = null;
        waypointGravityOrb = null;
        blazingSoulsCounter = 0;
        lastHP = -1.0f;
    }

    @SubscribeEvent
    public void onChat(@NotNull ClientChatReceivedEvent e) {
        if (Crimson.checkEssentials()) return;
        if (Config.feature.crimson.crimsonAshfangMuteChat) {
            Matcher matcher = DAMAGE_RESTRICTION_PATTERN.matcher(e.message.getUnformattedText());
            Matcher matcher2 = HIT_YOU_PATTERN.matcher(e.message.getUnformattedText());
            if (matcher.find() || matcher2.find()) {
                e.setCanceled(true);
                return;
            }
        }
        if (Config.feature.crimson.crimsonAshfangMuteChat) {
            Matcher matcher3 = BLAZING_SOUL_PATTERN.matcher(e.message.getUnformattedText());
            if (matcher3.find()) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onSoundPacketReceive(PacketReadEvent event) {
        // PacketReadEvent is posted on Netty; the location state is snapshotted on client ticks.
        if (!inCrimsonSkyBlock || !Config.feature.crimson.crimsonAshfangMuteSound) return;
        Packet packet = event.packet;
        if (packet instanceof S29PacketSoundEffect) {
            S29PacketSoundEffect soundEffect = (S29PacketSoundEffect) packet;
            String soundName = soundEffect.getSoundName();
            
            if ("mob.bat.hurt".equals(soundName)) {
                if (event.isCancelable()) event.setCanceled(true);
            }
        }
    }

    private void renderEntities(float partialTicks) {
        int blazingSoulCount = 0;
        WorldClient world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            clearTrackedEntities();
            return;
        }

        boolean gravityFound = false;
        boolean ashfangFound = false;
        for (int i = 0; i < world.loadedEntityList.size(); i++) {
            Entity entity = world.loadedEntityList.get(i);
            if (entity == null) continue;
            String entityName = entity.getName();
            if (entityName == null) continue;
            if (!(entity instanceof EntityArmorStand)) continue;

            // Avoid allocating a BlockPos for every loaded entity on every render pass.
            int x = MathHelper.floor_double(entity.posX);
            int y = MathHelper.floor_double(entity.posY);
            int z = MathHelper.floor_double(entity.posZ);
            if (!Crimson.checkAshfangArea(x, y, z)) continue;

            ItemStack helmet = ((EntityArmorStand) entity).getEquipmentInSlot(4);
            if (helmet != null && helmet.getItem() == Items.skull) {
                if (!Config.feature.crimson.crimsonGravityOrbWaypoint) continue;
                String texture = ItemUtils.getSkullTexture(helmet);
                if (texture == null || texture.isEmpty() || !Skins.equalsSkin(texture, Skins.GRAVITY_ORB)) continue;

                gravityFound = true;
                boolean newOrb = currentGravityOrb == null
                        || !entity.getUniqueID().equals(currentGravityOrb.getUniqueID());
                if (newOrb) {
                    SoundUtils.playSound(new int[]{x, y, z}, "random.pop", 5.0f, 1.5f);
                    currentGravityOrb = entity;
                }
                if (waypointGravityOrb == null || !sameCoordinates(waypointGravityOrb, x, y, z)) {
                    waypointGravityOrb = new Waypoint("GRAVITYORB", new int[]{x, y, z});
                }
                continue;
            }

            if (entityName.contains("Ashfang") && !isNameAshfangMinion(entityName)) {
                ashfangFound = true;
                boolean newAshfang = currentAshfang == null
                        || !entity.getUniqueID().equals(currentAshfang.getUniqueID());
                if (newAshfang) {
                    // Do not compare the first observed HP sample against a previous boss.
                    lastHP = -1.0f;
                    currentAshfang = entity;
                }

                // Track HP for the overlay even when the waypoint is disabled.
                if (Config.feature.crimson.crimsonAshfangWaypoint) {
                    if (waypointAshfang == null || !sameCoordinates(waypointAshfang, x, y, z)) {
                        waypointAshfang = new Waypoint(ashfangName, new int[]{x, y, z});
                    }
                } else {
                    waypointAshfang = null;
                }
                continue;
            }

            if (entityName.contains(underling)) {
                if (Config.feature.crimson.crimsonAshfangHitboxes) {
                    RenderUtils.renderEntityHitbox(entity, partialTicks, UNDERLING_COLOR, MobDisplayTypes.BLAZE);
                }
            } else if (entityName.contains(acolyte)) {
                if (Config.feature.crimson.crimsonAshfangHitboxes) {
                    RenderUtils.renderEntityHitbox(entity, partialTicks, ACOLYTE_COLOR, MobDisplayTypes.BLAZE);
                }
            } else if (entityName.contains(follower)) {
                if (Config.feature.crimson.crimsonAshfangHitboxes) {
                    RenderUtils.renderEntityHitbox(entity, partialTicks, FOLLOWER_COLOR, MobDisplayTypes.BLAZE);
                }
            } else if (entityName.contains("Blazing Soul")) {
                // The overlay counter is informational and must not depend on hitbox rendering.
                blazingSoulCount++;
                if (Config.feature.crimson.crimsonAshfangHitboxes) {
                    RenderUtils.renderEntityHitbox(entity, partialTicks, BLAZING_SOUL_COLOR, MobDisplayTypes.BLAZINGSOUL);
                }
            }
        }

        blazingSoulsCounter = blazingSoulCount;
        if (!gravityFound) {
            waypointGravityOrb = null;
            currentGravityOrb = null;
        }
        if (!ashfangFound) {
            currentAshfang = null;
            waypointAshfang = null;
            lastHP = -1.0f;
        }
    }

    private boolean sameCoordinates(Waypoint waypoint, int x, int y, int z) {
        if (waypoint == null) return false;
        int[] coordinates = waypoint.getCoordinates();
        return coordinates != null && coordinates.length >= 3
                && coordinates[0] == x && coordinates[1] == y && coordinates[2] == z;
    }

    private boolean isNameAshfangMinion(String name) {
        return name.contains(underling) || name.contains(acolyte) || name.contains(follower);
    }

    private void drawWaypoint(Waypoint waypoint, float partialTicks) {
        if (waypoint == null) return;
        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        if (viewer == null) return;
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
        Color colorDrawWaypoint = new Color(255, 255, 255);
        int offsetBossY = 0;

        if (waypoint.getType().equals("GRAVITYORB"))
            colorDrawWaypoint = ColorUtils.getColor(Config.feature.crimson.crimsonBlazingSoulWaypointColor);
        if (waypoint.getType().equals(ashfangName)) {
            colorDrawWaypoint = ColorUtils.getColor(Config.feature.crimson.crimsonAshfangWaypointColor);
            offsetBossY = -6;
        }

        colorDrawWaypoint = new Color(colorDrawWaypoint.getRed(), colorDrawWaypoint.getGreen(), colorDrawWaypoint.getBlue(), 75);
        AxisAlignedBB bb = new AxisAlignedBB(
                waypoint.getCoordinates()[0] - viewerX,
                waypoint.getCoordinates()[1] - viewerY + offsetBossY,
                waypoint.getCoordinates()[2] - viewerZ,
                waypoint.getCoordinates()[0] + 1 - viewerX,
                waypoint.getCoordinates()[1] + 1 - viewerY + 150,
                waypoint.getCoordinates()[2] + 1 - viewerZ
        ).expand(0.01f, 0.01f, 0.01f);

        GlStateManager.disableCull();
        if (waypoint.getType().equals(ashfangName)) GlStateManager.disableDepth();
        RenderUtils.drawFilledBoundingBox(bb, 1f, colorDrawWaypoint);
        if (waypoint.getType().equals(ashfangName)) GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
    }

    private void drawWaypoints(float partialTicks) {
        if (Config.feature.crimson.crimsonAshfangWaypoint && !BossNotifier.getAshfangScheduled()[0]) {
            drawWaypoint(waypointAshfang, partialTicks);
        }
        if (Config.feature.crimson.crimsonGravityOrbWaypoint) {
            drawWaypoint(waypointGravityOrb, partialTicks);
        }
    }

    private void checkHPForSound() {
        float currentHP = getAshfangHP();
        if (currentHP < 0.0f) {
            lastHP = -1.0f;
            return;
        }
        if (lastHP >= 0.0f && currentHP < lastHP) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                int[] position = new int[]{MathHelper.floor_double(mc.thePlayer.posX),
                        MathHelper.floor_double(mc.thePlayer.posY),
                        MathHelper.floor_double(mc.thePlayer.posZ)};
                SoundUtils.playSound(position, "mob.wither.hurt", 0.7f, 0.7f);
            }
        }
        lastHP = currentHP;
    }

    public static float getAshfangHP() {
        if (currentAshfang == null) return -1.0f;
        String name = currentAshfang.getName();
        if (name == null || name.isEmpty()) return -1.0f;
        Matcher matcher = ashfangHPPattern.matcher(name);
        if (matcher.find()) {
            String numberString = matcher.group(1).replace(",", ""); 
            String ordinalString = matcher.group(2);
            try {
                float number = Float.parseFloat(numberString); 
                if (ordinalString == null) return number;
                if (ordinalString.equals("M")) return number * (float) Math.pow(10, 6);
                if (ordinalString.equals("k")) return number * (float) Math.pow(10, 3);
                return number;
            } catch (NumberFormatException e) {
                System.err.println("Error when converting " + numberString);
                
                return -1.0f;
            }
        }
        return -1.0f;
    }

    public static int getHitsNeeded() {
        float hp = getAshfangHP();
        if (hp < 0.0f) return -1;
        return (int) Math.ceil(hp / 2_000_000.0);
    }

}
