package com.simplechestshop.mixin;

import com.simplechestshop.ChestShopManager;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityMixin {

    @Inject(method = "stopOpen", at = @At("RETURN"))
    private void onStopOpen(ContainerUser user, CallbackInfo ci) {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        ChestShopManager.onChestStopOpen(chest, user);
    }
}
