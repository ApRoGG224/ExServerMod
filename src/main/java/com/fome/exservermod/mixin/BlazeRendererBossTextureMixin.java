package com.fome.exservermod.mixin;

import com.fome.exservermod.client.BossCustomTextureState;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlazeRenderer.class)
public abstract class BlazeRendererBossTextureMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
    private void exservermod$getBlazeBossTexture(LivingEntityRenderState state, CallbackInfoReturnable<Identifier> cir) {
        if (state instanceof BossCustomTextureState bossState) {
            Identifier texture = bossState.exservermod$getCustomBossTexture();
            if (texture != null) {
                cir.setReturnValue(texture);
            }
        }
    }
}
