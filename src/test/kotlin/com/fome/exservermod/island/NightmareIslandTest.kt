package com.fome.exservermod.island

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NightmareIslandTest {
    @Test
    fun `test island coordinates boundaries`() {
        // Center of the island
        assertTrue(
            NightmareIslandManager.isLocationOnIsland(
                NightmareIslandManager.ISLAND_CENTER_X,
                NightmareIslandManager.ISLAND_CENTER_Z,
            ),
        )

        // Corners of the 9x9 chunk boundary
        assertTrue(NightmareIslandManager.isLocationOnIsland(NightmareIslandManager.ISLAND_MIN_X, NightmareIslandManager.ISLAND_MIN_Z))
        assertTrue(NightmareIslandManager.isLocationOnIsland(NightmareIslandManager.ISLAND_MAX_X, NightmareIslandManager.ISLAND_MAX_Z))
        assertTrue(NightmareIslandManager.isLocationOnIsland(NightmareIslandManager.ISLAND_MIN_X, NightmareIslandManager.ISLAND_MAX_Z))
        assertTrue(NightmareIslandManager.isLocationOnIsland(NightmareIslandManager.ISLAND_MAX_X, NightmareIslandManager.ISLAND_MIN_Z))

        // Outside coordinates
        assertFalse(NightmareIslandManager.isLocationOnIsland(0.0, 0.0))
        assertFalse(NightmareIslandManager.isLocationOnIsland(NightmareIslandManager.ISLAND_MIN_X - 1.0, 3080.0))
        assertFalse(NightmareIslandManager.isLocationOnIsland(NightmareIslandManager.ISLAND_MAX_X + 1.0, 3080.0))
        assertFalse(NightmareIslandManager.isLocationOnIsland(2250.0, NightmareIslandManager.ISLAND_MIN_Z - 1.0))
        assertFalse(NightmareIslandManager.isLocationOnIsland(2250.0, NightmareIslandManager.ISLAND_MAX_Z + 1.0))
    }
}
