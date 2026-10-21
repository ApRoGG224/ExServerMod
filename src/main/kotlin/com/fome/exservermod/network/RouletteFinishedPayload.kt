package com.fome.exservermod.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class RouletteFinishedPayload : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:roulette_finished")
        val TYPE: CustomPacketPayload.Type<RouletteFinishedPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, RouletteFinishedPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, RouletteFinishedPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): RouletteFinishedPayload = RouletteFinishedPayload()

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: RouletteFinishedPayload,
                ) {
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<RouletteFinishedPayload> = TYPE
}
