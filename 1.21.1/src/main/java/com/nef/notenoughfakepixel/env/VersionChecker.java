package com.nef.notenoughfakepixel.env;

import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.nefrepo.NefRepo;
import com.nef.notenoughfakepixel.nefrepo.NefRepo.Release;
import com.nef.notenoughfakepixel.nefrepo.NefRepo.Update;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@RegisterEvents
public final class VersionChecker implements ClientModInitializer {
    private static final String WELCOME = "Welcome to Fakepixel SkyBlock!";
    private static final String DEFAULT_DISCORD = "https://discord.gg/xjFy4GQmbp";
    private final Set<String> shownWarnings = new HashSet<>();
    private final Set<String> shownVersions = new HashSet<>();
    private boolean welcomed;
    private Update seen;

    @Override
    public void onInitializeClient() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay && message.getString().lines()
                    .anyMatch(line -> WELCOME.equals(Formatting.strip(line).strip()))) welcomed = true;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> welcomed = false));
        ClientTickEvents.END_CLIENT_TICK.register(this::notifyOnJoin);
    }

    private void notifyOnJoin(MinecraftClient client) {
        if (!welcomed || client.player == null || client.getNetworkHandler() == null) return;
        var update = NefRepo.update().snapshot().data();
        if (update == null || update == seen) return;

        seen = update;
        publishNotices(installedVersion(), update, message -> addChat(client, message));
    }

    private void publishNotices(String installed, Update update, Consumer<Text> publish) {
        if (hasWarning(update) && shownWarnings.add(update.warningMessage())) {
            publish.accept(Text.literal(update.warningMessage()));
        }
        Release release = update.fabric();
        if (release != null && isNewer(installed, release.version()) && shownVersions.add(release.version())) {
            publish.accept(releaseMessage(installed, release));
        }
    }

    public static String installedVersion() {
        return FabricLoader.getInstance().getModContainer("notenoughfakepixel")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    public static void reportVersion() {
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String installed = installedVersion();
        addChat(client, Text.literal("[NEF] Installed Fabric version " + installed).formatted(Formatting.GOLD));
        var snapshot = NefRepo.update().snapshot();
        var update = snapshot.data();
        if (update == null) {
            addChat(client, Text.literal(switch (snapshot.status()) {
                case LOADING -> "Loading repository version information.";
                case UNAVAILABLE, READY -> "Repository version information is unavailable.";
            }).formatted(Formatting.GRAY));
            return;
        }

        if (hasWarning(update)) addChat(client, Text.literal(update.warningMessage()));
        var release = update.fabric();
        if (release != null && isNewer(installed, release.version())) {
            addChat(client, releaseMessage(installed, release));
        } else {
            String status = release == null ? "The repository has no Fabric release entry."
                    : "Repository Fabric release " + release.version();
            addChat(client, Text.literal(status).formatted(Formatting.GRAY));
        }
    }

    public static void openDiscord() {
        var update = NefRepo.update().snapshot().data();
        String url = update == null ? null : httpsLink(update.discordLink());
        Util.getOperatingSystem().open(url == null ? DEFAULT_DISCORD : url);
    }

    private static boolean hasWarning(Update update) {
        return update.hasWarning() && !update.warningMessage().isBlank();
    }

    static boolean isNewer(String current, String latest) {
        try {
            return Version.parse(current) instanceof SemanticVersion installed
                    && Version.parse(latest) instanceof SemanticVersion available
                    && available.compareTo(installed) > 0;
        } catch (VersionParsingException ignored) {
            return false;
        }
    }

    static Text releaseMessage(String installed, Release release) {
        String template = release.message().isBlank()
                ? "NotEnoughFakepixel Fabric ${latestVersion} is available. Installed version ${currentVersion}."
                : release.message();
        var message = Text.literal(template.replace("${currentVersion}", installed)
                .replace("${latestVersion}", release.version()));
        String link = httpsLink(release.updateLink());
        if (link != null) {
            message.append(Text.literal(" [Fabric download]").formatted(Formatting.AQUA, Formatting.UNDERLINE)
                    .styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link))));
        }
        return message;
    }

    static String httpsLink(String value) {
        try {
            URI uri = URI.create(value.strip());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
                    ? uri.toASCIIString() : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void addChat(MinecraftClient client, Text message) {
        client.inGameHud.getChatHud().addMessage(message);
    }
}
