package com.fome.exservermod.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class RouletteCanceledPayload : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:roulette_canceled")
        val TYPE: CustomPacketPayload.Type<RouletteCanceledPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, RouletteCanceledPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, RouletteCanceledPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): RouletteCanceledPayload = RouletteCanceledPayload()

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: RouletteCanceledPayload,
                ) {
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<RouletteCanceledPayload> = TYPE
}
