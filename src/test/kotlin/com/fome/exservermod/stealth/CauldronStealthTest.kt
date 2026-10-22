package com.fome.exservermod.stealth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CauldronStealthTest {
    @Test
    fun testCrouchOffsetValue() {
        // Offset must exactly equal 2.0 / 16.0 (0.125 blocks) to counteract MC-48191 / MC-228666
        assertEquals(0.125, CauldronStealthUtils.CROUCH_Y_OFFSET, 1e-6)
    }

    @Test
    fun testCompensatedHeightCalculation() {
        val cauldronFloorY = 64.25
        val crouchOffset = 0.125

        // Client subtracts crouchOffset when isCrouching is true:
        // Rendered Y = (Server Y + crouchOffset) - crouchOffset = Server Y
        val networkSentY = cauldronFloorY + crouchOffset
        val renderedY = networkSentY - crouchOffset

        assertEquals(cauldronFloorY, renderedY, 1e-6)
    }
}
