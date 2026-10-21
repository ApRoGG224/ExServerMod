package com.fome.exservermod

import com.fome.exservermod.client.RouletteAnimation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RouletteTest {
    @Test
    fun testEaseOutCubicBoundaries() {
        assertEquals(0.0, RouletteAnimation.easeOutCubic(0.0), 1e-6)
        assertEquals(1.0, RouletteAnimation.easeOutCubic(1.0), 1e-6)
        assertEquals(0.0, RouletteAnimation.easeOutCubic(-0.5), 1e-6)
        assertEquals(1.0, RouletteAnimation.easeOutCubic(1.5), 1e-6)
    }

    @Test
    fun testEaseOutCubicDeceleration() {
        val mid = RouletteAnimation.easeOutCubic(0.5)
        // easeOutCubic at 0.5 is 1 - 0.5^3 = 0.875
        assertEquals(0.875, mid, 1e-6)
        assertTrue(mid > 0.5, "Ease out curve must advance more than 50% at half time")
    }

    @Test
    fun testMonotonicallyIncreasing() {
        var prev = 0.0
        for (i in 1..100) {
            val t = i / 100.0
            val current = RouletteAnimation.easeOutCubic(t)
            assertTrue(current >= prev, "Curve must be monotonically increasing")
            prev = current
        }
    }
}
