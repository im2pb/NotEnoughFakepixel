/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.score;

import com.nef.notenoughfakepixel.envcheck.registers.GatedListener;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.features.skyblock.dungeons.DungeonManager;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.SoundUtils;
import com.nef.notenoughfakepixel.utils.TablistParser;
import com.nef.notenoughfakepixel.utils.TitleUtils;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class ScoreManager implements GatedListener {

    @Override
    public boolean isListenerActive() {
        return DungeonManager.checkEssentials();
    }

    static int failedPuzzles = 0;
    public static int currentSeconds = -1;
    private static int reducedPenalty = 0;
    private static boolean hasNotified270 = false;

    private static boolean isBloodOpen = false;

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!DungeonManager.checkEssentials()) return;
        if (event.message.getUnformattedText().contains("PUZZLE FAIL!")) {
            failedPuzzles++;
        } else if (event.message.getUnformattedText().equals("Your Spirit Pet reduced your death score penalty to 1!")) {
            reducedPenalty = 1;
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc [NEF] I died with Spirit Pet, so my kill doesn't count");
        } else if (event.message.getUnformattedText().contains("[NEF] I died with Spirit Pet, so my kill doesn't count")) {
            reducedPenalty = 1;
        } else if (event.message.getUnformattedText().endsWith("The BLOOD DOOR has been opened!")) {
            isBloodOpen = true;
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!DungeonManager.checkEssentials()) return;
        reset();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!DungeonManager.checkEssentials() ||
                event.phase == TickEvent.Phase.END ||
                Minecraft.getMinecraft().thePlayer == null) return;

        int currentScore = getSkillScore() + getExplorationClearScore() + getSpeedScore() + getBonusScore();
        DungeonFloor floor = SkyblockData.getCurrentFloor();
        int secretNeeded = floor == null ? -1 : floor.getSecretPercentage();
        int virtualSecretScore = secretNeeded > 0 ? Math.min(40, getSecretPercentage() * 40 / secretNeeded) : 0;
        int virtualTotalScore = currentScore + virtualSecretScore;

        if (virtualTotalScore >= 270 && !hasNotified270 && Config.feature.dungeons.dungeonsSNotifier) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc 270 Score!");
            SoundUtils.playSound(mc.thePlayer.getPosition(), "note.pling", 2.0F, 2.0F);
            TitleUtils.showTitle(EnumChatFormatting.RED + "270 Score!", 2000);
            hasNotified270 = true;
        }
    }

    private void reset() {
        failedPuzzles = 0;
        currentSeconds = -1;
        reducedPenalty = 0;
        hasNotified270 = false;
        isBloodOpen = false;
    }

    public static int getTotalScore() {
        return getSkillScore() + getExplorationScore() + getSpeedScore() + getBonusScore();
    }

    public static int getSkillScore() {
        return DungeonScoreCalculator.skillScore(TablistParser.deaths, reducedPenalty, failedPuzzles);
    }

    public static int getExplorationClearScore() {
        return DungeonScoreCalculator.explorationClearScore(SkyblockData.getClearedPercentage(), isBloodOpen);
    }

    public static int getExplorationSecretScore() {
        DungeonFloor floor = SkyblockData.getCurrentFloor();
        return DungeonScoreCalculator.explorationSecretScore(getSecretPercentage(), floor);
    }

    public static int getExplorationScore() {
        return getExplorationClearScore() + getExplorationSecretScore();
    }

    public static int getSpeedScore() {
        currentSeconds = convertToSeconds(TablistParser.time);
        return DungeonScoreCalculator.speedScore(currentSeconds, SkyblockData.getCurrentFloor());
    }

    public static int getBonusScore() {
        return DungeonScoreCalculator.bonusScore(TablistParser.crypts, Config.feature.dungeons.dungeonsIsPaul);
    }

    public static int getSecretPercentage() {
        return TablistParser.secretPercentage;
    }

    public static int getRequiredSecretNeeded() {
        return DungeonScoreCalculator.requiredSecretPercentage(
                getSkillScore(), getSpeedScore(), getBonusScore(), SkyblockData.getCurrentFloor());
    }

    public static int convertToSeconds(String time) {
        if (time == null || time.trim().isEmpty()) return -1;
        String[] parts = time.trim().split(":");
        if (parts.length != 2) return -1;
        try {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            if (minutes < 0 || seconds < 0 || seconds > 59) return -1;
            return minutes * 60 + seconds;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
