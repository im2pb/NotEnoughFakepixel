/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.utils;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.variables.Constants;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.util.logging.Level;

public class Logger {

    private static final java.util.logging.Logger JUL =
            java.util.logging.Logger.getLogger(Logger.class.getName());

    private static long lastLogTime = 0;

    private Logger() {}

    private static final java.util.logging.Logger LOGGER =
            java.util.logging.Logger.getLogger(Logger.class.getName());

    @Setter
    private static volatile Boolean debugOverride = null;

    public static void log(String message) {
        if (!isDebugEnabled()) return;
        if (!isClientChatReady()) return;
        try {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(Constants.PREFIX + message)
            );
        } catch (Throwable ignored) {
            
            logConsole(message);
        }
    }

    public static void logOnlyOnce(String message) {
        if (!isClientChatReady()) return;
        long now = System.currentTimeMillis();
        if (now - lastLogTime > 1000L) {
            try {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(Constants.PREFIX + message));
            } catch (Throwable ignored) {
                
            }
            lastLogTime = now;
        }
    }

    public static void logError(String message) {
        if (!isDebugEnabled()) return;
        if (!isClientChatReady()) return;
        try {
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                    new ChatComponentText(Constants.ERROR_PREFIX + message)
            );
        } catch (Throwable ignored) {
            logErrorConsole(message);
        }
    }

    public static void log(Object object) {
        if (object == null) return;
        log(object.toString());
    }

    public static void logConsole(String message) {
        if (!isDebugEnabled()) return;
        safeJulLog(Level.INFO, message);
    }

    public static void logErrorConsole(String error) {
        if (!isDebugEnabled()) return;
        safeJulLog(Level.WARNING, error);
    }

    public static void logConsole(Object object) {
        if (!isDebugEnabled()) return;
        try {
            logConsole(String.valueOf(object));
        } catch (Throwable t) {
            safeJulLog(Level.WARNING, "Failed to log object: " + (object == null ? "null" : object.getClass().getName()));
        }
    }

    private static void safeJulLog(Level level, String msg) {
        String text = String.valueOf(msg);
        try {
            JUL.log(level, text);
        } catch (Throwable __) {
            if (level.intValue() >= Level.WARNING.intValue()) {
                System.err.println("[NEF][ERR] " + text);
            } else {
                System.out.println("[NEF] " + text);
            }
        }
    }

    private static boolean isDebugEnabled() {
        if (debugOverride != null) return debugOverride;
        try {
            
            return Config.feature.debug.debug;
        } catch (Throwable __) {
            return false;
        }
    }

    private static boolean isClientChatReady() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            return mc != null && mc.thePlayer != null && mc.getNetHandler() != null;
        } catch (Throwable __) {
            return false;
        }
    }

    public static void logErrorPlayers(String message) {
        if (!isClientChatReady()) return;
        try {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(Constants.ERROR_PREFIX + message));
        } catch (Throwable ignored) {}
    }

}
