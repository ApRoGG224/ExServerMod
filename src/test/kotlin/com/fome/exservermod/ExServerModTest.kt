package com.fome.exservermod

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExServerModTest {
    @Test
    fun testModIdFormat() {
        assertEquals("exservermod", ExServerMod.MOD_ID)
    }
}
