package com.fome.exservermod.mixin;

import com.fome.exservermod.stealth.CauldronStealthUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "getRenderOffset(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void exservermod$cancelCrouchOffsetInCauldron(AvatarRenderState state, CallbackInfoReturnable<Vec3> cir) {
        if (state.isCrouching) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && CauldronStealthUtils.isInHollowBlockAt(client.level, state.x, state.y, state.z)) {
                double crouchDelta = (double) (state.scale * -2.0f) / 16.0;
                cir.setReturnValue(cir.getReturnValue().subtract(0.0, crouchDelta, 0.0));
            }
        }
    }
}
