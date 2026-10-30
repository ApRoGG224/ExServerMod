package com.fome.exservermod.halloween.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

data class MerchantBuyPayload(
    val itemKey: String,
) : CustomPacketPayload {
    companion object {
        val ID: Identifier = Identifier.parse("exservermod:merchant_buy")
        val TYPE: CustomPacketPayload.Type<MerchantBuyPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, MerchantBuyPayload> =
            object : StreamCodec<RegistryFriendlyByteBuf, MerchantBuyPayload> {
                override fun decode(buf: RegistryFriendlyByteBuf): MerchantBuyPayload = MerchantBuyPayload(buf.readUtf())

                override fun encode(
                    buf: RegistryFriendlyByteBuf,
                    payload: MerchantBuyPayload,
                ) {
                    buf.writeUtf(payload.itemKey)
                }
            }
    }

    override fun type(): CustomPacketPayload.Type<MerchantBuyPayload> = TYPE
}
