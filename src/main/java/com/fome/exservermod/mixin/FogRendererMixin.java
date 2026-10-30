package com.fome.exservermod.mixin;

import com.fome.exservermod.halloween.client.HalloweenClientState;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void exservermod$applyBloodFog(Camera camera, int i, DeltaTracker deltaTracker, float f, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        if (HalloweenClientState.isBloodSky() && level.dimension() == Level.OVERWORLD) {
            FogData data = cir.getReturnValue();
            if (data != null && data.color != null) {
                data.color.set(0.55f, 0.06f, 0.06f, 1.0f);
            }
        }
    }
}
