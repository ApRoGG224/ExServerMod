package com.fome.exservermod.halloween.client

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.ZombieRenderer
import net.minecraft.client.renderer.entity.state.ZombieRenderState
import net.minecraft.resources.Identifier

class BloodZombieRenderer(
    context: EntityRendererProvider.Context,
) : ZombieRenderer(context) {
    companion object {
        val TEXTURE: Identifier = Identifier.parse("exservermod:textures/entity/blood_zombie.png")
    }

    override fun getTextureLocation(state: ZombieRenderState): Identifier = TEXTURE
}
