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
import com.nef.notenoughfakepixel.utils.TitleUtils;
import com.nef.notenoughfakepixel.variables.DungeonFloor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterEvents
public class SPlusNotifier implements GatedListener {

    @Override
    public boolean isListenerActive() {
        return DungeonManager.checkEssentials();
    }

    private static boolean remindedSPlus = true;
    private static boolean remindedUnreachable = true;
    
    private static boolean armedThisRun;
    
    private static int unreachableTicks;
    private static final int UNREACHABLE_CONFIRM_TICKS = 60;

    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (!DungeonManager.checkEssentials() ||
                Minecraft.getMinecraft().thePlayer == null ||
                Minecraft.getMinecraft().theWorld == null) {
            
            armedThisRun = false;
            unreachableTicks = 0;
            return;
        }
        if (e.phase == TickEvent.Phase.END) return;

        if (!armedThisRun && ScoreManager.currentSeconds > 0 && ScoreManager.currentSeconds <= 8) {
            armedThisRun = true;
            remindedSPlus = false;
            remindedUnreachable = false;
            unreachableTicks = 0;
        }
        reminderSPlus();
        reminderUnreachable();
    }

    public static void reminderSPlus() {
        if (remindedSPlus) return;
        if (!Config.feature.dungeons.dungeonsSPlusNotifier && !Config.feature.dungeons.dungeonsSPlusMessage) return;

        DungeonFloor floor = SkyblockData.getCurrentFloor();
        if (floor == null || floor == DungeonFloor.NONE || floor.getSecretPercentage() <= 0) return;
        int currentScore = ScoreManager.getSkillScore() + ScoreManager.getExplorationClearScore() + ScoreManager.getSpeedScore() + ScoreManager.getBonusScore();
        int virtualSecretScore = Math.min(40, ScoreManager.getSecretPercentage() * 40 / floor.getSecretPercentage());
        int virtualTotalScore = currentScore + virtualSecretScore;

        if (virtualTotalScore >= 300) {
            if (Config.feature.dungeons.dungeonsSPlusNotifier) {
                SoundUtils.playSound(mc.thePlayer.getPosition(), "note.pling", 2.0F, 2.0F);
                TitleUtils.showTitle(EnumChatFormatting.RED + "300 Score!", 2000);
            }
            if (Config.feature.dungeons.dungeonsSPlusMessage) {
                String customMessage = Config.feature.dungeons.dungeonsSPlusCustom.trim();
                if (!customMessage.isEmpty()) {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc " + customMessage);
                } else {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc 300 Score!");
                }
            }
            remindedSPlus = true;
        }
    }

    public void reminderUnreachable() {
        if (remindedUnreachable) return;
        if (!Config.feature.dungeons.dungeonsSPlusNotifier && !Config.feature.dungeons.dungeonsSPlusMessage) return;

        if (ScoreManager.getRequiredSecretNeeded() != -1) {
            unreachableTicks = 0;
            return;
        }
        if (++unreachableTicks < UNREACHABLE_CONFIRM_TICKS) return;
        if (Config.feature.dungeons.dungeonsSPlusMessage) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/pc [NEF] S+ may not be reached by secrets only, do crypts or restart");
        }
        remindedUnreachable = true;
    }
}
