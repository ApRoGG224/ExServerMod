package com.fome.exservermod.mixin;

import com.fome.exservermod.client.ClientDisguiseHolder;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class ClientEntityRenderDispatcherMixin {

    @Shadow
    public abstract <E extends Entity> EntityRenderState extractEntity(E entity, float partialTick);

    @Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void onExtractEntity(E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        if (entity instanceof Player player && ClientDisguiseHolder.INSTANCE.isDisguised(player.getUUID())) {
            Entity mob = ClientDisguiseHolder.INSTANCE.getDisguiseEntity(player);
            if (mob != null) {
                try {
                    ClientDisguiseHolder.INSTANCE.syncEntity(player, mob, partialTick);
                    cir.setReturnValue(this.extractEntity(mob, partialTick));
                } catch (Throwable ignored) {
                    // Fall back to standard player rendering if mob state extraction fails
                }
            }
        }
    }
}
