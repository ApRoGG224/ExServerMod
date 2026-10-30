package com.fome.exservermod.mixin;

import com.fome.exservermod.client.BossCustomTextureState;
import net.minecraft.client.renderer.entity.WardenRenderer;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WardenRenderer.class)
public abstract class WardenRendererBossTextureMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/WardenRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
    private void exservermod$getWardenBossTexture(WardenRenderState state, CallbackInfoReturnable<Identifier> cir) {
        if (state instanceof BossCustomTextureState bossState) {
            Identifier texture = bossState.exservermod$getCustomBossTexture();
            if (texture != null) {
                cir.setReturnValue(texture);
            }
        }
    }
}
