package com.simplechestshop.mixin;

import com.simplechestshop.ChestShopManager;
import com.simplechestshop.ShopTrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    /**
     * Prevents hoppers from extracting items from shop chests.
     */
    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void simplechestshop$preventShopSuck(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || level.isClientSide()) return;

        BlockPos targetPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0D, hopper.getLevelZ());
        BlockEntity be = level.getBlockEntity(targetPos);
        if (be instanceof ChestBlockEntity) {
            ShopTrade trade = ChestShopManager.getShopTrade(level, targetPos);
            if (trade != null && trade.isValid()) {
                cir.setReturnValue(false);
            }
        }
    }

    /**
     * Prevents hoppers from pushing items into shop chests.
     */
    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void simplechestshop$preventShopEject(Level level, BlockPos pos, HopperBlockEntity hopper, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || level.isClientSide()) return;

        Direction facing = hopper.getBlockState().getValue(net.minecraft.world.level.block.HopperBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        BlockEntity be = level.getBlockEntity(targetPos);
        if (be instanceof ChestBlockEntity) {
            ShopTrade trade = ChestShopManager.getShopTrade(level, targetPos);
            if (trade != null && trade.isValid()) {
                cir.setReturnValue(false);
            }
        }
    }
}
