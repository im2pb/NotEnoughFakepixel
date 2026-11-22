/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.performance;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.features.skyblock.qol.EntityTagFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.inventory.IInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnchantmentTable;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

@RegisterEvents
public final class PerformanceManager {
    private static final Minecraft MC = Minecraft.getMinecraft();

    private static int particleCount;
    private static boolean fancyGraphicsOverrideApplied;
    private static boolean savedFancyGraphics;
    private static boolean particleSettingOverrideApplied;
    private static int savedParticleSetting;
    private static boolean ambientOcclusionOverrideApplied;
    private static int savedAmbientOcclusion;

    private static boolean activeCached;
    private static boolean limitParticlesCached;
    private static boolean cullDecorativeCached;
    private static boolean maximumFpsDungeonEntityCullingCached;
    private static boolean inDungeonCached;
    private static boolean reduceBlockEntityCached;
    private static boolean disableHighlightsCached;
    private static boolean disableShadowsCached;
    private static boolean reduceBlockRenderDetailCached;
    private static boolean forceFastGraphicsCached;
    private static int particleLimitCached = 80;
    private static double decorativeDistanceSqCached = 32D * 32D;
    private static double blockEntityDistanceSqCached = 64D * 64D;

    /** Reused snapshot of named key/deployable markers, refreshed once per client tick. */
    private static final List<DungeonMarkerPosition> dungeonMarkerPositions = new ArrayList<>();
    private static final class DungeonMarkerPosition {
        final double x;
        final double y;
        final double z;

        DungeonMarkerPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private PerformanceManager() {
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        particleCount = 0;

        if (MC.thePlayer == null || MC.theWorld == null) {
            clearCachedSettings();
            restoreRenderOverrides();
            return;
        }

        refreshCachedSettings();
        refreshDungeonMarkerPositions();
        applyFastGraphicsIfNeeded();
    }

    public static boolean isActive() {
        return activeCached;
    }

    private static void refreshCachedSettings() {
        if (Config.feature == null || Config.feature.performance == null) {
            activeCached = false;
            maximumFpsDungeonEntityCullingCached = false;
            inDungeonCached = false;
            return;
        }

        boolean enabled = Config.feature.performance.enabled;
        boolean skyblock = true;
        if (enabled && Config.feature.performance.skyblockOnly) {
            try {
                skyblock = SkyblockData.getCurrentGamemode().isSkyblock();
            } catch (Throwable ignored) {
                skyblock = false;
            }
        }

        activeCached = enabled && skyblock;
        limitParticlesCached = Config.feature.performance.limitParticles;
        cullDecorativeCached = Config.feature.performance.cullDecorativeEntities;
        maximumFpsDungeonEntityCullingCached = Config.feature.performance.maximumFpsDungeonEntityCulling;
        try {
            inDungeonCached = SkyblockData.getCurrentLocation() != null
                    && SkyblockData.getCurrentLocation().isDungeon();
        } catch (Throwable ignored) {
            inDungeonCached = false;
        }
        reduceBlockEntityCached = Config.feature.performance.reduceBlockEntityDetail;
        disableHighlightsCached = Config.feature.performance.disableCustomEntityHighlights;
        disableShadowsCached = Config.feature.performance.disableEntityShadows;
        reduceBlockRenderDetailCached = Config.feature.performance.reduceBlockRenderDetail;
        forceFastGraphicsCached = Config.feature.performance.forceFastGraphics;

        particleLimitCached = Math.max(1, Config.feature.performance.particlesPerTick);
        double decorativeDistance = Math.max(8, Config.feature.performance.decorativeEntityDistance);
        double blockEntityDistance = Math.max(16, Config.feature.performance.blockEntityDistance);
        decorativeDistanceSqCached = decorativeDistance * decorativeDistance;
        blockEntityDistanceSqCached = blockEntityDistance * blockEntityDistance;
    }

    public static boolean shouldSuppressParticle(EnumParticleTypes type) {
        if (!activeCached || !limitParticlesCached) return false;

        if (particleCount >= particleLimitCached) return true;
        particleCount++;

        return type == null;
    }

    public static boolean shouldCullEntity(Entity entity) {
        if (entity == null) return false;

        // Bosses are important dungeon targets and must remain visible even in maximum-FPS mode.
        boolean dungeonBoss = entity instanceof EntityDragon || entity instanceof EntityWither;
        if (dungeonBoss) return false;

        // Preserve the local player and real server players. Hypixel player-model mobs can
        // also be EntityOtherPlayerMP instances, so class checks alone are insufficient:
        // only entities present in the server player list count as real players.
        boolean realPlayer = isRealPlayerEntity(entity);
        if (realPlayer) return false;

        // Hypixel player-model NPCs use EntityPlayer/EntityOtherPlayerMP and have skins,
        // but are not real players. Cull them explicitly BEFORE generic inventory/display
        // exceptions, so they cannot leak through the normal mob safety checks.
        if (DungeonEntityRenderPolicy.shouldCullPlayerModelNpc(
                maximumFpsDungeonEntityCullingCached,
                inDungeonCached,
                entity instanceof EntityPlayer,
                realPlayer
        )) {
            return true;
        }

        // Preserve dropped items, named key/deployable markers, item frames displaying an
        // item, and entities that actually own an inventory. Do NOT exempt ordinary mobs
        // merely because they wear armor or hold a weapon; those are still culled.
        if (isInventoryOrItemDisplayEntity(entity)) return false;

        // Some Hypixel pickups use a text marker and a separate, unnamed model entity.
        // Preserve nearby companion entities (item/block/skull models) before any culling
        // path, including damage-tag and decorative-distance filtering.
        if (isDungeonMarkerCompanion(entity) && isNearDungeonMarker(entity.posX, entity.posY, entity.posZ)) return false;

        // Damage-number armor stands are purely visual. When the existing toggle is on,
        // skip their entire renderer even if the broader low-end profile is disabled.
        if (EntityTagFilter.shouldSkipDamageTagModel(entity)) return true;

        // Inventory, chest and SkyBlock menu screens render their own ItemStacks through
        // RenderItem. Do not run world-entity culling while a container UI is open, so
        // preview entities and other UI-adjacent renderers cannot be affected either.
        if (InventoryRenderSafetyPolicy.shouldBypassForContainerGui(MC.currentScreen instanceof GuiContainer)) {
            return false;
        }

        // This is the dedicated maximum-FPS dungeon switch and intentionally does not
        // depend on the broader Low-End Optimizations master switch.
        if (DungeonEntityRenderPolicy.shouldCull(
                maximumFpsDungeonEntityCullingCached,
                inDungeonCached,
                realPlayer,
                dungeonBoss,
                entity instanceof EntityItem
        )) {
            return true;
        }

        if (!activeCached || MC.thePlayer == null) return false;

        double distanceSq = entity.getDistanceSqToEntity(MC.thePlayer);
        if (cullDecorativeCached && isDecorativeEntity(entity) && distanceSq > decorativeDistanceSqCached) {
            return true;
        }

        return false;
    }

    public static boolean shouldCullTileEntity(TileEntity tileEntity) {
        if (tileEntity == null) return false;

        // Never distance-cull inventory containers or interactive block entities. Chests,
        // ender chests, furnaces, hoppers, dispensers, brewing stands, beacons and signs
        // must remain rendered; their contents/state are managed by vanilla/server code.
        if (isInventoryOrInteractiveTileEntity(tileEntity)) return false;

        // A few deployable/key models are rendered as nearby tile entities (most often
        // skull models) while the visible label is a separate named entity.
        if (isNearDungeonMarker(tileEntity.getPos().getX() + 0.5D,
                tileEntity.getPos().getY() + 0.5D, tileEntity.getPos().getZ() + 0.5D)) return false;

        // While any chest/inventory/SkyBlock container screen is open, bypass all optional
        // tile-entity culling. GUI item icons are rendered separately, but this guard also
        // protects previews and menu-related block entities behind the screen.
        if (InventoryRenderSafetyPolicy.shouldBypassForContainerGui(MC.currentScreen instanceof GuiContainer)) {
            return false;
        }

        // Hypixel's AH/BZ search uses the vanilla sign editor with a temporary sign whose
        // world position can be outside the normal block-entity render radius. The editor
        // renders that sign through TileEntityRendererDispatcher, so distance culling can
        // make the sign model disappear even though the editor is open. Never cull the
        // sign currently being presented by GuiEditSign.
        if (SignEditorRenderGuard.shouldBypassCull(tileEntity, MC.currentScreen)) return false;

        if (!activeCached || !reduceBlockEntityCached || MC.thePlayer == null) return false;
        if (!isExpensiveTileEntity(tileEntity)) return false;

        double dx = tileEntity.getPos().getX() + 0.5D - MC.thePlayer.posX;
        double dy = tileEntity.getPos().getY() + 0.5D - MC.thePlayer.posY;
        double dz = tileEntity.getPos().getZ() + 0.5D - MC.thePlayer.posZ;
        return dx * dx + dy * dy + dz * dz > blockEntityDistanceSqCached;
    }

    public static boolean shouldSkipCustomEntityHighlights() {
        return activeCached && disableHighlightsCached;
    }

    public static boolean shouldSkipEntityShadows() {
        return activeCached && disableShadowsCached;
    }

    private static boolean isRealPlayerEntity(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return false;
        if (entity == MC.thePlayer) return true;
        if (MC.getNetHandler() == null) return false;
        try {
            NetworkPlayerInfo info = MC.getNetHandler().getPlayerInfo(((EntityPlayer) entity).getUniqueID());
            return info != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isInventoryOrItemDisplayEntity(Entity entity) {
        boolean droppedItem = entity instanceof EntityItem;
        boolean namedDungeonMarker = isNamedDungeonMarker(entity);
        boolean inventoryBearingEntity = entity instanceof IInventory;
        boolean itemFrameHasItem = entity instanceof EntityItemFrame
                && ((EntityItemFrame) entity).getDisplayedItem() != null;
        // Equipped mobs are intentionally NOT protected: their equipment must not
        // prevent dungeon culling. Only actual item displays and inventory owners survive.
        return namedDungeonMarker || InventoryRenderSafetyPolicy.mustAlwaysRenderEntity(
                droppedItem, inventoryBearingEntity, itemFrameHasItem, false
        );
    }

    /**
     * Hypixel uses named ArmorStands for certain dungeon pickups and deployables. These
     * may have no equipment exposed through EntityLivingBase.getEquipmentInSlot(), so
     * the ordinary item/equipment safety checks do not catch them. Preserve known markers
     * before the broad dungeon entity culling rule.
     */
    private static boolean isNamedDungeonMarker(Entity entity) {
        // Avoid constructing display-name components for every loaded entity on every
        // client tick. Hypixel's floating key/deployable labels use custom name tags.
        if (entity == null || !entity.hasCustomName()) return false;

        String name = entity.getCustomNameTag();
        if (name == null || name.trim().isEmpty()) {
            try {
                IChatComponent displayName = entity.getDisplayName();
                if (displayName != null) name = displayName.getFormattedText();
            } catch (Throwable ignored) {
                // Fall back to the entity's default name if a modded display name fails.
            }
        }
        if (name == null || name.trim().isEmpty()) {
            try {
                name = entity.getName();
            } catch (Throwable ignored) {
                return false;
            }
        }
        return DungeonEntityRenderPolicy.mustAlwaysRenderNamedDungeonMarker(name);
    }

    /**
     * Rebuild a tiny client-side snapshot of named dungeon markers. This scan happens once
     * per client tick, never once per rendered entity/frame, avoiding an O(n^2) render cost.
     */
    private static void refreshDungeonMarkerPositions() {
        dungeonMarkerPositions.clear();
        if (!maximumFpsDungeonEntityCullingCached || !inDungeonCached || MC.theWorld == null
                || MC.theWorld.loadedEntityList == null || MC.theWorld.loadedEntityList.isEmpty()) {
            return;
        }

        for (Object object : MC.theWorld.loadedEntityList) {
            if (!(object instanceof Entity)) continue;
            Entity candidate = (Entity) object;
            if (!isNamedDungeonMarker(candidate)) continue;
            dungeonMarkerPositions.add(new DungeonMarkerPosition(candidate.posX, candidate.posY, candidate.posZ));
        }
    }

    private static boolean isNearDungeonMarker(double x, double y, double z) {
        if (!inDungeonCached || dungeonMarkerPositions.isEmpty()) return false;
        for (DungeonMarkerPosition marker : dungeonMarkerPositions) {
            if (DungeonEntityRenderPolicy.isWithinNamedMarkerRadius(
                    x, y, z, marker.x, marker.y, marker.z)) return true;
        }
        return false;
    }

    private static boolean isDungeonMarkerCompanion(Entity entity) {
        // Preserve only plausible visual-model carriers near a named pickup marker.
        // Ordinary dungeon mobs near a key do not bypass culling, preserving FPS gains.
        return entity instanceof EntityArmorStand
                || entity instanceof EntityFallingBlock
                || entity instanceof EntityItemFrame;
    }


    private static boolean isInventoryOrInteractiveTileEntity(TileEntity tileEntity) {
        return InventoryRenderSafetyPolicy.mustAlwaysRenderTileEntity(
                tileEntity instanceof IInventory || tileEntity instanceof TileEntityChest,
                tileEntity instanceof TileEntityEnderChest,
                tileEntity instanceof TileEntitySign,
                tileEntity instanceof TileEntityEnchantmentTable,
                tileEntity instanceof TileEntityBeacon
        );
    }

    private static boolean isDecorativeEntity(Entity entity) {
        return entity instanceof EntityArmorStand
                || entity instanceof EntityItemFrame
                || entity instanceof EntityPainting
                || entity instanceof EntityLeashKnot;
    }

    private static boolean isExpensiveTileEntity(TileEntity tileEntity) {
        // Inventory/interactive tile entities are intentionally excluded above. Only
        // decorative skull tile renderers remain eligible for optional distance culling.
        return tileEntity instanceof TileEntitySkull;
    }

    /**
     * Applies temporary, reversible client render settings. AO and graphics quality affect
     * chunk mesh compilation, so chunk renderers are rebuilt only when a setting changes.
     * World simulation and packets are never modified by this block-render optimization.
     */
    private static void applyFastGraphicsIfNeeded() {
        if (MC.gameSettings == null) return;

        boolean reduceBlocks = activeCached && reduceBlockRenderDetailCached;
        boolean forceFast = activeCached && forceFastGraphicsCached;
        boolean reloadChunkMeshes = false;
        GameSettings settings = MC.gameSettings;

        // Fast graphics reduces fancy foliage/model variants. The focused block-detail
        // option also uses this setting but does not force the particle limit.
        boolean forceSimpleGraphics = reduceBlocks || forceFast;
        if (forceSimpleGraphics) {
            if (!fancyGraphicsOverrideApplied) {
                savedFancyGraphics = settings.fancyGraphics;
                fancyGraphicsOverrideApplied = true;
            } else if (settings.fancyGraphics) {
                // Preserve a user change made in the Options screen while the override is on.
                savedFancyGraphics = true;
            }
            if (settings.fancyGraphics) {
                settings.fancyGraphics = false;
                reloadChunkMeshes = true;
            }
        } else if (fancyGraphicsOverrideApplied) {
            settings.fancyGraphics = savedFancyGraphics;
            fancyGraphicsOverrideApplied = false;
            reloadChunkMeshes = true;
        }

        if (reduceBlocks) {
            if (!ambientOcclusionOverrideApplied) {
                savedAmbientOcclusion = settings.ambientOcclusion;
                ambientOcclusionOverrideApplied = true;
            } else if (settings.ambientOcclusion != 0) {
                // Remember user-selected AO changes while the temporary override is active.
                savedAmbientOcclusion = settings.ambientOcclusion;
            }
            if (settings.ambientOcclusion != 0) {
                settings.ambientOcclusion = 0;
                reloadChunkMeshes = true;
            }
        } else if (ambientOcclusionOverrideApplied) {
            settings.ambientOcclusion = savedAmbientOcclusion;
            ambientOcclusionOverrideApplied = false;
            reloadChunkMeshes = true;
        }

        if (forceFast) {
            if (!particleSettingOverrideApplied) {
                savedParticleSetting = settings.particleSetting;
                particleSettingOverrideApplied = true;
            } else if (settings.particleSetting != 2) {
                savedParticleSetting = settings.particleSetting;
            }
            settings.particleSetting = 2;
        } else if (particleSettingOverrideApplied) {
            settings.particleSetting = savedParticleSetting;
            particleSettingOverrideApplied = false;
        }

        if (reloadChunkMeshes && MC.renderGlobal != null) {
            try {
                MC.renderGlobal.loadRenderers();
            } catch (Throwable ignored) {
                // The next world load naturally rebuilds the meshes if shutdown has begun.
            }
        }
    }

    private static void clearCachedSettings() {
        activeCached = false;
        limitParticlesCached = false;
        cullDecorativeCached = false;
        maximumFpsDungeonEntityCullingCached = false;
        inDungeonCached = false;
        dungeonMarkerPositions.clear();
        reduceBlockEntityCached = false;
        disableHighlightsCached = false;
        disableShadowsCached = false;
        reduceBlockRenderDetailCached = false;
        forceFastGraphicsCached = false;
    }

    private static void restoreRenderOverrides() {
        if (MC.gameSettings == null) return;
        boolean reloadChunkMeshes = false;

        if (fancyGraphicsOverrideApplied) {
            MC.gameSettings.fancyGraphics = savedFancyGraphics;
            fancyGraphicsOverrideApplied = false;
            reloadChunkMeshes = true;
        }
        if (ambientOcclusionOverrideApplied) {
            MC.gameSettings.ambientOcclusion = savedAmbientOcclusion;
            ambientOcclusionOverrideApplied = false;
            reloadChunkMeshes = true;
        }
        if (particleSettingOverrideApplied) {
            MC.gameSettings.particleSetting = savedParticleSetting;
            particleSettingOverrideApplied = false;
        }

        if (reloadChunkMeshes && MC.renderGlobal != null) {
            try {
                MC.renderGlobal.loadRenderers();
            } catch (Throwable ignored) {
                // The next world load naturally rebuilds the meshes if shutdown has begun.
            }
        }
    }
}
