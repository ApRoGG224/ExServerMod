package com.fome.exservermod.mixin;

import com.fome.exservermod.game.TrackerCompassManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotContainerMixin {

    @Shadow
    @Final
    public Container container;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void exservermod$preventTrackerCompassPlacement(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (TrackerCompassManager.isTrackerCompass(stack)) {
            if (!(this.container instanceof Inventory)) {
                cir.setReturnValue(false);
            }
        }
    }
}
