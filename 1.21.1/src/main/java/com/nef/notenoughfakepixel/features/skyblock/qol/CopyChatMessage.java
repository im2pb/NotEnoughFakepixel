package com.nef.notenoughfakepixel.features.skyblock.qol;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class CopyChatMessage {
    private CopyChatMessage() {
    }

    public static Text withCopyButton(Text message, String buttonLabel) {
        String plain = Formatting.strip(message.getString());
        if (plain.isBlank()) return message;

        var button = "[COPY]".equals(buttonLabel)
                ? Text.literal("[COPY]").formatted(Formatting.AQUA, Formatting.BOLD)
                : Text.literal("✍").formatted(Formatting.DARK_GRAY);
        button.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, plain))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Copy message").formatted(Formatting.AQUA))));

        return Text.empty().append(message).append(" ").append(button);
    }
}
