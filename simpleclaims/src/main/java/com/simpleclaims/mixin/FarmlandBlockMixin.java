package com.simpleclaims.mixin;

import com.simpleclaims.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents trampling farmland into dirt inside a claim (jumping on crops, a mob/ravager
 * stepping on them). Targets turnToBaseBlock specifically rather than cancelling fallOn()
 * entirely, so normal fall damage on farmland is completely unaffected - only the "convert
 * to dirt" side effect is blocked. Trampling has no owner to bypass: the owner doesn't want
 * their own crops trampled either, so this is a blanket cancel like the explosion mixins.
 */
@Mixin(FarmlandBlock.class)
public class FarmlandBlockMixin {

    @Inject(method = "turnToBaseBlock", at = @At("HEAD"), cancellable = true)
    private void simpleclaims$preventTrample(Entity entity, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (ClaimManager.isProtected(level, pos)) {
            ci.cancel();
        }
    }
}
