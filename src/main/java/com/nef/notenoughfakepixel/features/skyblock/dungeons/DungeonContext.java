/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.score.ScoreManager;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.records.DungeonPersonalBestService;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.Logger;
import com.nef.notenoughfakepixel.utils.SkyblockGate;
import com.nef.notenoughfakepixel.utils.TablistParser;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RegisterEvents
public final class DungeonContext {
    public enum Mode {
        NONE, NORMAL, MASTER
    }

    public enum RunState {
        NONE, STARTING, RUNNING, BLOOD, BOSS, ENDING, FINISHED
    }

    public enum Phase {
        NONE, RUN, BLOOD, BOSS, MAXOR, STORM, TERMINAL, GOLDOR, NECRON, END
    }

    private static boolean inDungeon;
    private static DungeonFloor floor = DungeonFloor.NONE;
    private static Mode mode = Mode.NONE;
    private static RunState runState = RunState.NONE;
    private static Phase phase = Phase.NONE;

    private static long runStartTimestamp = -1L;
    private static long bloodTimestamp = -1L;
    private static long bossTimestamp = -1L;
    private static long bossDeadTimestamp = -1L;
    private static long clearedTimestamp = -1L;
    private static long runId;
    
    private static final DungeonRunMetadata runMetadata = new DungeonRunMetadata();

    private static final Map<String, Long> phaseStarts = new LinkedHashMap<>();
    private static final Map<String, Long> completedPhaseTimes = new LinkedHashMap<>();
    private static DungeonEndStats lastStats;
    private static DungeonRunSnapshot lastRunSnapshot;

    private DungeonContext() {}

    public static boolean isInDungeon() { return inDungeon; }
    public static DungeonFloor getFloor() { return floor; }
    public static Mode getMode() { return mode; }
    public static RunState getRunState() { return runState; }
    public static Phase getPhase() { return phase; }
    public static long getRunStart() { return runStartTimestamp; }
    public static long getRunStartTimestamp() { return runStartTimestamp; }
    public static long getBloodTimestamp() { return bloodTimestamp; }
    public static long getBossTimestamp() { return bossTimestamp; }
    public static long getBossDeadTimestamp() { return bossDeadTimestamp; }
    public static long getClearedTimestamp() { return clearedTimestamp; }
    public static long getBloodTime() { return DungeonRunTimingService.elapsedAt(runStartTimestamp, bloodTimestamp); }
    public static long getBossTime() { return DungeonRunTimingService.elapsedAt(runStartTimestamp, bossTimestamp); }
    public static long getBossDeadTime() { return DungeonRunTimingService.elapsedAt(runStartTimestamp, bossDeadTimestamp); }
    public static long getClearedTime() { return DungeonRunTimingService.elapsedAt(runStartTimestamp, clearedTimestamp); }
    public static long getDungeonXp() { return runMetadata.getDungeonXp(); }
    public static long getRunId() { return runId; }
    public static DungeonEndStats getLastStats() { return lastStats; }
    public static DungeonRunSnapshot getLastRunSnapshot() { return lastRunSnapshot; }

    /** Returns an immutable copy of the currently tracked run state. */
    public static DungeonRunSnapshot snapshot() {
        return createSnapshot();
    }

    private static DungeonRunSnapshot createSnapshot() {
        return new DungeonRunSnapshot(
                inDungeon, floor, mode, runState, phase,
                runStartTimestamp, bloodTimestamp, bossTimestamp, bossDeadTimestamp,
                clearedTimestamp, runMetadata.getDungeonXp(), runId, runMetadata.isStartAnchored(),
                runMetadata.getTabClockAtBegin(), runMetadata.getServerClearMs(), phaseStarts, completedPhaseTimes
        );
    }

    public static boolean isMasterMode() {
        return mode == Mode.MASTER;
    }

    public static boolean isF7orM7() {
        return floor != null && floor.isF7orM7();
    }

    public static long elapsed() {
        return DungeonRunTimingService.elapsedSince(runStartTimestamp, System.currentTimeMillis());
    }

    public static long phaseElapsed(String key) {
        return DungeonRunTimingService.phaseElapsed(phaseStarts, key, System.currentTimeMillis());
    }

    public static Map<String, Long> phaseSnapshot() {
        return DungeonRunTimingService.completedPhaseSnapshot(completedPhaseTimes);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!Config.feature.dungeonSystems.context) {
            clearActiveState();
            return;
        }

        boolean detected = SkyblockGate.isFakepixelDungeon();
        if (detected) {
            if (DungeonRunLifecycleService.shouldBeginDetectedRun(inDungeon)) {
                begin(SkyblockData.getCurrentFloor());
            } else if (DungeonRunLifecycleService.shouldStartNextRun(inDungeon, runState, SkyblockData.getClearedPercentage())) {
                begin(SkyblockData.getCurrentFloor());
            } else {
                updateFloor(SkyblockData.getCurrentFloor());
                anchorStartToServerClock();
            }
            return;
        }

        if (inDungeon) {
            if (DungeonRunLifecycleService.shouldFinishWhenLeaving(phase, runState)) finishRun(false);
            else clearActiveState();
        }
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        if (!Config.feature.dungeonSystems.context || event.message == null || !SkyblockGate.isFakepixelDungeon()) return;
        if (!inDungeon) begin(SkyblockData.getCurrentFloor());
        if (runState == RunState.FINISHED) return;

        String chatLine = event.message.getUnformattedText();
        if (chatLine == null || chatLine.isEmpty()) return;

        DungeonChatSignalParser.Result signals = DungeonChatSignalParser.parse(chatLine);
        runMetadata.apply(signals.getServerClearTimeMs(), signals.getDungeonXp());

        switch (signals.getPhaseSignal()) {
            case EXTRA_STATS:
                finishRun(true);
                return;
            case BLOOD_OPEN:
                markBlood();
                transitionTo(Phase.BLOOD);
                return;
            case BOSS_START:
                markBoss();
                transitionTo(Phase.BOSS);
                return;
            case MAXOR:
                transitionTo(Phase.MAXOR);
                return;
            case STORM:
                transitionTo(Phase.STORM);
                return;
            case TERMINAL:
                transitionTo(Phase.TERMINAL);
                return;
            case GOLDOR:
                transitionTo(Phase.GOLDOR);
                return;
            case NECRON:
                transitionTo(Phase.NECRON);
                return;
            case NONE:
            default:
                return;
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!inDungeon || runState == RunState.FINISHED || event.entity == null || bossDeadTimestamp >= 0L) return;
        if (!(event.entity instanceof EntityLivingBase)) return;

        String rawName = event.entity.getName();
        String clean = StringUtils.stripControlCodes(rawName == null ? "" : rawName)
                .trim()
                .toLowerCase(Locale.ROOT);

        DungeonBossDetectionService.Detection boss = DungeonBossDetectionService.detect(clean, floor);
        if (!boss.isTrackedBoss() || phase == Phase.NONE || phase == Phase.RUN || phase == Phase.BLOOD) return;

        long now = System.currentTimeMillis();
        completeActivePhase(now);
        if (boss.isFinalBoss()) {
            if (bossDeadTimestamp < 0L) bossDeadTimestamp = now;
            phase = Phase.END;
            runState = RunState.ENDING;
            phaseStarts.put(Phase.END.name(), now);
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        if (DungeonRunLifecycleService.shouldFinishOnDisconnect(inDungeon, phase, runState)) finishRun(false);
        else clearActiveState();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world != null && event.world.isRemote) clearActiveState();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == null || !event.world.isRemote) return;
        if (DungeonRunLifecycleService.shouldFinishOnWorldUnload(inDungeon, phase, runState)) finishRun(false);
        else clearActiveState();
    }

    private static void begin(DungeonFloor detectedFloor) {
        inDungeon = true;
        floor = detectedFloor == null ? DungeonFloor.NONE : detectedFloor;
        mode = floor == DungeonFloor.NONE ? Mode.NONE : (floor.isMasterMode() ? Mode.MASTER : Mode.NORMAL);
        runState = RunState.STARTING;
        phase = Phase.RUN;
        runStartTimestamp = System.currentTimeMillis();
        bloodTimestamp = -1L;
        bossTimestamp = -1L;
        bossDeadTimestamp = -1L;
        clearedTimestamp = -1L;
        runId = DungeonRunLifecycleService.nextRunId();
        phaseStarts.clear();
        completedPhaseTimes.clear();
        phaseStarts.put(Phase.RUN.name(), runStartTimestamp);
        runState = RunState.RUNNING;
        runMetadata.begin(TablistParser.time);
        Logger.logConsole("[NEF PB] run #" + runId + " tracking started floor=" + floor + " tabClock='" + runMetadata.getTabClockAtBegin() + "'");
    }

    private static void anchorStartToServerClock() {
        if (runMetadata.isStartAnchored() || runState == RunState.FINISHED || runStartTimestamp <= 0L) return;
        String clock = TablistParser.time;
        if (clock == null || clock.isEmpty() || clock.equals(runMetadata.getTabClockAtBegin())) return;
        long anchored = DungeonRunTimingService.anchoredStartFromClock(clock, System.currentTimeMillis());
        if (anchored < 0L) return;
        runMetadata.markStartAnchored();
        if (Math.abs(anchored - runStartTimestamp) > 3000L) {
            Logger.logConsole("[NEF PB] run start corrected by " + (runStartTimestamp - anchored) + " ms using tab clock '" + clock + "'");
            runStartTimestamp = anchored;
            phaseStarts.put(Phase.RUN.name(), anchored);
        }
    }

    private static void updateFloor(DungeonFloor detectedFloor) {
        if (detectedFloor == null || detectedFloor == DungeonFloor.NONE) return;
        if (floor == detectedFloor) return;
        if (runState == RunState.FINISHED) return;
        floor = detectedFloor;
        mode = floor.isMasterMode() ? Mode.MASTER : Mode.NORMAL;
    }

    private static void markBlood() {
        if (bloodTimestamp < 0L) bloodTimestamp = System.currentTimeMillis();
    }

    private static void markBoss() {
        if (bossTimestamp < 0L) bossTimestamp = System.currentTimeMillis();
    }

    private static boolean transitionTo(Phase next) {
        if (next == null || next == Phase.NONE || next == phase) return false;
        if (!inDungeon || runStartTimestamp <= 0L || runState == RunState.FINISHED) return false;
        if (!DungeonPhaseTransitions.isForwardTransition(phase, next)) return false;

        long now = System.currentTimeMillis();
        completeActivePhase(now);
        phase = next;
        phaseStarts.put(next.name(), now);
        runState = DungeonPhaseTransitions.stateForPhase(next);
        return true;
    }

    private static void completeActivePhase(long end) {
        if (phase == null || phase == Phase.NONE) return;
        String key = phase.name();
        DungeonRunTimingService.completePhase(key, phaseStarts, completedPhaseTimes, end);
    }


    public static void finishRun(boolean printStats) {
        if (!inDungeon || runStartTimestamp <= 0L || runState == RunState.FINISHED) return;

        long end = clearedTimestamp > 0L ? clearedTimestamp : System.currentTimeMillis();
        clearedTimestamp = end;
        runState = RunState.ENDING;
        completeActivePhase(end);
        DungeonFloor currentFloor = floor == null ? DungeonFloor.NONE : floor;

        if (runMetadata.getServerClearMs() > 0L) {
            long correctedStart = end - runMetadata.getServerClearMs();
            Logger.logConsole("[NEF PB] using the server clear time " + runMetadata.getServerClearMs() + " ms instead of local "
                    + (end - runStartTimestamp) + " ms");
            runStartTimestamp = correctedStart;
            phaseStarts.put(Phase.RUN.name(), correctedStart);
        }
        long total = Math.max(0L, end - runStartTimestamp);
        int score = 0;
        try {
            score = ScoreManager.getTotalScore();
        } catch (Throwable ignored) {
        }

        boolean totalIsTrustworthy = DungeonPersonalBestService.isTrustworthyTotal(
                total, runMetadata.getServerClearMs() > 0L, bossTimestamp >= 0L);
        Logger.logConsole("[NEF PB] run #" + runId + " finished floor=" + currentFloor + " total=" + total
                + "ms anchored=" + runMetadata.isStartAnchored() + " bossSeen=" + (bossTimestamp >= 0L) + " countsForPb=" + totalIsTrustworthy);
        DungeonPersonalBestService.Result personalBest = DungeonPersonalBestService.recordTotal(
                currentFloor, total, totalIsTrustworthy);
        java.util.Set<String> newRecordKeys = personalBest.getNewRecordKeys();
        boolean newTotalRecord = personalBest.isNewTotalRecord();

        if (newTotalRecord && printStats && Minecraft.getMinecraft().thePlayer != null) {
            String prefix = currentFloor.name().toLowerCase(Locale.ROOT);
            com.nef.notenoughfakepixel.utils.ChatUtils.notifyChat(
                    "§a[NEF] New Dungeon PB: §f" + prefix + " §a" + formatDuration(total)
            );
        }

        phase = Phase.END;
        runState = RunState.FINISHED;
        lastRunSnapshot = createSnapshot();
        lastStats = DungeonRunResultFactory.create(
                lastRunSnapshot,
                total,
                score,
                personalBest.getStatus(),
                newRecordKeys
        );

        if (printStats && Minecraft.getMinecraft().thePlayer != null) {
            DungeonStatsPrinter.print(lastStats);
        }
        DungeonPersonalBestService.save();
    }

    public static void clearActiveState() {
        inDungeon = false;
        floor = DungeonFloor.NONE;
        mode = Mode.NONE;
        phase = Phase.NONE;
        runState = RunState.NONE;
        runStartTimestamp = -1L;
        bloodTimestamp = -1L;
        bossTimestamp = -1L;
        bossDeadTimestamp = -1L;
        clearedTimestamp = -1L;
        runId = 0L;
        runMetadata.reset();
        phaseStarts.clear();
        completedPhaseTimes.clear();
    }

    public static void reset() {
        lastStats = null;
        lastRunSnapshot = null;
        clearActiveState();
    }

    private static String formatDuration(long ms) {
        long seconds = ms / 1000L;
        return String.format(Locale.ROOT, "%d:%02d.%03d", seconds / 60L, seconds % 60L, ms % 1000L);
    }
}
