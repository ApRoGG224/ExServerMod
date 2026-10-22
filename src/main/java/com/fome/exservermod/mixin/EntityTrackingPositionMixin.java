package com.fome.exservermod.mixin;

import com.fome.exservermod.stealth.CauldronStealthUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityTrackingPositionMixin {

    @Inject(method = "trackingPosition", at = @At("HEAD"), cancellable = true)
    private void exservermod$offsetTrackingPositionInCauldron(CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this instanceof ServerPlayer player) {
            if (player.isCrouching() && CauldronStealthUtils.isInHollowBlock(player)) {
                cir.setReturnValue(player.position().add(0.0, CauldronStealthUtils.CROUCH_Y_OFFSET, 0.0));
            }
        }
    }
}
