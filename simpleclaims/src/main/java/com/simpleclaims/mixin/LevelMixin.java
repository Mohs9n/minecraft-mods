package com.simpleclaims.mixin;

import com.simpleclaims.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void protectClaimsFromDestroy(BlockPos pos, boolean dropResources, Entity entity, int recursionLimit, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (ClaimManager.isProtected(level, pos)) {
            if (entity instanceof Player player && ClaimManager.canPlayerModify(player, level, pos)) {
                return;
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "removeBlock", at = @At("HEAD"), cancellable = true)
    private void protectClaimsFromRemove(BlockPos pos, boolean isMoving, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (ClaimManager.isProtected(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
