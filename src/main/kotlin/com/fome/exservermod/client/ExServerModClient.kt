package com.fome.exservermod.client

import com.fome.exservermod.halloween.HalloweenEntities
import com.fome.exservermod.halloween.client.BloodZombieRenderer
import com.fome.exservermod.halloween.client.CandyEntityRenderer
import com.fome.exservermod.halloween.client.DemonEyeModel
import com.fome.exservermod.halloween.client.DemonEyeRenderer
import com.fome.exservermod.halloween.client.GhostNamingScreen
import com.fome.exservermod.halloween.client.GhostPetModel
import com.fome.exservermod.halloween.client.GhostPetRenderer
import com.fome.exservermod.halloween.client.HalloweenClientState
import com.fome.exservermod.halloween.client.HalloweenMerchantScreen
import com.fome.exservermod.halloween.entity.CandyEntity
import com.fome.exservermod.halloween.network.CandyChannelPayload
import com.fome.exservermod.halloween.network.HalloweenSkySyncPayload
import com.fome.exservermod.halloween.network.OpenGhostNamingPayload
import com.fome.exservermod.halloween.network.OpenMerchantGuiPayload
import com.fome.exservermod.network.CloseRoulettePayload
import com.fome.exservermod.network.DisguiseSyncPayload
import com.fome.exservermod.network.OpenRoulettePayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry
import net.minecraft.client.renderer.entity.ThrownItemRenderer
import net.minecraft.client.renderer.entity.WanderingTraderRenderer
import net.minecraft.world.phys.EntityHitResult

class ExServerModClient : ClientModInitializer {
    private var lastChanneledCandyId: Int? = null

    override fun onInitializeClient() {
        // Disguise & Roulette
        ClientPlayNetworking.registerGlobalReceiver(OpenRoulettePayload.TYPE) { payload, ctx ->
            ctx.client().execute {
                ctx.client().setScreenAndShow(RouletteScreen(payload))
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(CloseRoulettePayload.TYPE) { _, ctx ->
            ctx.client().execute {
                RouletteScreen.currentScreen?.forceClose()
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(DisguiseSyncPayload.TYPE) { payload, ctx ->
            ctx.client().execute {
                ClientDisguiseHolder.setDisguise(payload.playerUuid, payload.mobId, payload.isDisguised)
                val mc = ctx.client()
                mc.level?.getPlayerByUUID(payload.playerUuid)?.refreshDimensions()
                if (mc.player?.uuid == payload.playerUuid) {
                    mc.player?.refreshDimensions()
                }
            }
        }

        // Halloween Network Receivers
        ClientPlayNetworking.registerGlobalReceiver(HalloweenSkySyncPayload.TYPE) { payload, _ ->
            HalloweenClientState.isBloodSky = payload.isBloodSky
        }

        ClientPlayNetworking.registerGlobalReceiver(OpenMerchantGuiPayload.TYPE) { payload, ctx ->
            ctx.client().execute {
                val currentScreen = HalloweenMerchantScreen.currentScreen
                if (currentScreen != null) {
                    currentScreen.updatePayload(payload)
                } else {
                    ctx.client().setScreenAndShow(HalloweenMerchantScreen(payload))
                }
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(OpenGhostNamingPayload.TYPE) { _, ctx ->
            ctx.client().execute {
                ctx.client().setScreenAndShow(GhostNamingScreen())
            }
        }

        // Halloween Entity Renderers & Models
        ModelLayerRegistry.registerModelLayer(GhostPetModel.LAYER_LOCATION) { GhostPetModel.createBodyLayer() }
        ModelLayerRegistry.registerModelLayer(DemonEyeModel.LAYER_LOCATION) { DemonEyeModel.createBodyLayer() }
        EntityRendererRegistry.register(HalloweenEntities.CANDY) { ctx -> CandyEntityRenderer(ctx) }
        EntityRendererRegistry.register(HalloweenEntities.PUMPKIN_PROJECTILE) { ctx -> ThrownItemRenderer(ctx) }
        EntityRendererRegistry.register(HalloweenEntities.HALLOWEEN_MERCHANT) { ctx -> WanderingTraderRenderer(ctx) }
        EntityRendererRegistry.register(HalloweenEntities.GHOST_PET) { ctx -> GhostPetRenderer(ctx) }
        EntityRendererRegistry.register(HalloweenEntities.BLOOD_ZOMBIE) { ctx -> BloodZombieRenderer(ctx) }
        EntityRendererRegistry.register(HalloweenEntities.DEMON_EYE) { ctx -> DemonEyeRenderer(ctx) }

        // Channeling Detection (Holding Right Click on Candy)
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            val level = client.level ?: return@register

            val hit = client.hitResult as? EntityHitResult
            val hitEntity = hit?.entity
            val candy =
                if (hitEntity is CandyEntity) {
                    hitEntity
                } else {
                    level.entitiesForRendering().firstOrNull {
                        it is CandyEntity &&
                            it.position().distanceTo(
                                player.position(),
                            ) <= 3.0
                    } as? CandyEntity
                }

            val isUsing = client.options.keyUse.isDown
            if (isUsing && candy != null && candy.isAlive && candy.position().distanceTo(player.position()) <= 3.0) {
                if (lastChanneledCandyId != candy.id) {
                    lastChanneledCandyId = candy.id
                    ClientPlayNetworking.send(CandyChannelPayload(candy.id, true))
                }
            } else if (lastChanneledCandyId != null) {
                ClientPlayNetworking.send(CandyChannelPayload(lastChanneledCandyId!!, false))
                lastChanneledCandyId = null
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            ClientDisguiseHolder.clear()
            HalloweenClientState.isBloodSky = false
            lastChanneledCandyId = null
        }
    }
}
