package com.fome.exservermod.halloween.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class OpenMerchantGuiPayload(
    val balance: Int,
    val wares: List<String>,
    val prices: List<Int>,
    val hasGhostPet: Boolean = false,
) : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:open_merchant_gui")
        val TYPE: CustomPacketPayload.Type<OpenMerchantGuiPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, OpenMerchantGuiPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, OpenMerchantGuiPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): OpenMerchantGuiPayload {
                    val bal = buf.readVarInt()
                    val count = buf.readVarInt()
                    val waresList = (0 until count).map { buf.readUtf() }
                    val pricesList = (0 until count).map { buf.readVarInt() }
                    val hasGhostPet = if (buf.isReadable) buf.readBoolean() else false
                    return OpenMerchantGuiPayload(bal, waresList, pricesList, hasGhostPet)
                }

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: OpenMerchantGuiPayload,
                ) {
                    buf.writeVarInt(payload.balance)
                    buf.writeVarInt(payload.wares.size)
                    payload.wares.forEach { buf.writeUtf(it) }
                    payload.prices.forEach { buf.writeVarInt(it) }
                    buf.writeBoolean(payload.hasGhostPet)
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<OpenMerchantGuiPayload> = TYPE
}
