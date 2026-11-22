/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.features.chat;

import net.minecraft.util.IChatComponent;

public interface ChatLineAccess {

    IChatComponent nef$getFullMessage();

    void nef$setFullMessage(IChatComponent message);
}
