package com.fome.exservermod.halloween.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

class OpenGhostNamingPayload : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:open_ghost_naming")
        val TYPE: CustomPacketPayload.Type<OpenGhostNamingPayload> = CustomPacketPayload.Type(ID)

        val INSTANCE: OpenGhostNamingPayload = OpenGhostNamingPayload()

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, OpenGhostNamingPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, OpenGhostNamingPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): OpenGhostNamingPayload = INSTANCE

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: OpenGhostNamingPayload,
                ) {
                    // No fields needed
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<OpenGhostNamingPayload> = TYPE
}
