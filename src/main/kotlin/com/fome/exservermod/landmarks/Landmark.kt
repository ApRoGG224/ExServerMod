package com.fome.exservermod.landmarks

import kotlin.math.hypot
import kotlin.math.max

data class Landmark(
    val name: String,
    val x: Double,
    val z: Double,
    val radius: Double = DEFAULT_RADIUS,
    val dimension: String = DEFAULT_DIMENSION,
) {
    companion object {
        const val DEFAULT_RADIUS = 50.0
        const val DEFAULT_DIMENSION = "minecraft:overworld"
    }

    fun distanceToPerimeter(
        targetX: Double,
        targetZ: Double,
    ): Double {
        val dx = targetX - x
        val dz = targetZ - z
        val centerDistance = hypot(dx, dz)
        return max(0.0, centerDistance - radius)
    }
}
