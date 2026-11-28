package com.nef.notenoughfakepixel.features.skyblock.qol;

import com.nef.notenoughfakepixel.config.ClientConfig;
import com.nef.notenoughfakepixel.serverdata.SkyblockData;
import com.nef.notenoughfakepixel.utils.SkyblockItemData;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;

public final class SwordOffhandShield {
    private SwordOffhandShield() {
    }

    // Keep the off-hand stack and its use behavior so the legacy translator receives both hand interactions.
    public static boolean hidden(LivingEntity player, ItemStack offhand) {
        if (!offhand.isOf(Items.SHIELD) || !(player instanceof ClientPlayerEntity localPlayer)
                || !SkyblockData.isSkyblock() || !ClientConfig.configuration().qol.visualTweaks.qolHideSwordOffhandShield) {
            return false;
        }

        ItemStack mainHand = localPlayer.getMainHandStack();
        return mainHand.getItem() instanceof SwordItem || SkyblockItemData.isAspect(SkyblockItemData.id(mainHand));
    }
}
