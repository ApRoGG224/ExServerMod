package com.fome.exservermod.mixin;

import com.fome.exservermod.halloween.client.HalloweenClientState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void exservermod$applyBloodSky(ClientLevel level, float f, Camera camera, SkyRenderState skyRenderState, CallbackInfo ci) {
        if (HalloweenClientState.isBloodSky() && level.dimension() == Level.OVERWORLD) {
            skyRenderState.skyColor = 0x660A0A;
            skyRenderState.sunriseAndSunsetColor = 0x881111;
        }
    }
}
