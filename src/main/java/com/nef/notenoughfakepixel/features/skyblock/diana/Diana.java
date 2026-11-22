/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.diana;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.features.skyblock.diana.burrows.ParticleProcessor;
import com.nef.notenoughfakepixel.features.skyblock.diana.core.DianaRuntimeContext;
import com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.alert.MinosInquisitorAlertController;
import com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.detection.InquisitorLocationRegistry;
import com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.detection.InquisitorOutlineTracker;
import com.nef.notenoughfakepixel.features.skyblock.diana.inquisitor.render.InquisitorOutlineRenderer;
import com.nef.notenoughfakepixel.features.skyblock.diana.mobs.gaia.GaiaConstruct;
import com.nef.notenoughfakepixel.features.skyblock.diana.mobs.gaia.GaiaTracker;
import com.nef.notenoughfakepixel.features.skyblock.diana.mobs.DianaMobTrackingService;
import com.nef.notenoughfakepixel.features.skyblock.diana.mobs.siamese.SiameseTracker;
import com.nef.notenoughfakepixel.features.skyblock.diana.render.BurrowWaypointRenderer;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.InventoryUtils;
import com.nef.notenoughfakepixel.utils.Waypoint;
import com.nef.notenoughfakepixel.events.PacketReadEvent;
import com.nef.notenoughfakepixel.events.RenderEntityModelEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Diana's Forge event adapter. Feature behavior lives in focused domain services;
 * this class only routes lifecycle and event callbacks to their owners.
 */
public final class Diana {
    private final DianaRuntimeContext runtimeContext;
    private final ParticleProcessor processor = new ParticleProcessor();

    private final GaiaTracker gaiaTracker = new GaiaTracker();
    private final SiameseTracker siameseTracker = new SiameseTracker();
    private final DianaMobTrackingService mobTrackingService = new DianaMobTrackingService(gaiaTracker, siameseTracker);
    private final BurrowWaypointRenderer waypointRenderer = new BurrowWaypointRenderer(processor);

    private final InquisitorLocationRegistry inquisitorLocations = new InquisitorLocationRegistry();
    private final InquisitorOutlineTracker inquisitorOutlineTracker = new InquisitorOutlineTracker();
    private final MinosInquisitorAlertController inquisitorAlertController;
    private final InquisitorOutlineRenderer inquisitorOutlineRenderer;

    private final Pattern cooldownPattern =
            Pattern.compile("§r§cThis ability is on cooldown for \\d+ more second(?:s)?\\.§r");
    private int mobTrackingTickCounter;
    /** Client-thread location snapshot consumed by Netty packet callbacks. */
    private volatile boolean inHubCached;
    private boolean wasInHub;
    private boolean wasBurrowDetectionEnabled;

    public Diana(DianaRuntimeContext runtimeContext) {
        if (runtimeContext == null) throw new IllegalArgumentException("Diana runtime context cannot be null");
        this.runtimeContext = runtimeContext;
        this.inquisitorAlertController = new MinosInquisitorAlertController(
                inquisitorLocations, runtimeContext.getScheduler(), inquisitorOutlineTracker);
        this.inquisitorOutlineRenderer = new InquisitorOutlineRenderer(inquisitorOutlineTracker);
    }

    @SubscribeEvent
    public void onParticlePacketReceive(PacketReadEvent event) {
        if (event == null || event.packet == null) return;
        // PacketReadEvent is invoked on Netty. Do not read the mutable SkyblockData state here.
        if (!inHubCached) return;
        if (!(event.packet instanceof S2APacketParticles)) return;

        S2APacketParticles particles = (S2APacketParticles) event.packet;
        if (particles.getParticleType() == null) return;
        String particleName = particles.getParticleType().getParticleName();

        if ("crit".equals(particleName) || "magicCrit".equals(particleName)
                || "dripLava".equals(particleName) || "enchantmenttable".equals(particleName)) {
            if (Config.feature.diana.dianaShowWaypointsBurrows) processor.addParticle(particles);
        } else if ("angryVillager".equals(particleName) && Config.feature.diana.dianaSiamese) {
            final int[] particleCoords = {
                    (int) particles.getXCoordinate(),
                    (int) particles.getYCoordinate(),
                    (int) particles.getZCoordinate()
            };
            runtimeContext.getScheduler().executeOnClientThread(() -> siameseTracker.markClosestHittable(particleCoords));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        boolean inHub = SkyblockData.getCurrentLocation().isHub();
        inHubCached = inHub;
        boolean inquisitorTrackingEnabled = Config.feature.diana.dianaMinosInquisitorAlert
                || Config.feature.diana.dianaMinosInquisitorOutline;
        // Discover targets on the client tick, not as a side effect of a mob render callback.
        inquisitorOutlineTracker.scanIfDue(mc, inHub && inquisitorTrackingEnabled);
        if (inquisitorOutlineTracker.consumeObservedTrackedDeath()) {
            inquisitorAlertController.onInquisitorDeath(processor);
        }

        if (!inHub) {
            if (wasInHub || wasBurrowDetectionEnabled) {
                processor.clearParticles();
                mobTrackingService.clear();
            }
            wasInHub = false;
            wasBurrowDetectionEnabled = false;
            mobTrackingTickCounter = 0;
            return;
        }
        wasInHub = true;

        if (Config.feature.diana.dianaShowWaypointsBurrows) {
            wasBurrowDetectionEnabled = true;
            processor.processParticles();
        } else if (wasBurrowDetectionEnabled) {
            processor.clearParticles();
            wasBurrowDetectionEnabled = false;
        }

        // Mob discovery only needs a periodic scan. Rendering is handled separately from cached targets.
        if (++mobTrackingTickCounter >= 4) {
            mobTrackingTickCounter = 0;
            if (mc != null && mc.theWorld != null && mc.thePlayer != null) {
                mobTrackingService.update(mc.theWorld, mc.thePlayer, processor,
                        Config.feature.diana.dianaGaiaConstruct, Config.feature.diana.dianaSiamese);
            }
        }
    }

    @SubscribeEvent
    public void onRenderLast(RenderWorldLastEvent event) {
        if (!SkyblockData.getCurrentLocation().isHub()) return;

        waypointRenderer.render(event.partialTicks);
        if (Config.feature.diana.dianaGaiaConstruct || Config.feature.diana.dianaSiamese) {
            mobTrackingService.render(event.partialTicks,
                    Config.feature.diana.dianaGaiaConstruct, Config.feature.diana.dianaSiamese);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!SkyblockData.getCurrentLocation().isHub()) return;
        if (!Config.feature.diana.dianaMinosInquisitorAlert) return;
        inquisitorAlertController.initializeLocations();
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || mc.thePlayer == null) return;
        if (!SkyblockData.getCurrentLocation().isHub()) return;
        boolean inquisitorTrackingEnabled = Config.feature.diana.dianaMinosInquisitorAlert
                || Config.feature.diana.dianaMinosInquisitorOutline;
        if (!inquisitorTrackingEnabled) return;

        try {
            // Keep the Beta 42 render-driven discovery path as a fallback to the client-tick scan.
            // scanIfDue is throttled, so this does not rescan the world for every rendered entity.
            inquisitorOutlineTracker.scanIfDue(mc, true);
            if (Config.feature.diana.dianaMinosInquisitorAlert) {
                inquisitorAlertController.onEntityRendered(event.entity, processor);
            }
        } catch (Throwable error) {
            error.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onInquisitorLivingDeath(LivingDeathEvent event) {
        if (event == null || event.entity == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null || event.entity.worldObj != mc.theWorld) return;
        if (!SkyblockData.getCurrentLocation().isHub()) return;
        if (!(event.entity instanceof EntityLivingBase)) return;

        EntityLivingBase dyingEntity = (EntityLivingBase) event.entity;
        if (!inquisitorOutlineTracker.isInquisitorEntity(dyingEntity)) return;

        inquisitorOutlineTracker.forget(dyingEntity);
        inquisitorAlertController.onInquisitorDeath(processor);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderEntityModel(RenderEntityModelEvent event) {
        inquisitorOutlineRenderer.onRenderEntityModel(event);
        if (inquisitorOutlineTracker.consumeObservedTrackedDeath()) {
            inquisitorAlertController.onInquisitorDeath(processor);
        }
    }

    /** Retained public accessor for integrations that inspect current Inquisitor outline targets. */
    public Set<EntityLivingBase> getCurrentEntities() {
        return inquisitorOutlineTracker.getTrackedEntities();
    }

    /** Clears only the current Inquisitor outline cache. */
    public void clearCache() {
        inquisitorOutlineTracker.clear();
    }

    @SubscribeEvent
    public void onSoundPacketReceive(PacketReadEvent event) {
        // This callback is on Netty; use the location snapshot published by onClientTick.
        if (event == null || event.packet == null || !inHubCached) return;
        Packet packet = event.packet;
        if (!(packet instanceof S29PacketSoundEffect)) return;

        S29PacketSoundEffect soundEffect = (S29PacketSoundEffect) packet;
        final int[] coordsSound = {
                (int) Math.floor(soundEffect.getX()),
                (int) Math.floor(soundEffect.getY()),
                (int) Math.floor(soundEffect.getZ())
        };
        final String soundName = soundEffect.getSoundName();

        switch (soundName) {
            case "random.explode":
                if (!Config.feature.diana.dianaDisableDianaExplosionSounds) return;
                if (Math.floor(soundEffect.getPitch() * 1000) / 1000 == 1.190) {
                    if (event.isCancelable()) event.setCanceled(true);
                }
                break;
            case "note.pling":
                if (Config.feature.diana.dianaShowWaypointsBurrows) {
                    runtimeContext.getScheduler().executeOnClientThread(() ->
                            deleteClosestWaypoint(coordsSound[0], coordsSound[1], coordsSound[2]));
                }
                break;
            case "mob.zombie.metal":
            case "mob.irongolem.death":
            case "mob.irongolem.hit":
                if (!Config.feature.diana.dianaGaiaConstruct) return;
                runtimeContext.getScheduler().executeOnClientThread(() -> {
                    GaiaConstruct closestGaia = gaiaTracker.getClosest(coordsSound);
                    if (closestGaia == null) return;
                    if ("mob.zombie.metal".equals(soundName)) {
                        closestGaia.addHit();
                    } else if ("mob.irongolem.hit".equals(soundName)) {
                        closestGaia.hurtAction();
                    } else {
                        runtimeContext.getScheduler().scheduleOnClientThread(
                                () -> gaiaTracker.remove(closestGaia), 1, TimeUnit.SECONDS);
                    }
                });
                break;
            default:
                break;
        }
    }

    private void deleteClosestWaypoint(int x, int y, int z) {
        int[] coords = {x, y, z};
        Waypoint result = processor.getClosestWaypointExcludingType(coords, "MINOS");
        if (result != null && processor.areCoordinatesClose(result.getCoordinates(), coords, 3)) {
            result.setHidden(true);
            runtimeContext.getScheduler().scheduleOnClientThread(() -> processor.deleteWaypoint(result), 30, TimeUnit.SECONDS);
        }
    }

    @SubscribeEvent
    public void onChatRecieve(ClientChatReceivedEvent event) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        if (!SkyblockData.getCurrentLocation().isHub()) return;
        if (com.nef.notenoughfakepixel.utils.ChatUtils.middleBar.matcher(event.message.getFormattedText()).matches()) return;

        if (Config.feature.diana.dianaCancelCooldownSpadeMessage
                && InventoryUtils.getSlot("Ancestral Spade") == InventoryUtils.getCurrentSlot()) {
            cancelMessage(true, event, cooldownPattern, true);
        }
        if (Config.feature.diana.dianaMinosInquisitorAlert) {
            inquisitorAlertController.onPartyChat(event, processor);
        }
    }

    private void cancelMessage(boolean option, ClientChatReceivedEvent event, Pattern pattern, boolean formatted) {
        if (!option) return;
        String message = event.message.getUnformattedText();
        if (formatted) message = event.message.getFormattedText();
        if (pattern.matcher(message).find() || pattern.matcher(message).matches()) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        resetTransientState();
    }

    /** Clears all world-scoped Diana state. Safe to call during module shutdown as well. */
    public void onModuleShutdown() {
        resetTransientState();
    }

    private void resetTransientState() {
        processor.clearWaypoints();
        processor.clearParticles();
        gaiaTracker.clear();
        siameseTracker.clear();
        inquisitorOutlineTracker.clear();
        inquisitorAlertController.reset();
        runtimeContext.getScheduler().clearPendingTasks();
        mobTrackingTickCounter = 0;
        wasInHub = false;
        inHubCached = false;
        wasBurrowDetectionEnabled = false;
    }
}
