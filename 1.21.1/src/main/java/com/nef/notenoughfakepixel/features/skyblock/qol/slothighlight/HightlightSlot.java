package com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight;

import com.nef.notenoughfakepixel.Configuration;
import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public abstract class HightlightSlot implements ClientModInitializer {
    protected static final int GREEN = 0xFF37FF37;
    protected static final int RED = 0xFFFF3737;
    private static final int SLOT_SIZE = 16;
    private static final List<HightlightSlot> HIGHLIGHTERS = new ArrayList<>();

    private final String titlePrefix;
    private final String itemName;
    private final String loreLine;

    protected HightlightSlot(String titlePrefix, String loreLine) {
        this(titlePrefix, "", loreLine);
    }

    protected HightlightSlot(String titlePrefix, String itemName, String loreLine) {
        this.titlePrefix = titlePrefix;
        this.itemName = itemName;
        this.loreLine = loreLine;
    }

    @Override
    public final void onInitializeClient() {
        HIGHLIGHTERS.add(this);
    }

    protected abstract boolean enabled(Configuration config);

    protected boolean onlyInSkyblock() {
        return true;
    }

    protected int color(Configuration config) {
        return GREEN;
    }

    protected boolean highlightOnlyFirst() {
        return false;
    }

    public static void renderChestHighlights(DrawContext context, GenericContainerScreenHandler handler, Text title, int x, int y) {
        String containerTitle = plain(title);
        boolean inSkyblock = SkyblockData.isSkyblock();
        Configuration config = null;
        for (HightlightSlot highlighter : HIGHLIGHTERS) {
            if (!containerTitle.startsWith(highlighter.titlePrefix) || (!inSkyblock && highlighter.onlyInSkyblock())) continue;

            if (config == null) config = ClientConfig.configuration();
            if (highlighter.enabled(config)) highlighter.renderSlots(context, handler, x, y, highlighter.color(config));
        }
    }

    private void renderSlots(DrawContext context, GenericContainerScreenHandler handler, int x, int y, int color) {
        for (Slot slot : handler.slots) {
            if (slot.inventory != handler.getInventory() || !slot.isEnabled() || !matches(slot.getStack())) continue;

            int slotX = x + slot.x;
            int slotY = y + slot.y;
            context.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, color);
            if (highlightOnlyFirst()) return;
        }
    }

    private boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!itemName.isEmpty() && plain(stack.getName()).contains(itemName)) return true;
        if (loreLine.isEmpty()) return false;

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return false;
        for (Text line : lore.lines()) {
            if (plain(line).contains(loreLine)) return true;
        }
        return false;
    }

    private static String plain(Text text) {
        return Formatting.strip(text.getString());
    }
}
