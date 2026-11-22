/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.commands.RenameCommand;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.utils.RomanNumeralParser;
import com.nef.notenoughfakepixel.variables.StackingEnchant;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.Map;

@RegisterEvents
public class ItemTooltipModifier {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemTooltipFirst(ItemTooltipEvent event) {
        ItemStack item = event.itemStack;
        List<String> tooltip = event.toolTip;
        if (tooltip == null || item == null || !SkyblockData.getCurrentGamemode().isSkyblock()) return;
        if (!ItemUtils.isSkyblockItem(item)) return;

        // Restore client-only custom names by UUID; the override is never written to ExtraAttributes.
        String uuid = ItemUtils.getItemUUID(item);
        Map<String, String> renamed = RenameCommand.renamedItems;
        if (uuid != null && !uuid.isEmpty() && renamed.containsKey(uuid) && !tooltip.isEmpty()) {
            // The ItemTooltipEvent is post-generation: update the title line rather than mutate a live stack.
            tooltip.set(0, renamed.get(uuid));
        }

        int insertAt = Math.max(1, tooltip.size() - 1);
        if (Config.feature.qol.qolShowAdminTag) {
            ItemUtils.AdminStatus status = ItemUtils.isAdminItem(item);
            if (status.isAdmin()) {
                String line = "§c§l⚠ ADMIN ITEM ⚠";
                String adminName = ItemUtils.getAdminName(item);
                if (!adminName.isEmpty()) line += " §7(§c" + adminName + "§7)";
                if (!status.getReason().isEmpty()) {
                    String detail = status.getDetail().isEmpty() ? "" : " " + status.getDetail();
                    line += " §8- (" + status.getReason() + detail + ")";
                }
                tooltip.add(Math.min(insertAt, tooltip.size()), line);
                insertAt++;
            }
        }

        if (Config.feature.qol.qolShowStackingCounter && ItemUtils.hasStackingCounter(item)) {
            StackingEnchant enchant = ItemUtils.getStackingEnchant(item);
            if (enchant != StackingEnchant.NONE) {
                int counter = ItemUtils.getStackingCounter(item);
                Integer next = enchant.getNextLevel(counter);
                String line = "§6§l" + enchant.getDisplayName() + ": §e" + String.format(java.util.Locale.ROOT, "%,d", counter);
                line += next == null ? " §7(MAX LEVEL)" : " §7(Next Level at §a" + String.format(java.util.Locale.ROOT, "%,d", next) + "§7)";
                tooltip.add(Math.min(insertAt, tooltip.size()), line);
            }
        }

        if (Config.feature.romanNumerals.tooltips) {
            for (int i = 0; i < tooltip.size(); i++) {
                String line = tooltip.get(i);
                if (line != null) tooltip.set(i, RomanNumeralParser.replaceInString(line));
            }
        }
    }
}
