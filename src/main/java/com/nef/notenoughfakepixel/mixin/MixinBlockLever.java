/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.gui.Config;
import net.minecraft.block.BlockLever;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockLever.class)
public abstract class MixinBlockLever {

    @Inject(method = "setBlockBoundsBasedOnState", at = @At("HEAD"), cancellable = true)
    private void modifyLeverBoundingBox(IBlockAccess worldIn, BlockPos pos, CallbackInfo ci) {
        if (Config.feature.qol.qolFullBlockLever) {
            BlockLever lever = (BlockLever) (Object) this;
            IBlockState state = worldIn.getBlockState(pos);
            EnumFacing facing = state.getValue(BlockLever.FACING).getFacing();

            if (facing == EnumFacing.DOWN) {
                lever.setBlockBounds(0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F); 
            } else if (facing == EnumFacing.UP) {
                lever.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.5F, 1.0F); 
            } else if (facing == EnumFacing.NORTH) {
                lever.setBlockBounds(0.0F, 0.0F, 0.5F, 1.0F, 1.0F, 1.0F); 
            } else if (facing == EnumFacing.SOUTH) {
                lever.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.5F); 
            } else if (facing == EnumFacing.WEST) {
                lever.setBlockBounds(0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F); 
            } else if (facing == EnumFacing.EAST) {
                lever.setBlockBounds(0.0F, 0.0F, 0.0F, 0.5F, 1.0F, 1.0F); 
            }
            ci.cancel();
        }
    }
}
