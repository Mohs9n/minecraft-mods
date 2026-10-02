package com.simpleclaims.mixin;

import com.simpleclaims.Claim;
import com.simpleclaims.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Without this, a hopper placed just outside a claim's border could slowly siphon items
 * out of any container sitting just inside it (or dump junk into one), entirely bypassing
 * the claim's break/place protection. A hopper may only move items between two positions
 * that are in the SAME claim (or both unclaimed) - an owner's own internal sorting systems
 * keep working, but nothing can reach across a claim boundary either direction.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private static void simpleclaims$preventCrossClaimSuck(Level level, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || level.isClientSide()) return;

        BlockPos hopperPos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ());
        BlockPos abovePos = BlockPos.containing(hopper.getLevelX(), hopper.getLevelY() + 1.0D, hopper.getLevelZ());
        if (crossesClaimBoundary(level, hopperPos, abovePos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "ejectItems", at = @At("HEAD"), cancellable = true)
    private static void simpleclaims$preventCrossClaimEject(Level level, BlockPos pos, HopperBlockEntity hopper, CallbackInfoReturnable<Boolean> cir) {
        if (level == null || level.isClientSide()) return;

        Direction facing = hopper.getBlockState().getValue(HopperBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        if (crossesClaimBoundary(level, pos, targetPos)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean crossesClaimBoundary(Level level, BlockPos a, BlockPos b) {
        Claim claimA = ClaimManager.getClaimAt(level, a);
        Claim claimB = ClaimManager.getClaimAt(level, b);
        if (claimA == null && claimB == null) return false;
        if (claimA == null || claimB == null) return true;
        return !claimA.getId().equals(claimB.getId());
    }
}
