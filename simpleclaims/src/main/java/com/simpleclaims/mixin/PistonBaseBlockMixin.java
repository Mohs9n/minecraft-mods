package com.simpleclaims.mixin;

import com.simpleclaims.ClaimManager;
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
 * The README advertises piston-proof claims, but no mixin enforced it - this closes that gap.
 * Cancels the push/pull entirely (vanilla's own "obstructed" return value) if any block the
 * piston would move sits inside a claim, so claimed builds can't be dismantled block-by-block
 * from outside the border.
 */
@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {

    private static final int MAX_PUSH_DISTANCE = 13;

    @Inject(method = "moveBlocks", at = @At("HEAD"), cancellable = true)
    private void simpleclaims$protectClaimsFromPistons(Level level, BlockPos pos, Direction dir, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide()) return;

        if (extending) {
            BlockPos check = pos;
            for (int i = 0; i <= MAX_PUSH_DISTANCE; i++) {
                check = check.relative(dir);
                BlockState state = level.getBlockState(check);
                if (state.isAir()) break;
                if (ClaimManager.isProtected(level, check)) {
                    cir.setReturnValue(false);
                    return;
                }
                if (state.getPistonPushReaction() == PushReaction.IMMOVEABLE) {
                    break;
                }
            }
        } else {
            // Sticky-piston retraction only ever pulls the single block two spaces in front.
            BlockPos pullPos = pos.relative(dir, 2);
            if (ClaimManager.isProtected(level, pullPos)) {
                cir.setReturnValue(false);
            }
        }
    }
}
