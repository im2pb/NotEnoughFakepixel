/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.protection;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterEvents;
import com.nef.notenoughfakepixel.envcheck.registers.RegisterKeybind;
import com.nef.notenoughfakepixel.events.SlotClickEvent;
import com.nef.notenoughfakepixel.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.Locale;
import java.util.UUID;

@RegisterEvents
public class ItemProtection {
    private static final ProtectedItemStore STORE = new ProtectedItemStore();
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static long lastBlockMessage;

    @RegisterKeybind
    public static final KeyBinding PROTECT_KEY = new KeyBinding("Protect Item", Keyboard.KEY_NONE, "NotEnoughFakepixel");

    public static ProtectedItemStore getStore() {
        return STORE;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        STORE.load();
        if (MC.thePlayer == null) return;
        if (PROTECT_KEY.isPressed()) toggleHoveredItem();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSlotClick(SlotClickEvent event) {
        if (!Config.feature.protection.enabled || MC.thePlayer == null || event == null) return;

        ItemStack clicked = event.slot == null ? null : event.slot.getStack();
        ItemStack cursor = MC.thePlayer.inventory.getItemStack();
        boolean protectedClicked = isProtected(clicked);
        boolean protectedCursor = isProtected(cursor);

        if (event.slotId == -999 && protectedCursor && (event.clickType == 0 || event.clickType == 1 || event.clickType == 4)) {
            block(event, "drop protected item");
            return;
        }

        if (event.clickType == 4 && (protectedClicked || protectedCursor)) {
            block(event, "drop protected item");
            return;
        }

        if (!isDangerousContainer(event.guiContainer)) return;

        if (event.clickType == 2) {
            ItemStack hotbar = getHotbarStack(event.clickedButton);
            if (protectedClicked || isProtected(hotbar) || protectedCursor) {
                block(event, "unsafe hotbar swap");
            }
            return;
        }

        if (event.clickType == 5 && protectedCursor) {
            block(event, "drag protected item");
            return;
        }

        if (event.clickType == 6 && containsProtectedItem(event.guiContainer)) {
            block(event, "unsafe inventory collect");
            return;
        }

        if (protectedClicked || protectedCursor) {
            block(event, "unsafe inventory action");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onItemToss(ItemTossEvent event) {
        if (!Config.feature.protection.enabled || event == null || event.entityItem == null) return;
        ItemStack dropped = event.entityItem.getEntityItem();
        if (!isProtected(dropped)) return;
        event.setCanceled(true);
        notifyBlocked("drop protected item");
    }

    public static boolean isProtected(ItemStack stack) {
        UUID uuid = ProtectionUtils.getItemUuid(stack);
        return STORE.contains(uuid);
    }

    public static boolean toggle(ItemStack stack) {
        UUID uuid = ProtectionUtils.getItemUuid(stack);
        if (uuid == null) return false;
        boolean now = STORE.toggle(new ProtectedItemStore.ItemId(uuid));
        if (Config.feature.protection.showChatNotifications) {
            ChatUtils.notifyChat(now ? "§a[NEF] Item protected." : "§e[NEF] Item unprotected.");
        }
        return true;
    }

    private void toggleHoveredItem() {
        if (!(MC.currentScreen instanceof GuiContainer)) return;
        Slot slot;
        try {
            slot = ((GuiContainer) MC.currentScreen).getSlotUnderMouse();
        } catch (Throwable ignored) {
            return;
        }
        if (slot == null || slot.getStack() == null) return;
        if (!toggle(slot.getStack()) && Config.feature.protection.showChatNotifications) {
            ChatUtils.notifyChat("§c[NEF] This item has no SkyBlock UUID and cannot be protected.");
        }
    }

    private static ItemStack getHotbarStack(int index) {
        if (MC.thePlayer == null || index < 0 || index > 8) return null;
        return MC.thePlayer.inventory.getStackInSlot(index);
    }

    private static boolean isDangerousContainer(GuiContainer gui) {
        if (gui == null || gui.inventorySlots == null) return false;
        String title = "";
        if (gui.inventorySlots instanceof ContainerChest) {
            try {
                title = ProtectionUtils.cleanTitle(((ContainerChest) gui.inventorySlots)
                        .getLowerChestInventory().getDisplayName().getUnformattedText());
            } catch (Throwable ignored) {
            }
        }

        if (title.contains("trading") || title.equals("trade") || title.contains("trade with")
                || title.equals("salvage") || title.contains("salvage") || title.contains("sell item")
                || title.contains("sell offer") || title.contains("create sell offer")
                || title.contains("create auction") || title.contains("create bin")
                || title.contains("buyback")) {
            return true;
        }

        return hasDestructiveActionLore(gui);
    }

    private static boolean hasDestructiveActionLore(GuiContainer gui) {
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot == null) continue;
            ItemStack stack = slot.getStack();
            if (stack == null) continue;
            String name = ProtectionUtils.displayName(stack);
            if (name.equals("sell item") || name.contains("sell offer") || name.contains("buyback")
                    || name.equals("salvage") || name.contains("confirm trade")
                    || name.contains("confirm purchase") || name.contains("confirm sell")) {
                return true;
            }
            if (stack.hasTagCompound()) {
                for (String line : com.nef.notenoughfakepixel.utils.ItemUtils.getLoreLines(stack)) {
                    String lore = net.minecraft.util.StringUtils.stripControlCodes(line == null ? "" : line)
                            .toLowerCase(Locale.ROOT);
                    if (lore.contains("click to buyback") || lore.contains("click to salvage")
                            || lore.contains("create sell offer") || lore.contains("click to sell")
                            || lore.contains("click to confirm trade")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsProtectedItem(GuiContainer gui) {
        if (gui == null || gui.inventorySlots == null) return false;
        for (Slot slot : gui.inventorySlots.inventorySlots) {
            if (slot != null && isProtected(slot.getStack())) return true;
        }
        return false;
    }

    private static void block(SlotClickEvent event, String reason) {
        event.setCanceled(true);
        notifyBlocked(reason);
    }

    private static void notifyBlocked(String reason) {
        if (!Config.feature.protection.showChatNotifications) return;
        long now = System.currentTimeMillis();
        if (now - lastBlockMessage < 500L) return;
        lastBlockMessage = now;
        ChatUtils.notifyChat("§c[NEF] Blocked: " + reason + ".");
    }

    public static void renderIndicator(GuiContainer gui, Slot slot, int guiLeft, int guiTop) {
        if (!Config.feature.protection.enabled || !Config.feature.protection.showProtectedStar
                || slot == null || slot.getStack() == null || !isProtected(slot.getStack())) return;
        int x = slot.xDisplayPosition + 10;
        int y = slot.yDisplayPosition - 1;
        int alpha = Math.max(0, Math.min(255, Math.round(Config.feature.protection.starOpacity * 255f)));

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        gui.mc.fontRendererObj.drawStringWithShadow("★", x, y, (alpha << 24) | 0xFFFFFF);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
    }
}
