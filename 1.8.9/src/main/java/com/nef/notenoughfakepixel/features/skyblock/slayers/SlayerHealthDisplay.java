package com.nef.notenoughfakepixel.features.skyblock.slayers;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.config.position.Position;
import com.nef.notenoughfakepixel.env.registers.RegisterEvents;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

@RegisterEvents
public class SlayerHealthDisplay {
    private final Minecraft mc = Minecraft.getMinecraft();
    private String displayText = "";
    private boolean isBoss = false;
    private final Position position;
    private int ticks = 0;

    private boolean isDemonlordFight = false;
    private final List<MinionInfo> minionInfos = new ArrayList<>();

    public SlayerHealthDisplay() {
        this.position = Config.feature.slayer.bossSettings.slayerBossHPPos;
    }

    private static class MinionInfo {
        final String attunement;
        final String hp;
        final Boolean attackable; // null = unknown

        MinionInfo(String attunement, String hp, Boolean attackable) {
            this.attunement = attunement;
            this.hp = hp;
            this.attackable = attackable;
        }
    }

    public static final String[] SLAYER_BOSSES = {
            "Revenant Horror",
            "Atoned Horror",
            "Sven Packmaster",
            "Tarantula Broodfather",
            "Voidgloom Seraph",
            "Inferno Demonlord"
    };

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.side != net.minecraftforge.fml.relauncher.Side.CLIENT || mc.theWorld == null) return;

        ticks++;
        if (ticks % 10 != 0) return;

        if (!Config.feature.slayer.bossSettings.slayerBossHP) return;

        isBoss = SkyblockData.isBossActive();
        if (!isBoss || mc.thePlayer == null) {
            displayText = "";
            isDemonlordFight = false;
            minionInfos.clear();
            return;
        }

        List<Entity> entities = mc.theWorld.getLoadedEntityList();
        Entity closestBoss = null;
        double closestDistance = Double.MAX_VALUE;
        String bossNameFound = "";

        for (Entity entity : entities) {
            if (!(entity instanceof EntityArmorStand) || !entity.hasCustomName()) continue;
            String entityName = entity.getCustomNameTag();
            for (String bossName : SLAYER_BOSSES) {
                if (entityName.contains(bossName)) {
                    double distance = mc.thePlayer.getDistanceToEntity(entity);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestBoss = entity;
                        bossNameFound = entityName;
                    }
                    break;
                }
            }
        }

        if (closestBoss != null) {
            if (bossNameFound.contains("Inferno Demonlord")) {
                isDemonlordFight = true;
                minionInfos.clear();
                String attunement = findAttunementNear(entities, closestBoss, 4.0);
                displayText = bossNameFound + (attunement.isEmpty() ? "" : " " + colorCode(attunement) + "[" + attunement + "]§r");
            } else {
                isDemonlordFight = false;
                minionInfos.clear();
                displayText = bossNameFound;
            }
        } else if (isDemonlordFight) {
            // Boss armor stand gone — minion phase
            displayText = "";
            updateMinionInfos(entities);
        } else {
            displayText = "";
            minionInfos.clear();
        }
    }

    private String findAttunementNear(List<Entity> entities, Entity reference, double maxDist) {
        double maxDistSq = maxDist * maxDist;
        for (Entity e : entities) {
            if (!(e instanceof EntityArmorStand) || e == reference || !e.hasCustomName()) continue;
            if (reference.getDistanceSqToEntity(e) > maxDistSq) continue;
            String name = ((EntityArmorStand) e).getDisplayName().getUnformattedText();
            Matcher m = BlazeAttunements.COLOR_PATTERN.matcher(name);
            if (m.find()) return m.group().toUpperCase();
        }
        return "";
    }

    private void updateMinionInfos(List<Entity> entities) {
        minionInfos.clear();

        for (Entity e : entities) {
            boolean isWither = e instanceof EntitySkeleton && ((EntitySkeleton) e).getSkeletonType() == 1;
            boolean isPigZombie = e instanceof EntityPigZombie;
            if ((!isWither && !isPigZombie) || mc.thePlayer.getDistanceSqToEntity(e) > 50) continue;

            net.minecraft.entity.EntityLivingBase minion = (net.minecraft.entity.EntityLivingBase) e;
            String attunement = BlazeAttunements.blazeEntity.get(minion);
            boolean attackable = attunement != null;

            // Find HP from the nearest armor stand with a health bar
            String hp = "";
            for (Entity stand : entities) {
                if (!(stand instanceof EntityArmorStand)) continue;
                if (stand.getDistanceSqToEntity(minion) > 25.0) continue;
                String name = stand.getDisplayName().getUnformattedText();
                if (name.contains("❤")) { hp = name; break; }
            }

            if (attunement == null && hp.isEmpty()) continue;
            minionInfos.add(new MinionInfo(attunement != null ? attunement : "", hp, attackable));
        }
    }

    private String colorCode(String attunement) {
        switch (attunement) {
            case "SPIRIT": return "§f";
            case "ASHEN": return "§8";
            case "CRYSTAL": return "§b";
            case "AURIC": return "§e";
            default: return "§r";
        }
    }

    private int attunementColor(String attunement) {
        switch (attunement) {
            case "ASHEN": return 0x555555;
            case "SPIRIT": return 0xFFFFFF;
            case "CRYSTAL": return 0x55FFFF;
            case "AURIC": return 0xFFFF55;
            default: return 0xFF5555;
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !isBoss) return;

        boolean showBoss = !displayText.isEmpty();
        boolean showMinions = !minionInfos.isEmpty();
        if (!showBoss && !showMinions) return;

        ScaledResolution resolution = event.resolution;
        FontRenderer fr = mc.fontRendererObj;
        float scale = 2.0F;

        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, scale);

        if (showBoss) {
            int textWidth = fr.getStringWidth(displayText);
            int x = position.getAbsX(resolution, textWidth) / (int) scale;
            int y = position.getAbsY(resolution, fr.FONT_HEIGHT) / (int) scale;
            fr.drawStringWithShadow(displayText, x, y, 0xFF5555);
        } else {
            int lineHeight = fr.FONT_HEIGHT + 2;
            int totalHeight = lineHeight * minionInfos.size();
            String firstLine = buildMinionLine(minionInfos.get(0));
            int x = position.getAbsX(resolution, fr.getStringWidth(firstLine)) / (int) scale;
            int baseY = position.getAbsY(resolution, totalHeight) / (int) scale;

            for (int i = 0; i < minionInfos.size(); i++) {
                MinionInfo info = minionInfos.get(i);
                fr.drawStringWithShadow(buildMinionLine(info), x, baseY + i * lineHeight, attunementColor(info.attunement));
            }
        }

        GL11.glPopMatrix();
    }

    private String buildMinionLine(MinionInfo info) {
        StringBuilder sb = new StringBuilder();
        if (!info.attunement.isEmpty()) sb.append("[").append(info.attunement).append("] ");
        sb.append(info.attackable ? "ATTACK" : "IMMUNE");
        if (!info.hp.isEmpty()) sb.append(" ").append(info.hp);
        return sb.toString();
    }

    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        displayText = "";
        isBoss = false;
        isDemonlordFight = false;
        minionInfos.clear();
    }
}

