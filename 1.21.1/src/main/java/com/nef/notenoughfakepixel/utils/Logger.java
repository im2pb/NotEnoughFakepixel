package com.nef.notenoughfakepixel.utils;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.config.features.Debug;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class Logger {
    private static final org.slf4j.Logger CONSOLE = LoggerFactory.getLogger("notenoughfakepixel.debug");

    private enum Output {
        CHAT, CONSOLE, BOTH
    }

    private Logger() {
    }

    public static void log(Object message) {
        Text text = message instanceof Text styled ? styled : notice(String.valueOf(message), false);
        write(text, Output.CHAT, false, true);
    }

    public static void logConsole(Object message) {
        Text text = message instanceof Text styled ? styled : Text.literal(String.valueOf(message));
        write(text, Output.CONSOLE, false, true);
    }

    public static void logError(String message) {
        write(notice(message, true), Output.CHAT, true, true);
    }

    public static void logErrorConsole(String message) {
        write(Text.literal(message), Output.CONSOLE, true, true);
    }

    public static void trace(String message) {
        trace(notice(message, false));
    }

    public static void trace(Text message) {
        write(message, Output.BOTH, false, true);
    }

    public static void report(List<String> lines) {
        report(notice(String.join("\n", lines), false));
    }

    public static void report(Text message) {
        write(message, Output.BOTH, false, false);
    }

    public static void feedback(String message) {
        feedback(message, false);
    }

    public static void feedbackError(String message) {
        feedback(message, true);
    }

    private static void feedback(String message, boolean error) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> writeChat(client, notice(message, error), error));
    }

    private static Text notice(String message, boolean error) {
        return Text.literal(message).formatted(error ? Formatting.RED : Formatting.GREEN);
    }

    private static void write(Text message, Output output, boolean error, boolean automatic) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            Debug settings = ClientConfig.configuration().debug;
            if (automatic && !settings.debug) return;

            boolean chat = output != Output.CONSOLE && settings.chatLogs;
            boolean console = output != Output.CHAT && settings.consoleLogs;
            if (chat) writeChat(client, message, error);
            if (console) {
                String plain = Formatting.strip(message.getString());
                if (error) {
                    CONSOLE.error("{}", plain);
                } else {
                    CONSOLE.info("{}", plain);
                }
            }
            if (!automatic && !chat && !console) {
                writeChat(client, Text.literal("Enable Chat Output or Console Output under Debug to view this report.")
                        .formatted(Formatting.YELLOW), false);
            }
        });
    }

    private static void writeChat(MinecraftClient client, Text message, boolean error) {
        if (client.player == null) return;

        Text prefix = Text.literal("[").formatted(Formatting.WHITE)
                .append(Text.literal(error ? "NEF" : "NEF DEBUG").formatted(error ? Formatting.RED : Formatting.BLUE))
                .append("] ");
        client.inGameHud.getChatHud().addMessage(Text.empty().append(prefix).append(message));
    }
}
