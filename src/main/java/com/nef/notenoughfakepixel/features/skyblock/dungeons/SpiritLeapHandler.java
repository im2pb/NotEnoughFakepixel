/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RegisterEvents
public class SpiritLeapHandler {

    private static final Pattern PLAYER_PATTERN = Pattern.compile("(?:\\[.+?] )?(\\w+)");

    public static class PlayerData {
        public final String playerName;
        public final String colorCode;
        public final int slotId;
        public String className;
        public int boxX, boxY, boxWidth, boxHeight;

        public PlayerData(String playerName, String colorCode, int slotId, String className) {
            this.playerName = playerName;
            this.colorCode = colorCode;
            this.slotId = slotId;
            this.className = className;
        }
    }

    private static final List<PlayerData> playerDataList = new ArrayList<>();

    private static String cleanSB(String scoreboard) {
        return StringUtils.stripControlCodes(scoreboard).chars()
                .filter(c -> c > 20 && c < 127)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static List<String> getSidebarLines() {
        List<String> lines = new ArrayList<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return lines;

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return lines;

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return lines;

        Collection<Score> scores = scoreboard.getSortedScores(objective);
        List<Score> list = scores.stream()
                .filter(input -> input != null && input.getPlayerName() != null && !input.getPlayerName().startsWith("#"))
                .collect(Collectors.toList());

        if (list.size() > 15) {
            scores = Lists.newArrayList(Iterables.skip(list, scores.size() - 15));
        } else {
            scores = list;
        }

        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            lines.add(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()));
        }

        return lines;
    }

    @RegisterEvents
    public static class ChestGuiOverlayHandler {

        private static final Minecraft mc = Minecraft.getMinecraft();

        private static final String UNKNOWN = "Unknown";

        private static final Pattern LORE_CLASS = Pattern.compile("(?i)class\\W+(archer|berserk|healer|mage|tank)");

        private long lastLearnTime;
        private int learnTicks;

        private static String normalizeClass(String name) {
            String lower = name.toLowerCase(Locale.ROOT);
            switch (lower) {
                case "berserk": return "Berserk";
                case "archer": return "Archer";
                case "tank": return "Tank";
                case "healer": return "Healer";
                case "mage": return "Mage";
                default: return UNKNOWN;
            }
        }

        private static int classColor(String className) {
            switch (className) {
                case "Berserk": return 0x55FF55; 
                case "Mage": return 0x40D8FF;    
                case "Tank": return 0xFF5555;    
                case "Healer": return 0xFF55E6;  
                case "Archer": return 0xFFAA00;  
                default: return -1;
            }
        }

        private static void learnClasses() {
            DungeonParty.refresh();
        }

        private static String classFromLore(ItemStack stack) {
            if (stack == null || !stack.hasTagCompound()) return null;
            NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");
            if (!display.hasKey("Lore", 9)) return null;
            NBTTagList lore = display.getTagList("Lore", 8);
            for (int i = 0; i < lore.tagCount(); i++) {
                Matcher matcher = LORE_CLASS.matcher(StringUtils.stripControlCodes(lore.getStringTagAt(i)));
                if (matcher.find()) return normalizeClass(matcher.group(1));
            }
            return null;
        }

        private static String resolveClass(String playerName, ItemStack stack) {
            String known = DungeonParty.classOf(playerName);
            if (known != null) return known;
            String fromLore = classFromLore(stack);
            return fromLore != null ? fromLore : UNKNOWN;
        }

        @SubscribeEvent
        public void onClientTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) return;
            if (++learnTicks < 40) return;
            learnTicks = 0;
            if (!Config.feature.dungeons.dungeonsSpiritLeapGUI || !SkyblockData.getCurrentLocation().isDungeon()) return;
            if (mc.thePlayer == null || mc.theWorld == null) return;
            learnClasses();
        }

        @SubscribeEvent
        public void onWorldUnload(WorldEvent.Unload event) {
            DungeonParty.clear();
            playerDataList.clear();
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
            if (event.gui instanceof GuiChest) {
                GuiChest chestGui = (GuiChest) event.gui;
                Container containerChest = chestGui.inventorySlots;
                String displayName = ((ContainerChest) containerChest)
                        .getLowerChestInventory()
                        .getDisplayName()
                        .getUnformattedText()
                        .trim();

                if (Config.feature.dungeons.dungeonsSpiritLeapGUI && SkyblockData.getCurrentLocation().isDungeon() && "Spirit Leap".equals(displayName)) {
                    playerDataList.clear();
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
            if (event.gui instanceof GuiChest) {
                GuiChest chestGui = (GuiChest) event.gui;
                Container containerChest = chestGui.inventorySlots;
                String displayName = ((ContainerChest) containerChest)
                        .getLowerChestInventory()
                        .getDisplayName()
                        .getUnformattedText()
                        .trim();

                if (Config.feature.dungeons.dungeonsSpiritLeapGUI && SkyblockData.getCurrentLocation().isDungeon() && "Spirit Leap".equals(displayName)) {
                    event.setCanceled(true);

                    if (playerDataList.isEmpty()) {
                        processSlots(chestGui);
                    }

                    drawCustomOverlay(event, chestGui);
                }
            }
        }

        private void processSlots(GuiChest chestGui) {
            for (Slot slot : chestGui.inventorySlots.inventorySlots) {
                if (slot.inventory == mc.thePlayer.inventory) continue;

                ItemStack stack = slot.getStack();
                if (stack != null && stack.getItem() == Items.skull && stack.hasDisplayName()) {
                    String rawName = StringUtils.stripControlCodes(stack.getDisplayName());
                    Matcher matcher = PLAYER_PATTERN.matcher(rawName);
                    if (!matcher.find()) continue;

                    String name = matcher.group(1);
                    if (name.equals("Unknown")) continue;

                    learnClasses();
                    String dungeonClass = resolveClass(name, stack);

                    Matcher colorMatcher = Pattern.compile("(§[0-9a-fk-or])").matcher(stack.getDisplayName());
                    String colorCode = colorMatcher.find() ? colorMatcher.group(1) : "§f";
                    playerDataList.add(new PlayerData(name, colorCode, slot.slotNumber, dungeonClass));
                }
            }
        }

        private void refreshUnknownClasses(GuiChest chestGui) {
            boolean anyUnknown = false;
            for (PlayerData pd : playerDataList) {
                if (UNKNOWN.equals(pd.className)) {
                    anyUnknown = true;
                    break;
                }
            }
            if (!anyUnknown) return;

            long now = System.currentTimeMillis();
            if (now - lastLearnTime < 250L) return;
            lastLearnTime = now;

            learnClasses();
            for (PlayerData pd : playerDataList) {
                if (!UNKNOWN.equals(pd.className)) continue;
                ItemStack stack = pd.slotId >= 0 && pd.slotId < chestGui.inventorySlots.inventorySlots.size()
                        ? chestGui.inventorySlots.getSlot(pd.slotId).getStack() : null;
                pd.className = resolveClass(pd.playerName, stack);
            }
        }

        private void drawCustomOverlay(GuiScreenEvent.DrawScreenEvent.Pre event, GuiChest chestGui) {
            int screenWidth = event.gui.width;
            int screenHeight = event.gui.height;

            final int boxWidth = 256;
            final int boxHeight = 72;
            final int padding = 32;

            int numBoxes = playerDataList.size();
            if (numBoxes == 0) return;

            refreshUnknownClasses(chestGui);

            int columns = 2;
            int rows = (int) Math.ceil((double) numBoxes / columns);

            int totalWidth = columns * boxWidth + (columns - 1) * padding;
            int totalHeight = rows * boxHeight + (rows - 1) * padding;

            int startX = (screenWidth - totalWidth) / 2;
            int startY = (screenHeight - totalHeight) / 2;

            for (int i = 0; i < numBoxes; i++) {
                PlayerData pd = playerDataList.get(i);
                int column = i % columns;
                int row = i / columns;

                int x = startX + column * (boxWidth + padding);
                int y = startY + row * (boxHeight + padding);

                pd.boxX = x;
                pd.boxY = y;
                pd.boxWidth = boxWidth;
                pd.boxHeight = boxHeight;

                int classRgb = classColor(pd.className);
                Gui.drawRect(x, y, x + boxWidth, y + boxHeight, 0x80000000);
                int borderColor = 0xFF202020;
                int borderWidth = 1;
                if (classRgb != -1) {
                    Gui.drawRect(x, y, x + boxWidth, y + boxHeight, 0x50000000 | classRgb); 
                    borderColor = 0xFF000000 | classRgb;
                    borderWidth = 2;
                }
                Gui.drawRect(x, y, x + boxWidth, y + borderWidth, borderColor);
                Gui.drawRect(x, y + boxHeight - borderWidth, x + boxWidth, y + boxHeight, borderColor);
                Gui.drawRect(x, y, x + borderWidth, y + boxHeight, borderColor);
                Gui.drawRect(x + boxWidth - borderWidth, y, x + boxWidth, y + boxHeight, borderColor);

                String name = pd.playerName;
                String className = pd.className;

                int nameWidth = mc.fontRendererObj.getStringWidth(name);
                int classWidth = mc.fontRendererObj.getStringWidth(className);

                int nameX = x + (boxWidth - nameWidth * 2) / 2;
                int classX = x + (boxWidth - classWidth) / 2;

                int nameY = y + 12;
                int classY = y + 40;

                int color = getColorFromString(pd.colorCode);

                GL11.glPushMatrix();
                GL11.glScalef(2.0F, 2.0F, 1.0F);
                mc.fontRendererObj.drawStringWithShadow(name, nameX / 2, nameY / 2, color);
                GL11.glPopMatrix();

                mc.fontRendererObj.drawStringWithShadow(className, classX, classY, classRgb != -1 ? (0xFF000000 | classRgb) : 0xFFAAAAAA);
            }
        }

        public static int getColorFromString(String colorCode) {
            switch (colorCode) {
                case "§0":
                    return 0xFF000000;
                case "§1":
                    return 0xFF0000AA;
                case "§2":
                    return 0xFF00AA00;
                case "§3":
                    return 0xFF00AAAA;
                case "§4":
                    return 0xFFAA0000;
                case "§5":
                    return 0xFFAA00AA;
                case "§6":
                    return 0xFFFFAA00;
                case "§7":
                    return 0xFFAAAAAA;
                case "§8":
                    return 0xFF555555;
                case "§9":
                    return 0xFF5555FF;
                case "§a":
                    return 0xFF55FF55;
                case "§b":
                    return 0xFF55FFFF;
                case "§c":
                    return 0xFFFF5555;
                case "§d":
                    return 0xFFFF55FF;
                case "§e":
                    return 0xFFFFFF55;
                default:
                    return 0xFFFFFFFF;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
            if (mc.currentScreen instanceof GuiChest) {
                GuiChest chestGui = (GuiChest) mc.currentScreen;
                Container containerChest = chestGui.inventorySlots;
                String displayName = ((ContainerChest) containerChest)
                        .getLowerChestInventory()
                        .getDisplayName()
                        .getUnformattedText()
                        .trim();

                if (Config.feature.dungeons.dungeonsSpiritLeapGUI && SkyblockData.getCurrentLocation().isDungeon() && "Spirit Leap".equals(displayName)) {
                    int button = Mouse.getEventButton();
                    boolean pressed = Mouse.getEventButtonState();

                    if (button == 0 && pressed) {
                        ScaledResolution sr = new ScaledResolution(mc);
                        int mouseX = Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth;
                        int mouseY = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight - 1;

                        boolean clickedBox = false;
                        for (PlayerData pd : playerDataList) {
                            if (mouseX >= pd.boxX && mouseX <= pd.boxX + pd.boxWidth &&
                                    mouseY >= pd.boxY && mouseY <= pd.boxY + pd.boxHeight) {
                                mc.playerController.windowClick(
                                        chestGui.inventorySlots.windowId,
                                        pd.slotId,
                                        0,
                                        0,
                                        mc.thePlayer);
                                if (Config.feature.dungeons.dungeonsLeapAnnounce) {
                                    String command = String.format("/pc Leaped to %s!", pd.playerName);
                                    mc.thePlayer.sendChatMessage(command);
                                }
                                mc.displayGuiScreen(null);
                                clickedBox = true;
                                break;
                            }
                        }

                        if (clickedBox) {
                            event.setCanceled(true);
                        }
                    }
                }
            }
        }
    }
}
