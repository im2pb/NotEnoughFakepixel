package com.nef.notenoughfakepixel.features.skyblock.overlays.stats;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RegisterEvents
public class StatBars {

    /** Height in pixels of every bar (also used as the UV sample height). */
    public static final int BAR_H = 9;

    private static final Pattern HEALTH_PATTERN  = Pattern.compile("(?<health>[0-9,]+)/(?<maxHealth>[0-9,]+)❤.*");
    private static final Pattern MANA_PATTERN    = Pattern.compile("(?<mana>[0-9,]+)/(?<maxMana>[0-9,]+)✎ Mana( ?(?<overflow>-?[0-9,]+)ʬ)?.*");
    private static final Pattern DEFENSE_PATTERN = Pattern.compile("(?<defense>[0-9,]+)❇ Defense.*");

    @Getter
    public static final PlayerStats STATS = new PlayerStats();

    // -----------------------------------------------------------------------
    // Bar type definitions
    // -----------------------------------------------------------------------

    public enum BarType {
        HEALTH(
                new ResourceLocation("notenoughfakepixel", "skyblock/stats/heart.png"),
                new Color(255, 0,   0),
                new Color(214, 19,  19)
        ),
        MANA(
                new ResourceLocation("notenoughfakepixel", "skyblock/stats/mana.png"),
                new Color(19,  231, 255),
                null
        ),
        EXP(
                new ResourceLocation("notenoughfakepixel", "skyblock/stats/exp.png"),
                new Color(200, 255, 143),
                null
        ),
        SPEED(
                new ResourceLocation("notenoughfakepixel", "skyblock/stats/speed.png"),
                Color.WHITE,
                null
        ),
        DEFENCE(
                new ResourceLocation("notenoughfakepixel", "skyblock/stats/defense.png"),
                Color.WHITE,
                null
        );

        public final ResourceLocation icon;
        /** Primary colour for this bar. */
        public final Color color;
        /** Overflow / absorption colour, or {@code null} if not applicable. */
        public final Color overflowColor;

        BarType(ResourceLocation icon, Color color, Color overflowColor) {
            this.icon = icon;
            this.color = color;
            this.overflowColor = overflowColor;
        }

        public int getX() {
            switch (this) {
                case HEALTH:  return Config.feature.overlays.healthBarSettings.posHealth.getRawX();
                case MANA:    return Config.feature.overlays.manaBarSettings.posMana.getRawX();
                case SPEED:   return Config.feature.overlays.speedBarSettings.posSpeed.getRawX();
                case EXP:     return Config.feature.overlays.expBarSettings.posExp.getRawX();
                default:      return Config.feature.overlays.defenceBarSettings.posDefense.getRawX();
            }
        }

        public int getY() {
            switch (this) {
                case HEALTH:  return Config.feature.overlays.healthBarSettings.posHealth.getRawY();
                case MANA:    return Config.feature.overlays.manaBarSettings.posMana.getRawY();
                case SPEED:   return Config.feature.overlays.speedBarSettings.posSpeed.getRawY();
                case EXP:     return Config.feature.overlays.expBarSettings.posExp.getRawY();
                default:      return Config.feature.overlays.defenceBarSettings.posDefense.getRawY();
            }
        }

        public float getFill() {
            switch (this) {
                case HEALTH:
                    return STATS.getMaxHealth() == 0 ? 0f
                            : Math.min(1f, (float) STATS.getHealth() / STATS.getMaxHealth());
                case MANA:
                    return STATS.getMaxMana() == 0 ? 0f
                            : Math.min(1f, (float) STATS.getMana() / STATS.getMaxMana());
                case SPEED:   return Math.min(1f, STATS.getSpeed() / 400f);
                case EXP:     return Math.min(1f, STATS.getExp());
                case DEFENCE: return Math.min(1f, STATS.getDefence() / 1000f);
                default:      return 0f;
            }
        }

        public boolean hasOverflow() {
            return this == HEALTH && STATS.getOverflowHealth() > 0;
        }
    }

    // -----------------------------------------------------------------------
    // Bar length — each size has its own pair of textures
    // -----------------------------------------------------------------------

    public enum BarLength {
        TINY  (34,  "size_0"),
        SMALL (50,  "size_1"),
        MEDIUM(80,  "size_2"),
        LARGE (182, "size_3");

        public final int width;
        /** Background texture (empty / dark pill shape). */
        public final ResourceLocation baseTexture;
        /** Fill texture (bright pill shape, tinted by GL color). */
        public final ResourceLocation fillTexture;

        BarLength(int width, String prefix) {
            this.width = width;
            this.baseTexture = new ResourceLocation("notenoughfakepixel",
                    "skyblock/stats/bars/" + prefix + "_base.png");
            this.fillTexture = new ResourceLocation("notenoughfakepixel",
                    "skyblock/stats/bars/" + prefix + "_fill.png");
        }
    }

    // -----------------------------------------------------------------------
    // Events
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (e.type != 2) return;
        String raw = e.message.getFormattedText();
        if (raw.contains("❤") || raw.contains("✎") || raw.contains("❇")) {
            updateStats(raw);
        }
        if (Config.feature.overlays.statBars.disableActionBar
                && e.message.getUnformattedText().contains("Defense")) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDraw(RenderGameOverlayEvent e) {
        if (!STATS.isValid() || !Config.feature.overlays.statBars.disableIcons) return;
        if (e.type == RenderGameOverlayEvent.ElementType.EXPERIENCE
                || e.type == RenderGameOverlayEvent.ElementType.ARMOR
                || e.type == RenderGameOverlayEvent.ElementType.HEALTH
                || e.type == RenderGameOverlayEvent.ElementType.HEALTHMOUNT
                || e.type == RenderGameOverlayEvent.ElementType.FOOD) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post e) {
        if (e.type != RenderGameOverlayEvent.ElementType.HOTBAR
                || !Config.feature.overlays.statBars.statOverlay
                || !STATS.isValid()) return;

        for (BarType type : BarType.values()) {
            switch (type) {
                case HEALTH:
                    if (!Config.feature.overlays.healthBarSettings.healthBar) continue;
                    break;
                case MANA:
                    if (!Config.feature.overlays.manaBarSettings.manaBar) continue;
                    break;
                case SPEED:
                    if (!Config.feature.overlays.speedBarSettings.speedBar) continue;
                    break;
                case DEFENCE:
                    if (!Config.feature.overlays.defenceBarSettings.defenceBar) continue;
                    break;
                case EXP:
                    if (!Config.feature.overlays.expBarSettings.expBar) continue;
                    break;
                default:
                    break;
            }
            drawBar(type, getBarLength(type));
        }
    }

    // -----------------------------------------------------------------------
    // Stat parsing
    // -----------------------------------------------------------------------

    private void updateStats(String formatted) {
        String[] sections = formatted.split(" {3,}");
        for (String section : sections) {
            String stripped = StringUtils.stripControlCodes(section).trim();
            try {
                if (section.contains("❤")) {
                    parseHealth(stripped);
                } else if (section.contains("❇")) {
                    parseDefense(stripped);
                } else if (section.contains("✎")) {
                    parseMana(stripped);
                }
            } catch (Exception ignored) {
            }
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            STATS.setExp(mc.thePlayer.experience, mc.thePlayer.experienceLevel);
        }
    }

    private void parseHealth(String stripped) {
        String cleaned = stripped.replaceAll("\\+[0-9,]+[▆▅▄▃▂▁]", "").trim();
        Matcher m = HEALTH_PATTERN.matcher(cleaned);
        if (m.matches()) {
            STATS.setHealth(parseNum(m.group("health")), parseNum(m.group("maxHealth")));
        }
    }

    private void parseMana(String stripped) {
        Matcher m = MANA_PATTERN.matcher(stripped);
        if (m.matches()) {
            String ovStr = m.group("overflow");
            STATS.setMana(parseNum(m.group("mana")), parseNum(m.group("maxMana")),
                    ovStr != null ? parseNum(ovStr) : 0);
        }
    }

    private void parseDefense(String stripped) {
        Matcher m = DEFENSE_PATTERN.matcher(stripped);
        if (m.matches()) {
            STATS.setDefence(parseNum(m.group("defense")));
        }
    }

    private static int parseNum(String s) {
        if (s == null) return 0;
        try {
            return Integer.parseInt(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    private void drawBar(BarType type, BarLength length) {
        int x = type.getX();
        int y = type.getY();

        GlStateManager.pushMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();

        // Icon
        GlStateManager.color(1f, 1f, 1f, 1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(type.icon);
        Gui.drawModalRectWithCustomSizedTexture(x - 11, y - 1, 0, 0, 9, 9, 9, 9);

        drawMultiLayeredBar(type, x, y, length);

        // Label
        String label = getLabel(type);
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        float textX = x + (float) length.width / 2f - (float) fr.getStringWidth(label) / 2f;
        fr.drawStringWithShadow(label, textX, y + 1, type.color.getRGB());

        GlStateManager.popMatrix();
    }

    /**
     * Draws the bar using the size-matched base + fill textures.
     *
     * <p>Draw order:
     * <ol>
     *   <li>Base texture at full width — the dark empty pill background.</li>
     *   <li>Fill texture clipped to the filled fraction — tinted by the bar colour.</li>
     *   <li>Overflow fill (right of fill line) — tinted by the overflow colour.</li>
     * </ol>
     */
    private void drawMultiLayeredBar(BarType type, int x, int y, BarLength length) {
        int barWidth  = length.width;
        float fill    = type.getFill();
        int fillPx    = Math.round(barWidth * fill);

        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();

        // 1. Base (dark background pill) — white so the texture keeps its original dark tone
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(length.baseTexture);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, barWidth, BAR_H, barWidth, BAR_H);

        // 2. Fill portion — tint the fill texture to the bar's colour, clip by UV
        if (fillPx > 0) {
            setGlColor(type.color);
            mc.getTextureManager().bindTexture(length.fillTexture);
            // UV samples 0..fillPx of a barWidth-wide texture → only the left fraction shows
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, fillPx, BAR_H, barWidth, BAR_H);
        }

        // 3. Overflow / absorption — tint remainder with overflow colour
        if (type.hasOverflow() && type.overflowColor != null && fillPx < barWidth) {
            setGlColor(type.overflowColor);
            mc.getTextureManager().bindTexture(length.fillTexture);
            int remainPx = barWidth - fillPx;
            // UV samples fillPx..barWidth → the right portion of the fill texture
            Gui.drawModalRectWithCustomSizedTexture(
                    x + fillPx, y,
                    fillPx, 0,
                    remainPx, BAR_H,
                    barWidth, BAR_H);
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Sets the GL colour from a {@link Color} (all channels 0–1). */
    private static void setGlColor(Color c) {
        GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 1f);
    }

    private String getLabel(BarType type) {
        switch (type) {
            case HEALTH:  return STATS.getHealth() + "/" + STATS.getMaxHealth();
            case MANA:    return STATS.getMana()   + "/" + STATS.getMaxMana();
            case SPEED:   return String.valueOf(STATS.getSpeed());
            case DEFENCE: return String.valueOf(STATS.getDefence());
            case EXP:     return "Lvl " + STATS.getExpLevel();
            default:      return "";
        }
    }

    // -----------------------------------------------------------------------
    // Bar length helpers
    // -----------------------------------------------------------------------

    public BarLength getBarLength(BarType type) {
        switch (type) {
            case HEALTH:  return fromIndex(Config.feature.overlays.healthBarSettings.barLengthH);
            case MANA:    return fromIndex(Config.feature.overlays.manaBarSettings.barLengthM);
            case EXP:     return fromIndex(Config.feature.overlays.expBarSettings.barLengthE);
            case SPEED:   return fromIndex(Config.feature.overlays.speedBarSettings.barLengthS);
            default:      return fromIndex(Config.feature.overlays.defenceBarSettings.barLengthD);
        }
    }

    private BarLength fromIndex(int i) {
        switch (i) {
            case 0:  return BarLength.TINY;
            case 1:  return BarLength.SMALL;
            case 2:  return BarLength.MEDIUM;
            default: return BarLength.LARGE;
        }
    }
}

