package com.fome.exservermod.mixin;

import com.fome.exservermod.client.BossCustomTextureState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public class EntityRenderStateMixin implements BossCustomTextureState {
    @Unique
    private Identifier exservermod$bossTexture;

    @Override
    public Identifier exservermod$getCustomBossTexture() {
        return this.exservermod$bossTexture;
    }

    @Override
    public void exservermod$setCustomBossTexture(Identifier texture) {
        this.exservermod$bossTexture = texture;
    }
}
