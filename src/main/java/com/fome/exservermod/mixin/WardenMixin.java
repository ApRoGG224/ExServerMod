package com.fome.exservermod.mixin;

import com.fome.exservermod.waves.WaveManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Warden.class)
public abstract class WardenMixin {

    @Inject(
        method = "applyDarknessAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void exservermod$cancelWaveWardenDarkness(
        ServerLevel level,
        Vec3 pos,
        Entity entity,
        int radius,
        CallbackInfo ci
    ) {
        if (entity != null && entity.entityTags().contains(WaveManager.TAG_WAVE_MOB)) {
            ci.cancel();
        }
    }
}
