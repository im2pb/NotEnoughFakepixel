package com.nef.notenoughfakepixel.alerts;

import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.apache.commons.lang3.text.WordUtils;

public enum Variables {

    PLAYER("player","The Display Name of the player playing the mod") {
        @Override
        public String getValue() {
            Minecraft mc = Minecraft.getMinecraft();
            return mc.thePlayer != null ? mc.thePlayer.getDisplayNameString() : "Unknown Player";
        }
    },
    LOCATION("location","The current Island Location of the player") {
        @Override
        public String getValue() {
            if (SkyblockData.getCurrentLocation() == null) return "Unknown Location";
            return WordUtils.capitalizeFully(SkyblockData.getCurrentLocation().name()).replace("_", " ");
        }
    },
    PING("ping", "Your current ping to the server") {
        @Override
        public String getValue() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.getNetHandler() == null) return "Unknown Ping";
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            return info != null ? info.getResponseTime() + "ms" : "Unknown Ping";
        }
    },
    TIME("time", "System time (HH:mm a)") {
        @Override
        public String getValue() {
            return new java.text.SimpleDateFormat("hh:mm a").format(new java.util.Date());
        }
    },
    AREA("area", "The current area on island of the player") {
        @Override
        public String getValue() {
            if (SkyblockData.getCurrentArea() == null || SkyblockData.getCurrentArea().getArea() == null) {
                return "Unknown Area";
            }
            return SkyblockData.getCurrentArea().getArea();
        }
    };

    public final String variableText, variableUsage;

    Variables(String variableText,String usage) {
        this.variableText = "${" + variableText + "}";
        this.variableUsage = usage;
    }
    public abstract String getValue();

    public static String replaceVariables(String text) {
        if (text == null) return "";

        String result = text;
        for (Variables var : values()) {
            String value;
            try {
                value = var.getValue();
            } catch (Throwable ignored) {
                value = "Unknown";
            }
            result = result.replace(var.variableText, value == null ? "Unknown" : value);
        }
        return result;
    }
}
