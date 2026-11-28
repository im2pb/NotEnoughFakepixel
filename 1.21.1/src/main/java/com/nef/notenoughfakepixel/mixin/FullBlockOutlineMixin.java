package com.nef.notenoughfakepixel.mixin;

import com.nef.notenoughfakepixel.config.ClientConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(AbstractBlock.AbstractBlockState.class)
abstract class FullBlockOutlineMixin {
    @Unique private static final Map<Direction, VoxelShape> nef$leverShapes = nef$faceShapes(8);
    @Unique private static final Map<Direction, VoxelShape> nef$buttonShapes = nef$faceShapes(4);

    @Shadow public abstract Block getBlock();

    @Inject(method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void nef$expandOutline(BlockView world, BlockPos pos, ShapeContext context,
                                   CallbackInfoReturnable<VoxelShape> callback) {
        if (!(world instanceof ClientWorld)) return;

        Block block = getBlock();
        if (block instanceof StainedGlassPaneBlock) {
            if (ClientConfig.configuration().mining.crystalHollows.crystalFullBlockPane) {
                callback.setReturnValue(VoxelShapes.fullCube());
            }
        } else if (block instanceof LeverBlock && ClientConfig.configuration().qol.qolFullBlockLever) {
            callback.setReturnValue(nef$leverShapes.get(nef$mountedDirection()));
        } else if (block instanceof ButtonBlock && ClientConfig.configuration().qol.qolFullBlockButton) {
            callback.setReturnValue(nef$buttonShapes.get(nef$mountedDirection()));
        }
    }

    @Unique
    private Direction nef$mountedDirection() {
        var state = (AbstractBlock.AbstractBlockState) (Object) this;
        return switch (state.get(WallMountedBlock.FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.get(WallMountedBlock.FACING);
        };
    }

    @Unique
    private static Map<Direction, VoxelShape> nef$faceShapes(int thickness) {
        int inset = 16 - thickness;
        return Map.of(
                Direction.UP, Block.createCuboidShape(0, 0, 0, 16, thickness, 16),
                Direction.DOWN, Block.createCuboidShape(0, inset, 0, 16, 16, 16),
                Direction.NORTH, Block.createCuboidShape(0, 0, inset, 16, 16, 16),
                Direction.SOUTH, Block.createCuboidShape(0, 0, 0, 16, 16, thickness),
                Direction.WEST, Block.createCuboidShape(inset, 0, 0, 16, 16, 16),
                Direction.EAST, Block.createCuboidShape(0, 0, 0, thickness, 16, 16));
    }
}
