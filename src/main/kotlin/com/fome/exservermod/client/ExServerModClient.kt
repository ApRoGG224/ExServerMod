package com.fome.exservermod.client

import com.fome.exservermod.network.DisguiseSyncPayload
import com.fome.exservermod.network.OpenRoulettePayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class ExServerModClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenRoulettePayload.TYPE) { payload, ctx ->
            ctx.client().execute {
                ctx.client().setScreenAndShow(RouletteScreen(payload))
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

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            ClientDisguiseHolder.clear()
        }
    }
}
