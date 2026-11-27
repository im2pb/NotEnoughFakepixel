package com.nef.notenoughfakepixel.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.nef.notenoughfakepixel.features.skyblock.garden.VacuumPestsBag;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawContext.class)
abstract class DrawContextMixin {
    private static final String DRAW_ITEM_IN_SLOT =
            "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V";

    @ModifyVariable(method = DRAW_ITEM_IN_SLOT, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String nef$suppressVacuumQuantity(String override, TextRenderer font, ItemStack stack, int x, int y,
                                               String countOverride, @Share("nefVacuumCount") LocalIntRef count) {
        int pests = VacuumPestsBag.count(stack);
        count.set(pests);
        return pests >= 0 ? "" : override;
    }

    @Inject(method = DRAW_ITEM_IN_SLOT, at = @At("TAIL"))
    private void nef$drawVacuumCount(TextRenderer font, ItemStack stack, int x, int y, String countOverride,
                                     CallbackInfo ci, @Share("nefVacuumCount") LocalIntRef count) {
        if (count.get() >= 0) VacuumPestsBag.drawCount((DrawContext) (Object) this, font, x, y, count.get());
    }
}
