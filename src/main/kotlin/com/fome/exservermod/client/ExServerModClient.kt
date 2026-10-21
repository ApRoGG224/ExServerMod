package com.fome.exservermod.client

import com.fome.exservermod.network.OpenRoulettePayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class ExServerModClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(OpenRoulettePayload.TYPE) { payload, ctx ->
            ctx.client().execute {
                ctx.client().setScreenAndShow(RouletteScreen(payload))
            }
        }
    }
}
