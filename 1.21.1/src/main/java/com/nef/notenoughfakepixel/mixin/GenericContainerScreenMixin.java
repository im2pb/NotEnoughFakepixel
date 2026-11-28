package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.features.skyblock.enchanting.ExperimentationSolver;
import com.nef.notenoughfakepixel.features.skyblock.qol.slothighlight.HightlightSlot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GenericContainerScreen.class)
abstract class GenericContainerScreenMixin extends HandledScreen<GenericContainerScreenHandler> {
    protected GenericContainerScreenMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void nef$drawSlotHighlights(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        HightlightSlot.renderChestHighlights(context, handler, getTitle(), x, y);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/GenericContainerScreen;drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V"))
    private void nef$drawExperimentClues(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ExperimentationSolver.INSTANCE.render(MinecraftClient.getInstance(), this, context, x, y);
    }
}
