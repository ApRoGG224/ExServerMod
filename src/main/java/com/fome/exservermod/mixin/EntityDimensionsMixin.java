package com.fome.exservermod.mixin;

import com.fome.exservermod.disguise.DisguiseUtils;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Avatar.class)
public abstract class EntityDimensionsMixin {

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void exservermod$overrideDisguisedDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if ((Object) this instanceof Player player) {
            EntityType<?> mobType = DisguiseUtils.getDisguiseType(player);
            if (mobType != null) {
                EntityDimensions dimensions = mobType.getDimensions();
                if (pose == Pose.CROUCHING) {
                    cir.setReturnValue(dimensions.scale(1.0f, 0.8333333f));
                } else if (pose == Pose.SWIMMING || pose == Pose.FALL_FLYING || pose == Pose.SPIN_ATTACK) {
                    float factor = Math.min(1.0f, 0.6f / dimensions.height());
                    cir.setReturnValue(dimensions.scale(1.0f, factor));
                } else {
                    cir.setReturnValue(dimensions);
                }
            }
        }
    }
}


