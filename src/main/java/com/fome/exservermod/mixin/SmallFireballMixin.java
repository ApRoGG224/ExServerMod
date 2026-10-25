package com.fome.exservermod.mixin;

import com.fome.exservermod.waves.WaveManager;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireball.class)
public abstract class SmallFireballMixin {

    @Inject(
        method = "onHitBlock(Lnet/minecraft/world/phys/BlockHitResult;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void exservermod$cancelWaveMobFire(BlockHitResult result, CallbackInfo ci) {
        SmallFireball fireball = (SmallFireball) (Object) this;
        if (WaveManager.isWaveProjectile(fireball)) {
            ci.cancel();
            fireball.discard();
        }
    }
}
