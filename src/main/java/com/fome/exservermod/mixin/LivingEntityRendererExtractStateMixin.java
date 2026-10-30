package com.fome.exservermod.mixin;

import com.fome.exservermod.client.BossCustomTextureState;
import com.fome.exservermod.client.BossCustomTextures;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererExtractStateMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void exservermod$extractBossTexture(T entity, S state, float partialTicks, CallbackInfo ci) {
        if (state instanceof BossCustomTextureState bossState && entity.hasCustomName()) {
            Identifier texture = BossCustomTextures.INSTANCE.getBossTextureFromComponent(entity.getCustomName());
            if (texture != null) {
                bossState.exservermod$setCustomBossTexture(texture);
            }
        }
    }
}
