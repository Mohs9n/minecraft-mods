package com.simplechestshop.mixin;

import com.simplechestshop.ChestShopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Without this, a piston could shove a registered shop chest (or anything behind it) around
 * the world, completely bypassing the break/explosion/hopper protections that assume the
 * chest never moves. Cancels the push/pull if any affected block is a protected shop chest.
 */
@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {

    private static final int MAX_PUSH_DISTANCE = 13;

    @Inject(method = "moveBlocks", at = @At("HEAD"), cancellable = true)
    private void simplechestshop$protectShopsFromPistons(Level level, BlockPos pos, Direction dir, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;

        if (extending) {
            BlockPos check = pos;
            for (int i = 0; i <= MAX_PUSH_DISTANCE; i++) {
                check = check.relative(dir);
                BlockState state = level.getBlockState(check);
                if (state.isAir()) break;
                if (ChestShopManager.isProtectedShop(level, check)) {
                    cir.setReturnValue(false);
                    return;
                }
                if (state.getPistonPushReaction() == PushReaction.IMMOVEABLE) {
                    break;
                }
            }
        } else {
            BlockPos pullPos = pos.relative(dir, 2);
            if (ChestShopManager.isProtectedShop(level, pullPos)) {
                cir.setReturnValue(false);
            }
        }
    }
}
