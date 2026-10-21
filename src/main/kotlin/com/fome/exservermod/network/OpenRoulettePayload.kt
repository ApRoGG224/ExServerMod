package com.fome.exservermod.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class OpenRoulettePayload(
    val targetMobId: String,
    val mobCandidates: List<String>,
) : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:open_roulette")
        val TYPE: CustomPacketPayload.Type<OpenRoulettePayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, OpenRoulettePayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, OpenRoulettePayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): OpenRoulettePayload {
                    val target = buf.readUtf()
                    val count = buf.readVarInt()
                    val candidates = (0 until count).map { buf.readUtf() }
                    return OpenRoulettePayload(target, candidates)
                }

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: OpenRoulettePayload,
                ) {
                    buf.writeUtf(payload.targetMobId)
                    buf.writeVarInt(payload.mobCandidates.size)
                    payload.mobCandidates.forEach { buf.writeUtf(it) }
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<OpenRoulettePayload> = TYPE
}
