/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.skyblock.inventory.equipment;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.mixin.accesors.AccessorGuiContainer;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.ConfigHandler;
import com.nef.notenoughfakepixel.utils.CustomConfigFiles;
import com.nef.notenoughfakepixel.variables.Resources;
import net.minecraft.block.BlockStainedGlassPane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nef.notenoughfakepixel.utils.ConfigHandler.GSON;

/** Renders the equipment preview and captures the equipment menu independently of the display toggle. */
public class EquipmentOverlay {
    public static final int EQUIPMENT_SLOT_OFFSET_Y = 8;
    public static final int ARMOR_OVERLAY_OVERHAND_WIDTH = 24;
    public static final int ARMOR_OVERLAY_HEIGHT = 86;
    public static final int ARMOR_OVERLAY_WIDTH = 31;

    private static final int EQUIPMENT_ROWS = 4;
    private static final long COMMAND_COOLDOWN_MS = 500L;

    private static EquipmentData data = new EquipmentData();
    private static Map<Integer, ItemStack> displayItems = new LinkedHashMap<>();
    private long lastCommandTime;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || !(mc.currentScreen instanceof GuiChest)
                || !SkyblockData.getCurrentGamemode().isSkyblock()) return;

        GuiChest chest = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) chest.inventorySlots;
        if (!isEquipmentContainer(container)) return;

        EquipmentData snapshot = EquipmentData.fromContainer(container);
        if (!snapshot.hasSameItems(data)) {
            data = snapshot;
            saveData();
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiInventory) || !isOverlayEnabled()) return;

        GuiInventory inventory = (GuiInventory) event.gui;
        AccessorGuiContainer container = (AccessorGuiContainer) inventory;
        int guiLeft = container.getGuiLeft();
        int overlayLeft = guiLeft - ARMOR_OVERLAY_OVERHAND_WIDTH;
        int overlayTop = container.getGuiTop();

        RenderHelper.enableGUIStandardItemLighting();
        try {
            renderHudBackground(guiLeft, overlayTop);
            renderEquipmentItems(inventory, overlayLeft, overlayTop, event.mouseX, event.mouseY);
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    /** The preview occupies space outside the vanilla inventory panel; own clicks there explicitly. */
    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiInventory) || !isOverlayEnabled()) return;
        int button = Mouse.getEventButton();
        if (button < 0 || !Mouse.getEventButtonState()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return;
        ScaledResolution scaled = new ScaledResolution(mc);
        int mouseX = Mouse.getEventX() * scaled.getScaledWidth() / mc.displayWidth;
        int mouseY = scaled.getScaledHeight() - Mouse.getEventY() * scaled.getScaledHeight() / mc.displayHeight - 1;

        AccessorGuiContainer container = (AccessorGuiContainer) event.gui;
        int left = container.getGuiLeft() - ARMOR_OVERLAY_OVERHAND_WIDTH;
        int top = container.getGuiTop();
        if (!contains(left, top, ARMOR_OVERLAY_WIDTH, ARMOR_OVERLAY_HEIGHT, mouseX, mouseY)) return;

        event.setCanceled(true);
        if (button == 0) sendEquipmentCommand(mc.thePlayer);
    }

    private boolean isOverlayEnabled() {
        return Config.feature.overlays.equipment
                && SkyblockData.getCurrentGamemode().isSkyblock()
                && Minecraft.getMinecraft().thePlayer != null;
    }

    private static boolean isEquipmentContainer(ContainerChest container) {
        if (container == null || container.getLowerChestInventory() == null
                || container.getLowerChestInventory().getDisplayName() == null) return false;
        String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();
        return title != null && title.trim().startsWith("Your Equipment");
    }

    private void renderEquipmentItems(GuiScreen screen, int overlayLeft, int overlayTop, int mouseX, int mouseY) {
        if (data == null || data.items == null || data.items.isEmpty()) return;
        for (int row = 0; row < EQUIPMENT_ROWS; row++) {
            ItemStack stack = displayItems.get(row);
            if (stack == null) continue;
            if (stack.getItem() instanceof ItemBlock
                    && ((ItemBlock) stack.getItem()).block instanceof BlockStainedGlassPane) continue;

            int itemX = overlayLeft + 8;
            int itemY = overlayTop + EQUIPMENT_SLOT_OFFSET_Y + row * 18;
            Minecraft.getMinecraft().getRenderItem().renderItemIntoGUI(stack, itemX, itemY);
            if (contains(itemX, itemY, 16, 16, mouseX, mouseY)) {
                renderToolTip(screen, stack, mouseX, mouseY);
            }
        }
    }

    private static boolean contains(int left, int top, int width, int height, int x, int y) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    protected void renderToolTip(GuiScreen screen, ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        List<String> tooltip = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
        for (int i = 0; i < tooltip.size(); i++) {
            if (i == 0) tooltip.set(i, stack.getRarity().rarityColor + tooltip.get(i));
            else tooltip.set(i, EnumChatFormatting.GRAY + tooltip.get(i));
        }
        FontRenderer font = stack.getItem().getFontRenderer(stack);
        drawHoveringText(tooltip, x, y, font == null ? mc.fontRendererObj : font, screen);
    }

    protected void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font, GuiScreen screen) {
        try {
            GuiUtils.drawHoveringText(textLines, x, y, screen.width, screen.height, -1, font);
        } catch (RuntimeException error) {
            // Tooltip rendering must not abort the inventory screen render.
        }
    }

    public static void saveData() {
        if (data == null) data = new EquipmentData();
        data.normalize();
        rebuildDisplayItems();
        ConfigHandler.saveConfig(data, new File(CustomConfigFiles.EQUIPMENTS.path), GSON);
    }

    public static void loadData() {
        File file = new File(CustomConfigFiles.EQUIPMENTS.path);
        if (!file.isFile()) {
            data = new EquipmentData();
            rebuildDisplayItems();
            return;
        }
        EquipmentData loaded = ConfigHandler.loadConfig(EquipmentData.class, file, GSON);
        data = loaded == null ? new EquipmentData() : loaded;
        data.normalize();
        rebuildDisplayItems();
    }

    private static void rebuildDisplayItems() {
        displayItems = data == null ? new LinkedHashMap<Integer, ItemStack>() : data.getItemStacks();
    }

    private void sendEquipmentCommand(EntityPlayerSP player) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastCommandTime < COMMAND_COOLDOWN_MS) return;
        player.sendChatMessage("/equipment");
        lastCommandTime = now;
    }

    public static void renderHudBackground(int guiLeft, int guiTop) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_TEXTURE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glColor4f(1F, 1F, 1F, 1F);
            ResourceLocation equipmentTexture = Resources.EQUIPMENT.getResource();
            Minecraft.getMinecraft().getTextureManager().bindTexture(equipmentTexture);
            int overlayLeft = guiLeft - ARMOR_OVERLAY_OVERHAND_WIDTH;
            Gui.drawScaledCustomSizeModalRect(overlayLeft, guiTop, 0f, 0f,
                    ARMOR_OVERLAY_WIDTH, ARMOR_OVERLAY_HEIGHT,
                    ARMOR_OVERLAY_WIDTH, ARMOR_OVERLAY_HEIGHT,
                    ARMOR_OVERLAY_WIDTH, ARMOR_OVERLAY_HEIGHT);
        } finally {
            GL11.glColor4f(1F, 1F, 1F, 1F);
            GL11.glPopAttrib();
        }
    }
}
