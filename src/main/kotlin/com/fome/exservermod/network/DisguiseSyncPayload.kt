package com.fome.exservermod.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import java.util.UUID

data class DisguiseSyncPayload(
    val playerUuid: UUID,
    val mobId: String,
    val isDisguised: Boolean,
) : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:disguise_sync")
        val TYPE: CustomPacketPayload.Type<DisguiseSyncPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DisguiseSyncPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, DisguiseSyncPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): DisguiseSyncPayload {
                    val uuid = buf.readUUID()
                    val mobId = buf.readUtf()
                    val isDisguised = buf.readBoolean()
                    return DisguiseSyncPayload(uuid, mobId, isDisguised)
                }

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: DisguiseSyncPayload,
                ) {
                    buf.writeUUID(payload.playerUuid)
                    buf.writeUtf(payload.mobId)
                    buf.writeBoolean(payload.isDisguised)
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<DisguiseSyncPayload> = TYPE
}
