/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.crimson;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import com.nef.notenoughfakepixel.utils.SoundUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class BossNotifier {
    private final static String countdownSound = "random.orb";
    private final static String titleSoundMageOutlawReady = "mob.wither.spawn";
    private final static String titleTextBossReady = " Ready";
    private final boolean[] bladesoulScheduled = new boolean[6];
    private final boolean[] mageOutlawScheduled = new boolean[6];
    @Getter
    private static final boolean[] ashfangScheduled = new boolean[6];
    private static final boolean[] barbarianDukeXScheduled = new boolean[6];

    private static final Pattern BLADESOUL_DOWN = Pattern.compile("BLADESOUL DOWN!");
    private static final Pattern MAGE_OUTLAW_DOWN = Pattern.compile("MAGE OUTLAW DOWN!");
    private static final Pattern ASHFANG_DOWN = Pattern.compile("ASHFANG DOWN!");
    private static final Pattern BARBARIAN_DUKE_X_DOWN = Pattern.compile("BARBARIAN DUKE X DOWN!");

    private long mageOutlawLastKill = -1;
    private long mageOutlawReady = -1;
    private long ashfangLastKill = -1;
    private long ashfangReady = -1;
    private long barbarianDukeXLastKill = -1;
    private long barbarianDukeXReady = -1;
    private long bladesoulLastKill = -1;
    private long bladesoulReady = -1;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (Crimson.checkEssentials()) return;
        if (Config.feature.crimson.crimsonBladesoulNotifier) {
            playCountdown("Bladesoul", bladesoulReady, bladesoulLastKill, bladesoulScheduled);
        }
        if (Config.feature.crimson.crimsonMageOutlawNotifier) {
            playCountdown("Mage Outlaw", mageOutlawReady, mageOutlawLastKill, mageOutlawScheduled);
        }
        if (Config.feature.crimson.crimsonAshfangNotifier) {
            playCountdown("Ashfang", ashfangReady, ashfangLastKill, ashfangScheduled);
        }
        if (Config.feature.crimson.crimsonBarbarianDukeXNotifier) {
            playCountdown("Barbarian Duke X", barbarianDukeXReady, barbarianDukeXLastKill, barbarianDukeXScheduled);
        }
    }

    @SubscribeEvent
    public void onChat(@NotNull ClientChatReceivedEvent e) {
        if (Crimson.checkEssentials()) return;
        int spawnBossMilliseconds = 125000;

        if (Config.feature.crimson.crimsonBladesoulNotifier) {
            Matcher matcher = BLADESOUL_DOWN.matcher(e.message.getUnformattedText());
            if (matcher.find()) {
                bladesoulLastKill = System.currentTimeMillis();
                bladesoulReady = bladesoulLastKill + spawnBossMilliseconds;
                Arrays.fill(bladesoulScheduled, true);
                return;
            }
        }

        if (Config.feature.crimson.crimsonMageOutlawNotifier) {
            Matcher matcher = MAGE_OUTLAW_DOWN.matcher(e.message.getUnformattedText());
            if (matcher.find()) {
                mageOutlawLastKill = System.currentTimeMillis();
                mageOutlawReady = mageOutlawLastKill + spawnBossMilliseconds;
                Arrays.fill(mageOutlawScheduled, true);
                return;
            }
        }

        if (Config.feature.crimson.crimsonAshfangNotifier) {
            Matcher matcher = ASHFANG_DOWN.matcher(e.message.getUnformattedText());
            if (matcher.find()) {
                ashfangLastKill = System.currentTimeMillis();
                ashfangReady = ashfangLastKill + spawnBossMilliseconds;
                Arrays.fill(ashfangScheduled, true);
                return;
            }
        }

        if (Config.feature.crimson.crimsonBarbarianDukeXNotifier) {
            Matcher matcher = BARBARIAN_DUKE_X_DOWN.matcher(e.message.getUnformattedText());
            if (matcher.find()) {
                barbarianDukeXLastKill = System.currentTimeMillis();
                barbarianDukeXReady = barbarianDukeXLastKill + spawnBossMilliseconds;
                Arrays.fill(barbarianDukeXScheduled, true);
            }
        }
    }

    @SubscribeEvent()
    public void onWorldUnload(WorldEvent.Unload event) {
        Arrays.fill(bladesoulScheduled, false);
        Arrays.fill(mageOutlawScheduled, false);
        Arrays.fill(ashfangScheduled, false);
        Arrays.fill(barbarianDukeXScheduled, false);
        bladesoulReady = -1;
        bladesoulLastKill = -1;
        mageOutlawReady = -1;
        mageOutlawLastKill = -1;
        ashfangReady = -1;
        ashfangLastKill = -1;
        barbarianDukeXReady = -1;
        barbarianDukeXLastKill = -1;
    }

    private void playCountdown(String boss, long timeBossReady, long bossLastKill, boolean[] bossScheduled) {
        if (timeBossReady == -1L || bossLastKill == -1L) return;

        long now = System.currentTimeMillis();
        long remaining = timeBossReady - now;
        // Once the boss is ready, never emit old countdown messages on later ticks.
        if (remaining <= 0L) {
            if (bossScheduled[0]) notifyTitle(boss);
            Arrays.fill(bossScheduled, false);
            return;
        }

        // Check the nearest threshold first. If the client was paused or the player
        // entered the area late, do not play stale 120/60/30-second messages in a burst.
        if (remaining <= 5000L && bossScheduled[5]) {
            notifyChat(boss, 5);
            bossScheduled[5] = false;
            clearOlderCountdowns(bossScheduled, 5);
        } else if (remaining <= 10000L && bossScheduled[4]) {
            notifyChat(boss, 10);
            bossScheduled[4] = false;
            clearOlderCountdowns(bossScheduled, 4);
        } else if (remaining <= 30000L && bossScheduled[3]) {
            notifyChat(boss, 30);
            bossScheduled[3] = false;
            clearOlderCountdowns(bossScheduled, 3);
        } else if (remaining <= 60000L && bossScheduled[2]) {
            notifyChat(boss, 60);
            bossScheduled[2] = false;
            clearOlderCountdowns(bossScheduled, 2);
        } else if (remaining <= 123000L && bossScheduled[1]) {
            notifyChat(boss, 120);
            bossScheduled[1] = false;
        }
    }

    /** Removes countdown milestones already passed when a later milestone is emitted. */
    private void clearOlderCountdowns(boolean[] bossScheduled, int currentIndex) {
        for (int i = 1; i < currentIndex; i++) bossScheduled[i] = false;
    }

    private void notifyChat(String boss, int seconds) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        String joinText = " spawning in ";
        if (seconds == 120) {
            ChatUtils.notifyChat(EnumChatFormatting.YELLOW + boss + joinText + "2 minutes");
        } else if (seconds == 60) {
            ChatUtils.notifyChat(EnumChatFormatting.YELLOW + boss + joinText + "1 minute");
        } else {
            ChatUtils.notifyChat(EnumChatFormatting.YELLOW + boss + joinText + seconds + " seconds");
        }
        SoundUtils.playSound(new int[]{mc.thePlayer.getPosition().getX(),
                mc.thePlayer.getPosition().getY(),
                mc.thePlayer.getPosition().getZ()}, countdownSound, 2.0f, 1.0f
        );
    }

    private void notifyTitle(String boss) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.ingameGUI == null) return;

        mc.ingameGUI.displayTitle(EnumChatFormatting.GOLD + boss + titleTextBossReady, "", 2, 25, 2);
        SoundUtils.playSound(new int[]{mc.thePlayer.getPosition().getX(),
                mc.thePlayer.getPosition().getY(),
                mc.thePlayer.getPosition().getZ()}, titleSoundMageOutlawReady, 2.0f, 1.0f);
        switch (boss) {
            case "Bladesoul":
                bladesoulScheduled[0] = false;
                break;
            case "Mage Outlaw":
                mageOutlawScheduled[0] = false;
                break;
            case "Ashfang":
                ashfangScheduled[0] = false;
                break;
            case "Barbarian Duke X":
                barbarianDukeXScheduled[0] = false;
                break;
        }
    }

}
