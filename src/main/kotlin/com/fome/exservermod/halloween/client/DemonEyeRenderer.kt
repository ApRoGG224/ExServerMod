package com.fome.exservermod.halloween.client

import com.fome.exservermod.halloween.entity.DemonEyeEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.Identifier

class DemonEyeRenderer(
    context: EntityRendererProvider.Context,
) : MobRenderer<DemonEyeEntity, DemonEyeRenderState, DemonEyeModel>(
        context,
        DemonEyeModel(context.bakeLayer(DemonEyeModel.LAYER_LOCATION)),
        0.4f,
    ) {
    companion object {
        val TEXTURE_NORMAL: Identifier = Identifier.parse("exservermod:textures/entity/demon_eye_normal.png")
        val TEXTURE_CATARACT: Identifier = Identifier.parse("exservermod:textures/entity/demon_eye_cataract.png")
        val TEXTURE_DILATED: Identifier = Identifier.parse("exservermod:textures/entity/demon_eye_dilated.png")
        val TEXTURE_BLOOD: Identifier = Identifier.parse("exservermod:textures/entity/demon_eye_blood.png")
    }

    override fun createRenderState(): DemonEyeRenderState = DemonEyeRenderState()

    override fun extractRenderState(
        entity: DemonEyeEntity,
        state: DemonEyeRenderState,
        partialTick: Float,
    ) {
        super.extractRenderState(entity, state, partialTick)
        state.variant = entity.getVariant()
    }

    override fun getTextureLocation(state: DemonEyeRenderState): Identifier =
        when (state.variant) {
            1 -> TEXTURE_CATARACT
            2 -> TEXTURE_DILATED
            3 -> TEXTURE_BLOOD
            else -> TEXTURE_NORMAL
        }
}
