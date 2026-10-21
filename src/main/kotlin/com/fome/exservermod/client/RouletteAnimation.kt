package com.fome.exservermod.client

object RouletteAnimation {
    fun easeOutCubic(t: Double): Double {
        val clamped = t.coerceIn(0.0, 1.0)
        val inv = 1.0 - clamped
        return 1.0 - (inv * inv * inv)
    }
}
