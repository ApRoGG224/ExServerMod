package com.fome.exservermod.halloween.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class OpenMerchantGuiPayload(
    val balance: Int,
    val wares: List<String>,
    val prices: List<Int>,
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
                    return OpenMerchantGuiPayload(bal, waresList, pricesList)
                }

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: OpenMerchantGuiPayload,
                ) {
                    buf.writeVarInt(payload.balance)
                    buf.writeVarInt(payload.wares.size)
                    payload.wares.forEach { buf.writeUtf(it) }
                    payload.prices.forEach { buf.writeVarInt(it) }
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<OpenMerchantGuiPayload> = TYPE
}
