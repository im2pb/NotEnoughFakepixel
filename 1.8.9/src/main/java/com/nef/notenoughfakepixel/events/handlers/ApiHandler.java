package com.nef.notenoughfakepixel.events.handlers;

import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.Logger;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ApiHandler {

    private static final String OPTIFINE_URL = "https://optifine.net/downloadx?f=OptiFine_1.8.9_HD_U_M5.jar&x=2368da25408fcffee40a4e9740e9d3eb";

    private static boolean hasShownOptifineNotice = false;

    private ApiHandler() {
        // Utility class
    }

    public static void init() {
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;

        if (!hasShownOptifineNotice) {
            notifyMissingOptifine();
        }
    }

    private static void notifyMissingOptifine() {
        if (hasOptifine()) {
            hasShownOptifineNotice = true;
            Logger.log("Has Optifine");
            return;
        }
        Logger.log("Does not have Optifine");

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        ChatComponentText message = new ChatComponentText(
                EnumChatFormatting.RED + "You are not using Optifine! Consider adding it for better performance §aclicking here"
        );
        message.setChatStyle(new ChatStyle()
                .setColor(EnumChatFormatting.RED)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, OPTIFINE_URL))
                .setChatHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(EnumChatFormatting.YELLOW + "Open Optifine download")
                )));

        mc.thePlayer.addChatMessage(message);
        hasShownOptifineNotice = true;
    }

    private static boolean hasOptifine() {
        for (ModContainer mod : Loader.instance().getModList()) {
            if (containsOptifine(mod.getModId()) || containsOptifine(mod.getName())) {
                return true;
            }
        }

        for (String modFile : collectInstalledModNames()) {
            if (containsOptifine(modFile)) {
                return true;
            }
        }

        return false;
    }

    private static boolean containsOptifine(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("optifine");
    }

    private static List<String> collectInstalledModNames() {
        List<String> installedMods = collectModsFromDirectory();

        if (installedMods.isEmpty()) {
            installedMods.addAll(collectLoadedForgeModIds());
        }

        return installedMods;
    }

    private static List<String> collectModsFromDirectory() {
        List<String> modFileNames = new ArrayList<>();
        File modsDirectory = new File(Minecraft.getMinecraft().mcDataDir, "mods");

        File[] modFiles = modsDirectory.listFiles(file ->
                file.isFile() && isModFileName(file.getName()));
        if (modFiles == null) {
            return modFileNames;
        }

        for (File modFile : modFiles) {
            String fileName = modFile.getName().replaceAll("\\s+", "").trim();
            if (!fileName.isEmpty()) {
                modFileNames.add(fileName);
            }
        }

        return modFileNames;
    }

    private static boolean isModFileName(String fileName) {
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        return lowerFileName.endsWith(".jar")
                || lowerFileName.endsWith(".zip")
                || lowerFileName.endsWith(".litemod");
    }

    private static List<String> collectLoadedForgeModIds() {
        List<String> modIds = new ArrayList<>();

        for (ModContainer mod : Loader.instance().getModList()) {
            modIds.add(mod.getModId().toLowerCase(Locale.ROOT));
        }

        return modIds;
    }
}
