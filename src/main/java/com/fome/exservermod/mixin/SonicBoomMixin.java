package com.fome.exservermod.mixin;

import com.fome.exservermod.waves.WaveManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SonicBoom.class)
public abstract class SonicBoomMixin {

    @Inject(
        method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void exservermod$cancelWaveWardenSonicBoom(ServerLevel level, Warden warden, CallbackInfoReturnable<Boolean> cir) {
        if (warden.entityTags().contains(WaveManager.TAG_WAVE_MOB)) {
            cir.setReturnValue(false);
        }
    }
}
