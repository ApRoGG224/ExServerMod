package com.fome.exservermod.halloween.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class HalloweenSkySyncPayload(
    val isBloodSky: Boolean,
) : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:halloween_sky_sync")
        val TYPE: CustomPacketPayload.Type<HalloweenSkySyncPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, HalloweenSkySyncPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, HalloweenSkySyncPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): HalloweenSkySyncPayload = HalloweenSkySyncPayload(buf.readBoolean())

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: HalloweenSkySyncPayload,
                ) {
                    buf.writeBoolean(payload.isBloodSky)
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<HalloweenSkySyncPayload> = TYPE
}
