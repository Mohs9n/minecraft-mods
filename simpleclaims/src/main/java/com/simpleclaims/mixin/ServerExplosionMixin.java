package com.simpleclaims.mixin;

import com.simpleclaims.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void protectClaimsFromExplosion(List<BlockPos> positions, CallbackInfo ci) {
        if (positions != null && !positions.isEmpty()) {
            positions.removeIf(pos -> ClaimManager.isProtected(this.level, pos));
        }
    }

    @Inject(method = "createFire", at = @At("HEAD"))
    private void protectClaimsFromExplosionFire(List<BlockPos> positions, CallbackInfo ci) {
        if (positions != null && !positions.isEmpty()) {
            positions.removeIf(pos -> ClaimManager.isProtected(this.level, pos));
        }
    }
}
