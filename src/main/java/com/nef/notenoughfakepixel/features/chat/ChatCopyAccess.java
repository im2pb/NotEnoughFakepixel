/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.chat;

import net.minecraft.client.gui.ChatLine;

public interface ChatCopyAccess {

    ChatLine nef$getChatLineAt(int mouseX, int mouseY);

    int nef$getVisibleRowAt(int mouseX, int mouseY);

    int nef$getMessageFirstRow(int row);

    int nef$getMessageLastRow(int row);
}
