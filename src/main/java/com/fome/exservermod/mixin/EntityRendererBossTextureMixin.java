package com.fome.exservermod.mixin;

import com.fome.exservermod.client.BossCustomTextureState;
import com.fome.exservermod.client.BossCustomTextures;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererBossTextureMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void exservermod$extractBossCustomTexture(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
        if (state instanceof BossCustomTextureState bossState && entity.hasCustomName()) {
            Component customName = entity.getCustomName();
            if (customName != null) {
                Identifier texture = BossCustomTextures.INSTANCE.getBossTexture(customName.getString());
                if (texture != null) {
                    bossState.exservermod$setCustomBossTexture(texture);
                }
            }
        }
    }
}
