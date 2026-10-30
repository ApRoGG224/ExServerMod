package com.fome.exservermod.halloween.client

import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.state.AllayRenderState
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.animal.allay.Allay

class GhostPetRenderer(
    context: EntityRendererProvider.Context,
) : MobRenderer<Allay, AllayRenderState, GhostPetModel>(
        context,
        GhostPetModel(context.bakeLayer(GhostPetModel.LAYER_LOCATION)),
        0.3f,
    ) {
    companion object {
        val TEXTURE: Identifier = Identifier.parse("exservermod:textures/entity/ghost_pet.png")
    }

    override fun getTextureLocation(state: AllayRenderState): Identifier = TEXTURE

    override fun createRenderState(): AllayRenderState = AllayRenderState()

    override fun getBlockLightLevel(
        entity: Allay,
        pos: BlockPos,
    ): Int = 15
}
