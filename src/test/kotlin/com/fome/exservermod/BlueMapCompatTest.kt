package com.fome.exservermod

import com.fome.exservermod.compat.bluemap.BlueMapCompat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.UUID

class BlueMapCompatTest {
    @Test
    fun testSafeFallbackWhenBlueMapAbsent() {
        assertFalse(BlueMapCompat.isModLoaded, "BlueMap should not be loaded in unit tests")
        assertDoesNotThrow {
            BlueMapCompat.setPlayerVisibility(UUID.randomUUID(), false)
            BlueMapCompat.setPlayerVisibility(UUID.randomUUID(), true)
            BlueMapCompat.setPlayersVisibility(listOf(UUID.randomUUID(), UUID.randomUUID()), false)
            BlueMapCompat.setPlayersVisibility(listOf(UUID.randomUUID(), UUID.randomUUID()), true)
        }
    }
}
