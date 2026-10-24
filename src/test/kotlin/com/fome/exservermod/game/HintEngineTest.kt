package com.fome.exservermod.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HintEngineTest {
    @Test
    fun `test first hint is always distance`() {
        repeat(20) {
            val engine = HintEngine()
            assertEquals(HintEngine.SeekerHintType.DISTANCE, engine.nextHint().type)
        }
    }

    @Test
    fun `test distance shows up every other hint`() {
        val engine = HintEngine()
        val sequence = (1..40).map { engine.nextHint().type }

        // Distance on odd positions (1st, 3rd, 5th...), varied on even ones
        sequence.forEachIndexed { index, type ->
            val expected = if (index % 2 == 0) HintEngine.SeekerHintType.DISTANCE else HintEngine.SeekerHintType.VARIED
            assertEquals(expected, type, "Hint at position $index must alternate distance/varied")
        }
    }

    @Test
    fun `test varied hints do not repeat until bag exhausted`() {
        val engine = HintEngine()
        val variedTypes = HintEngine.VariedHint.entries.size

        // Collect a full bag worth of varied hints (skipping distance slots)
        val variedSeen = mutableListOf<Int>()
        while (variedSeen.size < variedTypes) {
            val hint = engine.nextHint()
            if (hint.type == HintEngine.SeekerHintType.VARIED) {
                variedSeen.add(hint.variedOrdinal)
            }
        }

        // No repeats while the bag had fresh entries
        assertEquals(variedTypes, variedSeen.distinct().size, "Every varied hint must be unique until exhausted")
        assertTrue(variedSeen.all { it in 0 until variedTypes })
    }

    @Test
    fun `test bag refills indefinitely without immediate repetition`() {
        val engine = HintEngine()

        var previousVaried = -1
        var variedCount = 0
        while (variedCount < HintEngine.VariedHint.entries.size * 3) {
            val hint = engine.nextHint()
            if (hint.type == HintEngine.SeekerHintType.VARIED) {
                assertTrue(hint.variedOrdinal != previousVaried, "Consecutive varied hints must differ, even across bag refills")
                previousVaried = hint.variedOrdinal
                variedCount++
            }
        }
    }

    @Test
    fun `test reset restores initial state`() {
        val engine = HintEngine()
        engine.nextHint()
        engine.nextHint()

        engine.reset()
        assertEquals(HintEngine.SeekerHintType.DISTANCE, engine.nextHint().type, "After reset the first hint must be distance again")
    }
}
