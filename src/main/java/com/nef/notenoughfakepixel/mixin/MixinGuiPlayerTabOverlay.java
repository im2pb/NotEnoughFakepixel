/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.gui.Config;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.RomanNumeralParser;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void nef$romanNumerals(NetworkPlayerInfo info, CallbackInfoReturnable<String> cir) {
        if (Config.feature == null || Config.feature.romanNumerals == null || !Config.feature.romanNumerals.tabList) return;
        if (!SkyblockData.getCurrentGamemode().isSkyblock()) return;

        String name = cir.getReturnValue();
        String converted = RomanNumeralParser.replaceInString(name);
        if (converted != name) cir.setReturnValue(converted); 
    }
}
