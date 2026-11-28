package com.nef.notenoughfakepixel.features.skyblock.enchanting;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RegisterEvents
public class EnchantingSolvers {
    private static ContainerChest chest;
    private static Solver solver;
    private static ItemStack[] serverSlots;
    private static final List<Integer> colors = new ArrayList<>();
    private static Set<Integer> highlightedColors = new HashSet<>();
    private static final List<Integer> numberedSlots = new ArrayList<>();
    private static final BitSet coveredNumbers = new BitSet();
    private static final Map<Integer, ItemStack> cards = new HashMap<>();
    private static final Map<Integer, BitSet> rejectedPartners = new HashMap<>();
    private static final BitSet cardSlots = new BitSet();
    private static final BitSet visibleCards = new BitSet();
    private static final BitSet matchedCards = new BitSet();
    private static final List<Integer> openCards = new ArrayList<>();
    private static int next;
    private static int highestNumber;
    private static int expectedNumbers;
    private static int instantReveal = -1;
    private static boolean guessing;
    private static boolean complete;
    private static boolean ambiguous;
    private static boolean colorReady;
    private static boolean colorLit;
    private static boolean finished;
    private static boolean instantFind;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        clear();
    }

    private static void clear() {
        chest = null;
        solver = null;
        serverSlots = null;
        resetPattern();
        highlightedColors.clear();
        expectedNumbers = 0;
        cards.clear();
        rejectedPartners.clear();
        cardSlots.clear();
        matchedCards.clear();
        visibleCards.clear();
        openCards.clear();
        instantReveal = -1;
        guessing = finished = instantFind = false;
    }

    public static void contentsReceived(int windowId, ItemStack[] contents) {
        if (!accepts(windowId) || contents.length < serverSlots.length) return;
        for (int slot = 0; slot < serverSlots.length; slot++) serverSlots[slot] = copy(contents[slot]);
        observeBoard();
    }

    public static void slotReceived(int windowId, int slot, ItemStack stack) {
        if (slot < 0 || !accepts(windowId) || slot >= serverSlots.length) return;
        serverSlots[slot] = copy(stack);
        observeBoard();
    }

    private static boolean accepts(int windowId) {
        if (windowId <= 0 || !Config.feature.experimentation.experimentationSolvers
                || !SkyblockData.getCurrentGamemode().isSkyblock()
                || !(Minecraft.getMinecraft().currentScreen instanceof GuiChest)) return false;
        ContainerChest current = (ContainerChest) ((GuiChest) Minecraft.getMinecraft().currentScreen).inventorySlots;
        IInventory inventory = current.getLowerChestInventory();
        int size = inventory.getSizeInventory();
        if (current.windowId != windowId || size < 27) return false;

        if (current != chest) {
            clear();
            chest = current;
            String title = inventory.getDisplayName().getUnformattedText();
            if (title.startsWith("Chronomatron (")) solver = Solver.CHRONOMATRON;
            else if (title.startsWith("Ultrasequencer (")) solver = Solver.ULTRASEQUENCER;
            else if (title.startsWith("Superpairs (")) solver = Solver.SUPERPAIRS;
            if (solver != null) serverSlots = new ItemStack[size];
        }
        return solver != null;
    }

    // Capture the server's board independently of local cursor and slot predictions.
    private static void observeBoard() {
        int size = serverSlots.length;
        if (solver == Solver.SUPERPAIRS) {
            if (Config.feature.experimentation.experimentationSuperpairsSolver) observeCards();
            return;
        }

        boolean clock = is(serverSlots[size - 5], Items.clock);
        Set<Integer> previousColors = highlightedColors;
        if (solver == Solver.CHRONOMATRON) {
            highlightedColors = new HashSet<>();
            for (int i = 0; i < size; i++) {
                ItemStack stack = serverSlots[i];
                if (is(stack, Item.getItemFromBlock(Blocks.stained_hardened_clay))) {
                    highlightedColors.add(stack.getItemDamage());
                }
            }
        }
        if (!clock && guessing) {
            expectedNumbers = complete ? numberedSlots.size() + 1 : 0;
            resetPattern();
            previousColors = new HashSet<>();
        }
        if (!clock || !guessing) {
            if (solver == Solver.CHRONOMATRON) {
                Set<Integer> pulses = new HashSet<>(highlightedColors);
                pulses.removeAll(previousColors);
                if (pulses.size() > 1) ambiguous = true;
                if (pulses.size() == 1) colors.add(pulses.iterator().next());
            } else {
                rememberNumbers();
            }
        }
        if (clock && !guessing) {
            ItemStack round = serverSlots[4];
            complete = solver == Solver.CHRONOMATRON
                    ? !ambiguous && round != null && colors.size() == round.stackSize
                    : !numberedSlots.isEmpty() && numberedSlots.size() == (expectedNumbers > 0 ? expectedNumbers : highestNumber);
            if (complete && solver == Solver.CHRONOMATRON) colorReady = !highlightedColors.contains(colors.get(0));
        }
        guessing = clock;
        if (!guessing || !complete) return;

        if (solver == Solver.CHRONOMATRON && next < colors.size()) {
            boolean lit = highlightedColors.contains(colors.get(next));
            if (!colorReady) colorReady = !lit;
            else if (lit) colorLit = true;
            else if (colorLit) {
                next++;
                colorLit = false;
                colorReady = next < colors.size() && !highlightedColors.contains(colors.get(next));
            }
        } else if (solver == Solver.ULTRASEQUENCER) {
            for (int numberedSlot : numberedSlots) {
                if (!is(serverSlots[numberedSlot], Items.dye)) coveredNumbers.set(numberedSlot);
            }
            while (next < numberedSlots.size()) {
                int numberedSlot = numberedSlots.get(next);
                if (!coveredNumbers.get(numberedSlot) || !is(serverSlots[numberedSlot], Items.dye)) break;
                next++;
            }
        }
    }

    private static void resetPattern() {
        colors.clear();
        numberedSlots.clear();
        coveredNumbers.clear();
        next = highestNumber = 0;
        complete = ambiguous = colorReady = colorLit = false;
    }

    private static void rememberNumbers() {
        Map<Integer, Integer> byNumber = new HashMap<>();
        int count = 0;
        for (int slot = 0; slot < serverSlots.length; slot++) {
            ItemStack stack = serverSlots[slot];
            if (!is(stack, Items.dye)) continue;
            count++;
            highestNumber = Math.max(highestNumber, stack.stackSize);
            byNumber.put(stack.stackSize, slot);
        }
        highestNumber = Math.max(highestNumber, count);
        if (count == 0 || byNumber.size() != count || count < numberedSlots.size()
                || (expectedNumbers > 0 && count != expectedNumbers)) return;
        for (int i = 1; i <= count; i++) {
            if (!byNumber.containsKey(i)) return;
        }
        numberedSlots.clear();
        for (int i = 1; i <= count; i++) numberedSlots.add(byNumber.get(i));
    }

    static boolean blocksClick(Slot slot) {
        if (!guessing || !complete || !active(slot.inventory)) return false;

        ItemStack stack = slot.getStack();
        if (solver == Solver.CHRONOMATRON && Config.feature.experimentation.experimentationChronomatronSolver
                && nextColorKnown() && is(stack, Item.getItemFromBlock(Blocks.stained_glass))
                && stack.getItemDamage() != colors.get(next)) {
            return true;
        }
        return solver == Solver.ULTRASEQUENCER && Config.feature.experimentation.experimentationUltraSequencerSolver
                && next < numberedSlots.size() && coveredNumbers.get(numberedSlots.get(next))
                && is(stack, Item.getItemFromBlock(Blocks.stained_glass_pane))
                && slot.slotNumber != numberedSlots.get(next)
                && slot.slotNumber >= 9 && slot.slotNumber < slot.inventory.getSizeInventory() - 9;
    }

    public static void drawSlotHint(Slot slot) {
        if (!active(slot.inventory)) return;
        int color = 0;
        int number = -1;
        ItemStack stack = slot.getStack();
        if (solver == Solver.SUPERPAIRS && Config.feature.experimentation.experimentationSuperpairsSolver && !finished) {
            ItemStack reward = cards.get(slot.getSlotIndex());
            if (reward != null) {
                int dye = 0;
                if (powerup(reward)) dye = Config.feature.experimentation.supPower;
                else if (matchedCards.get(slot.getSlotIndex())) dye = Config.feature.experimentation.supMatched;
                else if (hasPartner(slot.getSlotIndex(), reward)) dye = Config.feature.experimentation.supPossible;
                else if (cover(stack)) dye = Config.feature.experimentation.supUnmatched;
                color = dyeColor(dye);
            }
        } else if (guessing && complete && solver == Solver.CHRONOMATRON
                && Config.feature.experimentation.experimentationChronomatronSolver && nextColorKnown()
                && is(stack, Item.getItemFromBlock(Blocks.stained_glass)) && stack.getItemDamage() == colors.get(next)) {
            color = 0xFF46FF69;
        } else if (guessing && complete && solver == Solver.ULTRASEQUENCER
                && Config.feature.experimentation.experimentationUltraSequencerSolver && !is(stack, Items.dye)) {
            int order = numberedSlots.indexOf(slot.slotNumber);
            if (order >= next && coveredNumbers.get(slot.slotNumber)) {
                number = order + 1;
                if (order == next) color = 0xFF46FF69;
                else if (order == next + 1) color = dyeColor(Config.feature.experimentation.seqUpcoming);
            }
        }
        if (color == 0 && number < 0) return;

        int x = slot.xDisplayPosition;
        int y = slot.yDisplayPosition;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GlStateManager.disableDepth();
        if (color != 0) {
            Gui.drawRect(x, y, x + 16, y + 16, 0x40000000 | (color & 0xFFFFFF));
            Gui.drawRect(x, y, x + 16, y + 1, color);
            Gui.drawRect(x, y + 15, x + 16, y + 16, color);
            Gui.drawRect(x, y + 1, x + 1, y + 15, color);
            Gui.drawRect(x + 15, y + 1, x + 16, y + 15, color);
        }
        if (number > 0) {
            FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
            String label = Integer.toString(number);
            Gui.drawRect(x + 2, y + 3, x + 14, y + 13, 0xC0000000);
            font.drawStringWithShadow(label, x + 8 - font.getStringWidth(label) / 2.0F, y + 4, 0xFFFFFF);
        }
        if (depth) GlStateManager.enableDepth();
    }

    private static boolean nextColorKnown() {
        return next < colors.size() && colorReady && !colorLit;
    }

    private static int dyeColor(int id) {
        return id > 0 && id <= 16 ? 0xFF000000 | EnumDyeColor.byMetadata(id - 1).getMapColor().colorValue : 0;
    }

    public static ItemStack displayStack(Slot slot, ItemStack stack) {
        if (solver != Solver.SUPERPAIRS || finished || !active(slot.inventory)
                || !Config.feature.experimentation.experimentationSuperpairsSolver || !cover(stack)) return stack;
        return cards.getOrDefault(slot.getSlotIndex(), stack);
    }

    private static void observeCards() {
        if (finished) return;

        if (openCards.stream().anyMatch(slot -> cover(serverSlots[slot]))) {
            if (openCards.size() == 2) {
                int a = openCards.get(0);
                int b = openCards.get(1);
                if (sameReward(cards.get(a), cards.get(b))) {
                    rejectedPartners.computeIfAbsent(a, ignored -> new BitSet()).set(b);
                    rejectedPartners.computeIfAbsent(b, ignored -> new BitSet()).set(a);
                }
            }
            openCards.clear();
        }
        for (int slot = 0; slot < serverSlots.length; slot++) {
            ItemStack stack = serverSlots[slot];
            if (cover(stack)) {
                cardSlots.set(slot);
                visibleCards.clear(slot);
            } else if (stack != null && cardSlots.get(slot)) {
                if (loreContains(stack, "Click any to proceed!")) {
                    finished = true;
                    return;
                }
                if (visibleCards.get(slot)) continue;

                if (openCards.size() == 2) {
                    confirmPair(openCards.get(0), openCards.get(1));
                    openCards.clear();
                }
                ItemStack copy = stack.copy();
                cards.put(slot, copy);
                visibleCards.set(slot);
                if (powerup(copy)) {
                    if (loreContains(copy, "Powerup for next click!")) instantFind = true;
                } else if (instantFind && instantReveal < 0) {
                    instantReveal = slot;
                } else {
                    if (instantFind) {
                        instantFind = false;
                        int earlier = instantReveal;
                        instantReveal = -1;
                        if (confirmPair(earlier, slot)) continue;
                        if (!openCards.isEmpty() && confirmPair(openCards.get(0), earlier)) openCards.clear();
                    }
                    openCards.add(slot);
                }
            }
        }
    }

    private static boolean hasPartner(int slot, ItemStack reward) {
        for (Map.Entry<Integer, ItemStack> entry : cards.entrySet()) {
            if (entry.getKey() != slot && !matchedCards.get(entry.getKey())
                    && !rejected(slot, entry.getKey()) && sameReward(reward, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean confirmPair(int a, int b) {
        if (!visibleCards.get(a) || !visibleCards.get(b) || rejected(a, b)
                || !sameReward(cards.get(a), cards.get(b))) return false;
        matchedCards.set(a);
        matchedCards.set(b);
        return true;
    }

    private static boolean rejected(int a, int b) {
        BitSet partners = rejectedPartners.get(a);
        return partners != null && partners.get(b);
    }

    private static boolean sameReward(ItemStack a, ItemStack b) {
        return a != null && b != null && !powerup(a) && !powerup(b) && a.getItem() == b.getItem()
                && a.getItemDamage() == b.getItemDamage() && a.stackSize == b.stackSize
                && a.getDisplayName().equals(b.getDisplayName()) && lore(a).equals(lore(b));
    }

    private static boolean active(IInventory inventory) {
        return solver != null && chest != null && inventory == chest.getLowerChestInventory()
                && Minecraft.getMinecraft().currentScreen instanceof GuiChest
                && ((GuiChest) Minecraft.getMinecraft().currentScreen).inventorySlots == chest
                && Config.feature.experimentation.experimentationSolvers && SkyblockData.getCurrentGamemode().isSkyblock();
    }

    private static boolean cover(ItemStack stack) {
        if (!is(stack, Item.getItemFromBlock(Blocks.stained_glass)) || stack.getItemDamage() != 8) return false;
        String name = EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName()).trim();
        return name.isEmpty() || name.equals("?");
    }

    private static boolean powerup(ItemStack stack) {
        return loreContains(stack, "Instant Powerup!") || loreContains(stack, "Powerup for next click!");
    }

    private static boolean loreContains(ItemStack stack, String text) {
        NBTTagList lore = lore(stack);
        for (int index = 0; index < lore.tagCount(); index++) {
            if (text.equalsIgnoreCase(EnumChatFormatting.getTextWithoutFormattingCodes(lore.getStringTagAt(index)).trim())) return true;
        }
        return false;
    }

    private static NBTTagList lore(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getCompoundTag("display").getTagList("Lore", 8) : new NBTTagList();
    }

    private static boolean is(ItemStack stack, Item item) {
        return stack != null && stack.getItem() == item;
    }

    private static ItemStack copy(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private enum Solver { CHRONOMATRON, ULTRASEQUENCER, SUPERPAIRS }
}
