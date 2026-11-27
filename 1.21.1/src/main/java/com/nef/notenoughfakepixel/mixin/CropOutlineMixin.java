package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.ClientConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(AbstractBlock.AbstractBlockState.class)
abstract class CropOutlineMixin {
    @Unique private static final VoxelShape[] nef$wheat = nef$shapes(2, 4, 6, 8, 10, 12, 14, 16);
    @Unique private static final VoxelShape[] nef$carrotsAndPotatoes = nef$shapes(2, 3, 4, 5, 6, 7, 8, 16);
    @Unique private static final VoxelShape[] nef$netherWart = nef$shapes(5, 8, 11, 16);

    @Shadow public abstract Block getBlock();

    @Inject(method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void nef$useLegacyCropHeight(BlockView world, BlockPos pos, ShapeContext context,
                                        CallbackInfoReturnable<VoxelShape> callback) {
        Block block = getBlock();
        if (!(block instanceof CropBlock || block instanceof NetherWartBlock) || !(world instanceof ClientWorld)) return;
        if (!ClientConfig.configuration().qol.qolCropsHeight) return;

        var state = (AbstractBlock.AbstractBlockState) (Object) this;
        if (block == Blocks.WHEAT) {
            callback.setReturnValue(nef$wheat[state.get(CropBlock.AGE)]);
        } else if (block == Blocks.CARROTS || block == Blocks.POTATOES) {
            callback.setReturnValue(nef$carrotsAndPotatoes[state.get(CropBlock.AGE)]);
        } else if (block == Blocks.NETHER_WART) {
            callback.setReturnValue(nef$netherWart[state.get(NetherWartBlock.AGE)]);
        }
    }

    @Unique
    private static VoxelShape[] nef$shapes(int... heights) {
        return Arrays.stream(heights)
                .mapToObj(height -> Block.createCuboidShape(0, 0, 0, 16, height, 16))
                .toArray(VoxelShape[]::new);
    }
}
