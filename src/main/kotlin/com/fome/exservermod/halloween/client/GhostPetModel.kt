package com.fome.exservermod.halloween.client

import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.entity.state.AllayRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth

class GhostPetModel(
    root: ModelPart,
) : EntityModel<AllayRenderState>(root, RenderTypes::entityTranslucent) {
    private val body: ModelPart = root.getChild("body")

    companion object {
        val LAYER_LOCATION: ModelLayerLocation =
            ModelLayerLocation(Identifier.parse("exservermod:ghost_pet"), "main")

        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val rootPart = mesh.root
            rootPart.addOrReplaceChild(
                "body",
                CubeListBuilder
                    .create()
                    .texOffs(0, 0)
                    .addBox(-4.0f, -8.0f, -4.0f, 8.0f, 16.0f, 8.0f, CubeDeformation.NONE),
                PartPose.offset(0.0f, 16.0f, 0.0f),
            )
            return LayerDefinition.create(mesh, 64, 64)
        }
    }

    override fun setupAnim(state: AllayRenderState) {
        super.setupAnim(state)
        val age = state.ageInTicks.toDouble()
        body.y = 16.0f + Mth.sin(age * 0.1) * 1.2f
        body.zRot = Mth.cos(age * 0.08) * 0.06f
        body.xRot = state.xRot * (Math.PI.toFloat() / 180.0f)
        body.yRot = state.yRot * (Math.PI.toFloat() / 180.0f)
    }
}
