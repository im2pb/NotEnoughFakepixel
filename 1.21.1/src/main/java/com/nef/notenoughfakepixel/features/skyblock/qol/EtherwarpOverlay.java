package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.utils.ConfigColor;
import com.nef.notenoughfakepixel.utils.WorldBoxRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.AbstractSkullBlock;
import net.minecraft.block.CarpetBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.BlockView;

import java.util.List;

final class EtherwarpOverlay {
    private static final String TOO_FAR = "Too far!";
    private static final String NOT_SOLID = "Not solid!";
    private static final String NO_HEADROOM = "No air above!";

    private final WorldBoxRenderer renderer = new WorldBoxRenderer();
    private final ConfigColor validColor = new ConfigColor();
    private final ConfigColor failedColor = new ConfigColor();
    private Target target;

    void register() {
        WorldRenderEvents.LAST.register(this::drawBlock);
        HudRenderCallback.EVENT.register((context, ticks) -> drawWarning(context));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> renderer.close());
    }

    void update(BlockView world, HitResult hit) {
        target = hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                ? inspect(world, block.getBlockPos()) : new Target(null, TOO_FAR);
    }

    void clear() {
        target = null;
    }

    private void drawBlock(WorldRenderContext context) {
        if (target == null || target.bounds() == null || MinecraftClient.getInstance().currentScreen != null) return;

        var settings = ClientConfig.configuration().qol.etherwarpSettings;
        if (!settings.qolEtherwarpOverlay) return;

        int color = target.warning() == null ? validColor.resolve(settings.qolEtherwarpOverlayColor)
                : failedColor.resolve(settings.qolEtherwarpFailedOverlayColor);
        var marker = List.of(new WorldBoxRenderer.Marker(target.bounds(), color));
        renderer.draw(context, marker, true, false);
        renderer.draw(context, marker, false, true);
    }

    private void drawWarning(DrawContext context) {
        var client = MinecraftClient.getInstance();
        if (target == null || target.warning() == null || client.currentScreen != null || client.options.hudHidden
                || !ClientConfig.configuration().qol.etherwarpSettings.qolEtherwarpText) return;

        context.drawCenteredTextWithShadow(client.textRenderer, "Can't TP: " + target.warning(),
                context.getScaledWindowWidth() / 2, context.getScaledWindowHeight() / 2 + 10, 0xFFFF5555);
    }

    static Target inspect(BlockView world, BlockPos pos) {
        var state = world.getBlockState(pos);
        var outline = state.getOutlineShape(world, pos);
        var bounds = outline.isEmpty() ? new Box(pos) : outline.getBoundingBox().offset(pos);
        var block = state.getBlock();

        String warning = null;
        if (block instanceof CarpetBlock || block instanceof AbstractSkullBlock
                || (!(block instanceof AbstractSignBlock) && state.getCollisionShape(world, pos).isEmpty())) {
            warning = NOT_SOLID;
        } else if (blocksLanding(world, pos.up()) || blocksLanding(world, pos.up(2))) {
            warning = NO_HEADROOM;
        }
        return new Target(bounds.expand(0.01), warning);
    }

    private static boolean blocksLanding(BlockView world, BlockPos pos) {
        var state = world.getBlockState(pos);
        var block = state.getBlock();
        return !(block instanceof CarpetBlock || block instanceof AbstractSkullBlock)
                && (block instanceof AbstractSignBlock || !state.getCollisionShape(world, pos).isEmpty());
    }

    record Target(Box bounds, String warning) {
    }
}
