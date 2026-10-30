package com.fome.exservermod.halloween.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class CandyChannelPayload(
    val candyEntityId: Int,
    val isHolding: Boolean,
) : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:candy_channel")
        val TYPE: CustomPacketPayload.Type<CandyChannelPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, CandyChannelPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, CandyChannelPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): CandyChannelPayload =
                    CandyChannelPayload(buf.readVarInt(), buf.readBoolean())

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: CandyChannelPayload,
                ) {
                    buf.writeVarInt(payload.candyEntityId)
                    buf.writeBoolean(payload.isHolding)
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<CandyChannelPayload> = TYPE
}
