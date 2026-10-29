package com.fome.exservermod.landmarks

import net.minecraft.network.chat.Component

object DimensionFormatter {
    fun format(dimension: Any?): Component {
        val idStr = dimension?.toString() ?: return Component.empty()
        val clean = idStr.removePrefix("minecraft:")
        val key =
            when (clean) {
                "overworld" -> "exservermod.dimension.overworld"
                "the_nether" -> "exservermod.dimension.the_nether"
                "the_end" -> "exservermod.dimension.the_end"
                else -> return Component.literal(idStr)
            }
        return Component.translatable(key)
    }
}
