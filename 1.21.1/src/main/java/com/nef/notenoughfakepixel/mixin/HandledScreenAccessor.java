package com.nef.notenoughfakepixel.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Invoker("getSlotAt")
    Slot nef$slotAt(double x, double y);

    @Accessor("x")
    int nef$left();

    @Accessor("y")
    int nef$top();

    @Accessor("backgroundWidth")
    int nef$backgroundWidth();

    @Accessor("backgroundHeight")
    int nef$backgroundHeight();
}
