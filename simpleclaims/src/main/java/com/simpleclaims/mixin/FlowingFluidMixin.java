package com.simpleclaims.mixin;

import com.simpleclaims.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops water/lava placed just outside a claim from flowing in and paving over flowers,
 * grass, saplings, snow layers, and other "replaceable" decoration. spreadTo is the exact
 * per-neighbor "place the fluid at this position" step, so cancelling it only stops the
 * fluid from entering claimed ground - it doesn't affect the fluid's behavior anywhere else.
 */
@Mixin(FlowingFluid.class)
public class FlowingFluidMixin {

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void simpleclaims$preventFlowIntoClaim(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci) {
        if (level instanceof Level realLevel && ClaimManager.isProtected(realLevel, pos)) {
            ci.cancel();
        }
    }
}
