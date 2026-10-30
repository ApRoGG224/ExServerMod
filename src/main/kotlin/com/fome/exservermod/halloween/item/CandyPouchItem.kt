package com.fome.exservermod.halloween.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class CandyPouchItem(
    val tier: Int,
    val channelDurationTicks: Int,
    val candyReward: Int,
    properties: Properties,
) : Item(properties) {
    fun appendHoverText(
        stack: ItemStack,
        tooltipComponents: MutableList<Component>,
    ) {
        val seconds = channelDurationTicks.toDouble() / 20.0
        tooltipComponents.add(
            Component
                .translatable(
                    "exservermod.item.candy_pouch.desc",
                    "%.1f".format(seconds),
                    candyReward,
                ).withStyle(ChatFormatting.GOLD),
        )
    }

    companion object {
        const val TIER_1_TICKS = 80 // 4.0s
        const val TIER_1_YIELD = 30
        const val TIER_2_TICKS = 70 // 3.5s
        const val TIER_2_YIELD = 45
        const val TIER_3_TICKS = 20 // 1.0s
        const val TIER_3_YIELD = 80
    }
}
