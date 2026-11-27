package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.ClientConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
abstract class StainedGlassPaneOutlineMixin {
    @Shadow public abstract Block getBlock();

    @Inject(method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void nef$fullBlockPaneOutline(BlockView world, BlockPos pos, ShapeContext context,
                                         CallbackInfoReturnable<VoxelShape> callback) {
        if (getBlock() instanceof StainedGlassPaneBlock && world instanceof ClientWorld
                && ClientConfig.configuration().mining.crystalHollows.crystalFullBlockPane) {
            callback.setReturnValue(VoxelShapes.fullCube());
        }
    }
}
