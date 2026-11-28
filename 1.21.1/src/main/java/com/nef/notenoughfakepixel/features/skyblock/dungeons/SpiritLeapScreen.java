package com.nef.notenoughfakepixel.features.skyblock.dungeons;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.serverdata.SkyblockSidebar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SpiritLeapScreen extends GenericContainerScreen {
    private static final Pattern MENU_NAME = Pattern.compile(
            "^(?:(?:Click to )?leap to )?(?:\\[[^]]+]\\s*)*([A-Za-z0-9_]{1,16})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUNGEON_ROLE = Pattern.compile(
            "\\[([BATHM])]\\s*(?:\\[[^]]+]\\s*)*([A-Za-z0-9_]{1,16})(?![A-Za-z0-9_])");
    private static final Map<String, String> ROLES = Map.of(
            "B", "Berserk", "A", "Archer", "T", "Tank", "H", "Healer", "M", "Mage");
    private List<Card> cards = List.of();

    private SpiritLeapScreen(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    public static Screen replace(MinecraftClient client, Screen screen) {
        if (screen == null || screen.getClass() != GenericContainerScreen.class || client.player == null || !enabled()
                || !Formatting.strip(screen.getTitle().getString()).strip().equals("Spirit Leap")) return screen;

        var chest = (GenericContainerScreen) screen;
        return new SpiritLeapScreen(chest.getScreenHandler(), client.player.getInventory(), screen.getTitle());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        cards = enabled() ? layout() : List.of();
        if (cards.isEmpty()) {
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, getTitle(), width / 2, Math.max(4, cards.getFirst().y() - 20), 0xFFFFFFFF);
        for (Card card : cards) {
            int x = card.x();
            int y = card.y();
            boolean hovered = card.contains(mouseX, mouseY);
            context.fill(x, y, x + card.width(), y + card.height(), hovered ? 0xF0353E53 : 0xE5212633);
            context.drawBorder(x, y, card.width(), card.height(), hovered ? 0xFF88B9FF : 0xFF525E77);
            context.drawItem(card.stack(), x + 8, y + (card.height() - 16) / 2);
            int labelWidth = card.width() - 34;
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(card.name(), labelWidth),
                    x + 30, y + card.height() / 2 - 10, card.color());
            context.drawTextWithShadow(textRenderer, card.role(), x + 30, y + card.height() / 2 + 1, 0xFFBBBBBB);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (cards.isEmpty()) return super.mouseClicked(mouseX, mouseY, button);
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !enabled() || client.player == null
                || client.player.currentScreenHandler != handler) return true;

        for (Card card : cards) {
            if (!card.contains(mouseX, mouseY)) continue;
            var slot = handler.getSlot(card.slot());
            if (!card.name().equals(playerName(slot.getStack()))) return true;

            onMouseClick(slot, slot.id, 0, SlotActionType.PICKUP);
            if (ClientConfig.configuration().dungeons.general.dungeonsLeapAnnounce) {
                client.player.networkHandler.sendChatCommand("pc Leaped to " + card.name() + "!");
            }
            if (client.currentScreen == this) close();
            break;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return !cards.isEmpty() || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return !cards.isEmpty() || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (cards.isEmpty()) return super.keyPressed(key, scanCode, modifiers);
        if (key == GLFW.GLFW_KEY_ESCAPE || client.options.inventoryKey.matchesKey(key, scanCode)) close();
        return true;
    }

    private List<Card> layout() {
        if (client.world == null || client.player == null) return List.of();
        Map<String, String> roles = new HashMap<>();
        SkyblockSidebar.displayedLines(client).forEach(line -> {
            var match = DUNGEON_ROLE.matcher(Formatting.strip(line.getString()));
            if (match.find()) roles.put(match.group(2).toLowerCase(Locale.ROOT), ROLES.get(match.group(1)));
        });

        List<Card> entries = new ArrayList<>();
        for (var slot : handler.slots) {
            if (slot.inventory == client.player.getInventory()) continue;
            ItemStack stack = slot.getStack();
            String name = playerName(stack);
            if (name == null) continue;
            int color = stack.getName().visit((style, part) -> style.getColor() == null || part.isBlank()
                    ? Optional.empty() : Optional.of(0xFF000000 | style.getColor().getRgb()), Style.EMPTY).orElse(0xFFFFFFFF);
            entries.add(new Card(name, roles.getOrDefault(name.toLowerCase(Locale.ROOT), "Unknown"),
                    slot.id, stack, color, 0, 0, 0, 0));
        }
        if (entries.isEmpty()) return List.of();

        int gap = 8;
        int columns = entries.size() > 1 && width >= 240 ? 2 : 1;
        int rows = (entries.size() + columns - 1) / columns;
        int cardWidth = Math.min(172, (width - 16 - (columns - 1) * gap) / columns);
        int cardHeight = Math.clamp((height - 36 - (rows - 1) * gap) / rows, 26, 44);
        int startY = (height - rows * cardHeight - (rows - 1) * gap) / 2 + 8;
        for (int i = 0; i < entries.size(); i++) {
            Card entry = entries.get(i);
            int rowColumns = Math.min(columns, entries.size() - i / columns * columns);
            int startX = (width - rowColumns * cardWidth - (rowColumns - 1) * gap) / 2;
            entries.set(i, new Card(entry.name(), entry.role(), entry.slot(), entry.stack(), entry.color(),
                    startX + i % columns * (cardWidth + gap), startY + i / columns * (cardHeight + gap), cardWidth, cardHeight));
        }
        return entries;
    }

    static String playerName(ItemStack stack) {
        if (!stack.isOf(Items.PLAYER_HEAD)) return null;
        var match = MENU_NAME.matcher(Formatting.strip(stack.getName().getString()).strip());
        return match.matches() && !match.group(1).equalsIgnoreCase("Unknown") ? match.group(1) : null;
    }

    private static boolean enabled() {
        return DungeonContext.active() && ClientConfig.configuration().dungeons.general.dungeonsSpiritLeapGUI;
    }

    private record Card(String name, String role, int slot, ItemStack stack, int color, int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }
}
