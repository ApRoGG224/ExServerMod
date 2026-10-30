package com.fome.exservermod.halloween.item

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import java.util.function.Consumer

class CandyPouchItem(
    val tier: Int,
    val channelDurationTicks: Int,
    val candyReward: Int,
    properties: Properties,
) : Item(properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        tooltipOutput: Consumer<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        val seconds = channelDurationTicks.toDouble() / 20.0
        tooltipOutput.accept(
            Component
                .translatable(
                    "item.exservermod.candy_pouch.desc",
                    "%.1f".format(seconds),
                    candyReward,
                ).withStyle(ChatFormatting.GOLD),
        )
        tooltipOutput.accept(
            Component
                .translatable("item.exservermod.candy_pouch.lore")
                .withStyle(ChatFormatting.DARK_PURPLE),
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
