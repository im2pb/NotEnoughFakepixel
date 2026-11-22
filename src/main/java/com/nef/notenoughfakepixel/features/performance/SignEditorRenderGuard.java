/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.performance;

import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;

/** Keeps the temporary sign visible while the vanilla sign editor is active. */
public final class SignEditorRenderGuard {
    private SignEditorRenderGuard() {
    }

    /**
     * Returns true only for the sign rendered by the active sign editor. Other signs and
     * block entities continue through the normal performance culling policy.
     */
    public static boolean shouldBypassCull(TileEntity tileEntity, GuiScreen currentScreen) {
        return tileEntity instanceof TileEntitySign && currentScreen instanceof GuiEditSign;
    }
}
