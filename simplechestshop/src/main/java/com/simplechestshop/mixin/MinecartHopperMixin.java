package com.simplechestshop.mixin;

import com.simplechestshop.ChestShopManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A hopper minecart has its own separate suckInItems() (it does NOT share
 * HopperBlockEntity's static method, which only HopperBlockEntityMixin covers), so without
 * this a thief could park one next to a shop chest to vacuum its stock/payments.
 */
@Mixin(MinecartHopper.class)
public abstract class MinecartHopperMixin {

    @Inject(method = "suckInItems", at = @At("HEAD"), cancellable = true)
    private void simplechestshop$preventShopSuck(CallbackInfoReturnable<Boolean> cir) {
        MinecartHopper self = (MinecartHopper) (Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;
        BlockPos pos = self.blockPosition();
        if (ChestShopManager.isProtectedShop(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
