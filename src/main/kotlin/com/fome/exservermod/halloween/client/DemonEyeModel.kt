package com.fome.exservermod.halloween.client

import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth

class DemonEyeModel(
    root: ModelPart,
) : EntityModel<DemonEyeRenderState>(root, RenderTypes::entityCutout) {
    private val body: ModelPart = root.getChild("body")
    private val tendril1: ModelPart = body.getChild("tendril1")
    private val tendril2: ModelPart = body.getChild("tendril2")
    private val tendril3: ModelPart = body.getChild("tendril3")
    private val tendril4: ModelPart = body.getChild("tendril4")

    companion object {
        val LAYER_LOCATION: ModelLayerLocation =
            ModelLayerLocation(Identifier.parse("exservermod:demon_eye"), "main")

        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val rootPart = mesh.root
            val bodyPart =
                rootPart.addOrReplaceChild(
                    "body",
                    CubeListBuilder
                        .create()
                        .texOffs(0, 0)
                        .addBox(-4.0f, -4.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation.NONE),
                    PartPose.offset(0.0f, 16.0f, 0.0f),
                )

            bodyPart.addOrReplaceChild(
                "tendril1",
                CubeListBuilder
                    .create()
                    .texOffs(32, 0)
                    .addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, CubeDeformation.NONE),
                PartPose.offset(-2.0f, -2.0f, 4.0f),
            )

            bodyPart.addOrReplaceChild(
                "tendril2",
                CubeListBuilder
                    .create()
                    .texOffs(32, 8)
                    .addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, CubeDeformation.NONE),
                PartPose.offset(2.0f, -2.0f, 4.0f),
            )

            bodyPart.addOrReplaceChild(
                "tendril3",
                CubeListBuilder
                    .create()
                    .texOffs(48, 0)
                    .addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, CubeDeformation.NONE),
                PartPose.offset(-2.0f, 2.0f, 4.0f),
            )

            bodyPart.addOrReplaceChild(
                "tendril4",
                CubeListBuilder
                    .create()
                    .texOffs(48, 8)
                    .addBox(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 6.0f, CubeDeformation.NONE),
                PartPose.offset(2.0f, 2.0f, 4.0f),
            )

            return LayerDefinition.create(mesh, 64, 32)
        }
    }

    override fun setupAnim(state: DemonEyeRenderState) {
        super.setupAnim(state)
        val age = state.ageInTicks.toDouble()
        body.xRot = state.xRot * (Math.PI.toFloat() / 180.0f)
        body.yRot = state.yRot * (Math.PI.toFloat() / 180.0f)

        tendril1.xRot = Mth.sin(age * 0.2) * 0.25f
        tendril1.yRot = Mth.cos(age * 0.2) * 0.25f

        tendril2.xRot = Mth.cos(age * 0.2) * 0.25f
        tendril2.yRot = -Mth.sin(age * 0.2) * 0.25f

        tendril3.xRot = -Mth.sin(age * 0.2 + 1.0) * 0.25f
        tendril3.yRot = Mth.cos(age * 0.2 + 1.0) * 0.25f

        tendril4.xRot = -Mth.cos(age * 0.2 + 1.0) * 0.25f
        tendril4.yRot = -Mth.sin(age * 0.2 + 1.0) * 0.25f
    }
}
