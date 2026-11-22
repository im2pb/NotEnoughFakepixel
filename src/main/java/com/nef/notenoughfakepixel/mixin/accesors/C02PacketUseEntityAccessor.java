/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin.accesors;

import net.minecraft.network.play.client.C02PacketUseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes only the packet's primitive entity id; it does not resolve the entity from World. */
@Mixin(C02PacketUseEntity.class)
public interface C02PacketUseEntityAccessor {

    @Accessor("entityId")
    int getEntityId();
}
