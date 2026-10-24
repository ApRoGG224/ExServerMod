package com.fome.exservermod.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class CloseRoulettePayload : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:close_roulette")
        val TYPE: CustomPacketPayload.Type<CloseRoulettePayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, CloseRoulettePayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, CloseRoulettePayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): CloseRoulettePayload = CloseRoulettePayload()

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: CloseRoulettePayload,
                ) {
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<CloseRoulettePayload> = TYPE
}
