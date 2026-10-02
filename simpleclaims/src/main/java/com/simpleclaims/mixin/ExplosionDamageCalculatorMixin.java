package com.simpleclaims.mixin;

import com.simpleclaims.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin {

    @Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void getClaimExplosionResistance(Explosion explosion, BlockGetter blockGetter, BlockPos pos, BlockState state, FluidState fluidState, CallbackInfoReturnable<Optional<Float>> cir) {
        if (blockGetter instanceof Level level && ClaimManager.isProtected(level, pos)) {
            // Bedrock level blast resistance prevents explosion penetration
            cir.setReturnValue(Optional.of(3600000.0F));
        }
    }

    @Inject(method = "shouldBlockExplode", at = @At("HEAD"), cancellable = true)
    private void preventClaimExploding(Explosion explosion, BlockGetter blockGetter, BlockPos pos, BlockState state, float power, CallbackInfoReturnable<Boolean> cir) {
        if (blockGetter instanceof Level level && ClaimManager.isProtected(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
