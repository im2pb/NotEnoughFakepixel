/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons.rewards;

import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.utils.ItemUtils;
import com.nef.notenoughfakepixel.utils.SkyblockGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.world.WorldEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@RegisterEvents
public final class DungeonRewardAnalyzer {
    private static DungeonRewardAnalyzer instance;
    private final List<DungeonDropData> lastDrops = new ArrayList<>();
    private String lastChest = "";
    private long analysisStartedAt;
    private GuiChest lastGui;
    private int lastCheckedTick = -1;
    private String lastContentSignature = "";

    public DungeonRewardAnalyzer() {
        instance = this;
    }

    public static DungeonRewardAnalyzer getInstance() { return instance; }

    public List<DungeonDropData> getLastDrops() { return Collections.unmodifiableList(new ArrayList<>(lastDrops)); }
    public String getLastChest() { return lastChest; }
    public long getAnalysisStartedAt() { return analysisStartedAt; }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui != lastGui) {
            lastChest = "";
            lastDrops.clear();
            analysisStartedAt = 0L;
            lastContentSignature = "";
            lastCheckedTick = -1;
        }
        lastGui = event.gui instanceof GuiChest ? (GuiChest) event.gui : null;
    }

    @SubscribeEvent
    public void onGuiDraw(GuiScreenEvent.BackgroundDrawnEvent event) {
        if (!SkyblockGate.isFakepixelDungeon() || !(event.gui instanceof GuiChest)) return;
        GuiChest gui = (GuiChest) event.gui;
        if (!(gui.inventorySlots instanceof ContainerChest)) return;
        String title = ((ContainerChest) gui.inventorySlots).getLowerChestInventory().getDisplayName().getUnformattedText();
        if (!isRewardChest(title)) return;
        int currentTick = Minecraft.getMinecraft().thePlayer == null ? -1 : Minecraft.getMinecraft().thePlayer.ticksExisted;
        if (currentTick >= 0 && currentTick == lastCheckedTick) return;
        lastCheckedTick = currentTick;

        String contentSignature = buildContentSignature(gui);
        if (title.equals(lastChest) && !lastDrops.isEmpty() && contentSignature.equals(lastContentSignature)) return;
        lastChest = title;
        lastContentSignature = contentSignature;
        analysisStartedAt = System.currentTimeMillis();
        lastDrops.clear();
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot == null || slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            int quantity = Math.max(1, stack.stackSize);
            String id = ItemUtils.getInternalName(stack);
            String display = StringUtils.stripControlCodes(stack.getDisplayName());
            String rarity = ItemUtils.getRarity(stack).name();
            lastDrops.add(new DungeonDropData(stack.copy(), id, display, rarity, quantity));
        }
    }

    private String buildContentSignature(GuiChest gui) {
        StringBuilder signature = new StringBuilder();
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot == null || slot.inventory == Minecraft.getMinecraft().thePlayer.inventory) continue;
            ItemStack stack = slot.getStack();
            if (stack == null) {
                signature.append(slot.slotNumber).append(':').append(';');
                continue;
            }
            signature.append(slot.slotNumber).append(':')
                    .append(ItemUtils.getInternalName(stack)).append(':')
                    .append(stack.stackSize).append(':')
                    .append(stack.getMetadata()).append(';');
        }
        return signature.toString();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world != null && event.world.isRemote) {
            lastDrops.clear();
            lastChest = "";
            analysisStartedAt = 0L;
            lastGui = null;
            lastCheckedTick = -1;
            lastContentSignature = "";
        }
    }

    private boolean isRewardChest(String title) {
        String clean = StringUtils.stripControlCodes(title == null ? "" : title).toLowerCase(Locale.ROOT);
        return clean.contains("bedrock chest") || clean.contains("obsidian chest") || clean.contains("emerald chest")
                || clean.contains("diamond chest") || clean.contains("gold chest") || clean.contains("wood chest");
    }
}
