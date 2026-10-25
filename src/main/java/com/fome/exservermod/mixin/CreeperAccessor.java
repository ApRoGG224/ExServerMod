package com.fome.exservermod.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Creeper.class)
public interface CreeperAccessor {
    @Accessor("explosionRadius")
    void exservermod$setExplosionRadius(int radius);

    @Accessor("explosionRadius")
    int exservermod$getExplosionRadius();
}
