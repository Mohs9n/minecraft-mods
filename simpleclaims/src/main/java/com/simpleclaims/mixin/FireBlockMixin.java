package com.simpleclaims.mixin;

import com.simpleclaims.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops ambient (non-explosion) fire from spreading into a claim - e.g. a fire left
 * burning just outside a wooden build can otherwise walk right in and burn it down.
 * checkBurnOut is FireBlock's per-neighbor "ignite/burn this specific position" helper
 * called from tick(); cancelling it when the target position is claimed blocks the spread
 * without touching the rest of the fire's own tick behavior (burning out, aging, etc.).
 */
@Mixin(FireBlock.class)
public class FireBlockMixin {

    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void simpleclaims$preventSpreadIntoClaim(Level level, BlockPos pos, int odds, RandomSource random, int age, CallbackInfo ci) {
        if (ClaimManager.isProtected(level, pos)) {
            ci.cancel();
        }
    }
}
