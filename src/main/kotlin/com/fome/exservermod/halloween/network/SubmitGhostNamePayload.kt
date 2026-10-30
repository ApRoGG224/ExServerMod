package com.fome.exservermod.halloween.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class SubmitGhostNamePayload(
    val petName: String,
) : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:submit_ghost_name")
        val TYPE: CustomPacketPayload.Type<SubmitGhostNamePayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SubmitGhostNamePayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, SubmitGhostNamePayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): SubmitGhostNamePayload = SubmitGhostNamePayload(buf.readUtf(32))

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: SubmitGhostNamePayload,
                ) {
                    buf.writeUtf(payload.petName.take(32))
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<SubmitGhostNamePayload> = TYPE
}
