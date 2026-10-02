package com.simplechestshop.mixin;

import com.simplechestshop.ChestShopManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public class RandomizableContainerBlockEntityMixin {

    @Inject(method = "setItem", at = @At("RETURN"))
    private void onSetItem(int slot, ItemStack stack, CallbackInfo ci) {
        if ((Object) this instanceof ChestBlockEntity chest) {
            ChestShopManager.onChestContentsChanged(chest);
        }
    }
}
