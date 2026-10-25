package com.fome.exservermod.waves

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class WaveDefenseGameTest {
    private val defender1: UUID = UUID.randomUUID()
    private val defender2: UUID = UUID.randomUUID()

    @Test
    fun `test initial state is idle`() {
        val game = WaveDefenseGame()
        assertFalse(game.isActive())
        assertEquals(WavePhase.IDLE, game.phase)
        assertEquals(0, game.currentWave)
        assertEquals(0, game.kills)
        assertTrue(game.defenders.isEmpty())
    }

    @Test
    fun `test start enters countdown with defenders`() {
        val game = WaveDefenseGame()
        assertTrue(game.start(listOf(defender1, defender2)))
        assertEquals(WavePhase.COUNTDOWN, game.phase)
        assertTrue(game.isActive())
        assertEquals(WaveDefenseGame.COUNTDOWN_TICKS, game.remainingTicks)
        assertEquals(setOf(defender1, defender2), game.defenders)
    }

    @Test
    fun `test start rejected when already active or without defenders`() {
        val game = WaveDefenseGame()
        assertFalse(game.start(emptyList()), "Empty defender list must be rejected")

        assertTrue(game.start(listOf(defender1)))
        assertFalse(game.start(listOf(defender2)), "Second start while active must be rejected")
    }

    @Test
    fun `test countdown leads to wave 1 fight`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))

        repeat(WaveDefenseGame.COUNTDOWN_TICKS - 1) {
            assertEquals(WaveTickResult.None, game.tick())
            assertEquals(WavePhase.COUNTDOWN, game.phase)
        }

        val result = game.tick()
        assertTrue(result is WaveTickResult.WaveStarted)
        val started = result as WaveTickResult.WaveStarted
        assertEquals(1, started.wave)
        assertEquals(WaveDefenseGame.TOTAL_WAVES, started.totalWaves)
        assertEquals(WavePhase.FIGHT, game.phase)
        assertEquals(15, WaveDefenseGame.totalMobsInWave(started.groups), "Wave 1 must have 15 mobs")
    }

    @Test
    fun `test wave cleared mid gauntlet moves to intermission`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))

        // Skip countdown
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        assertEquals(WavePhase.FIGHT, game.phase)
        assertEquals(1, game.currentWave)

        val cleared = game.onWaveCleared()
        assertTrue(cleared is WaveTickResult.PhaseChanged)
        cleared as WaveTickResult.PhaseChanged
        assertEquals(WavePhase.INTERMISSION, cleared.newPhase)
        assertEquals(WaveDefenseGame.INTERMISSION_TICKS, game.remainingTicks)

        // During intermission kills keep accumulating
        game.registerKill()
        game.registerKill()
        assertEquals(2, game.kills)
    }

    @Test
    fun `test intermission leads to next wave`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        game.onWaveCleared()

        repeat(WaveDefenseGame.INTERMISSION_TICKS - 1) {
            assertEquals(WaveTickResult.None, game.tick())
        }

        val result = game.tick()
        assertTrue(result is WaveTickResult.WaveStarted)
        assertEquals(2, (result as WaveTickResult.WaveStarted).wave)
        assertEquals(WavePhase.FIGHT, game.phase)
    }

    @Test
    fun `test full gauntlet reaches victory`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1, defender2))

        // Run through all waves: countdown + (fight-clear + intermission) x waves
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        for (wave in 1 until WaveDefenseGame.TOTAL_WAVES) {
            assertEquals(wave, game.currentWave)
            game.onWaveCleared()
            repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }
        }
        assertEquals(WaveDefenseGame.TOTAL_WAVES, game.currentWave)

        // Final wave cleared -> victory
        val cleared = game.onWaveCleared()
        assertTrue(cleared is WaveTickResult.Victory, "Clearing the final wave must trigger victory")
        assertEquals(WavePhase.VICTORY, game.phase)

        // Victory window then resets to idle
        repeat(WaveDefenseGame.VICTORY_TICKS - 1) {
            assertEquals(WaveTickResult.None, game.tick())
        }
        val idle = game.tick()
        assertTrue(idle is WaveTickResult.PhaseChanged)
        idle as WaveTickResult.PhaseChanged
        assertEquals(WavePhase.IDLE, idle.newPhase)
        assertFalse(game.isActive())
    }

    @Test
    fun `test victory is idempotent`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        for (wave in 1 until WaveDefenseGame.TOTAL_WAVES) {
            game.onWaveCleared()
            repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }
        }

        assertTrue(game.onWaveCleared() is WaveTickResult.Victory)
        assertEquals(WaveTickResult.None, game.onWaveCleared(), "Repeated victory call must be a no-op")
    }

    @Test
    fun `test wave cleared ignored outside fight phase`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))

        assertEquals(WaveTickResult.None, game.onWaveCleared())
        assertEquals(WavePhase.COUNTDOWN, game.phase)
    }

    @Test
    fun `test stop resets everything`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        game.registerKill()

        game.stop()
        assertEquals(WavePhase.IDLE, game.phase)
        assertFalse(game.isActive())
        assertEquals(0, game.kills)
        assertEquals(0, game.currentWave)
        assertTrue(game.defenders.isEmpty())
    }

    @Test
    fun `test all defenders offline detection`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1, defender2))

        assertFalse(game.areAllDefendersOffline(setOf(defender1)))
        assertFalse(game.areAllDefendersOffline(setOf(defender1, defender2, UUID.randomUUID())))
        assertTrue(game.areAllDefendersOffline(setOf(UUID.randomUUID())))
        assertTrue(game.areAllDefendersOffline(emptySet()))
    }

    @Test
    fun `test wave composition is vanilla and zero grief`() {
        val allowed =
            setOf(
                "minecraft:zombie",
                "minecraft:skeleton",
                "minecraft:spider",
                "minecraft:cave_spider",
                "minecraft:creeper",
                "minecraft:witch",
                "minecraft:drowned",
                "minecraft:vex",
                "minecraft:blaze",
                "minecraft:wither_skeleton",
                "minecraft:stray",
                "minecraft:warden",
                "minecraft:bogged",
                "minecraft:husk",
                "minecraft:vindicator",
                "minecraft:evoker",
                "minecraft:hoglin",
                "minecraft:piglin_brute",
            )
        val forbidden = setOf("minecraft:wither", "minecraft:ghast", "minecraft:ravager", "minecraft:ender_dragon")

        for (wave in 1..WaveDefenseGame.TOTAL_WAVES) {
            val groups = WaveDefenseGame.buildWave(wave)
            assertTrue(groups.isNotEmpty(), "Wave $wave must not be empty")
            for (group in groups) {
                assertTrue(group.mobId in allowed, "Mob ${group.mobId} must stay on the vanilla whitelist")
                assertFalse(group.mobId in forbidden, "Mob ${group.mobId} is destructive and must never be used")
                assertTrue(group.count > 0, "Wave $wave groups must have positive counts")
            }
        }
    }

    @Test
    fun `test boss waves include champions and bosses`() {
        assertTrue(WaveDefenseGame.isBossWave(5))
        assertTrue(WaveDefenseGame.isBossWave(8))
        assertTrue(WaveDefenseGame.isBossWave(WaveDefenseGame.TOTAL_WAVES))
        assertFalse(WaveDefenseGame.isBossWave(1))
        assertFalse(WaveDefenseGame.isBossWave(6))

        val wave5 = WaveDefenseGame.buildWave(5)
        assertTrue(wave5.any { it.role == WaveMobRole.BLAZE_CHAMPION }, "Wave 5 must include Blaze Champion")

        val wave8 = WaveDefenseGame.buildWave(8)
        assertTrue(wave8.any { it.role == WaveMobRole.WITCH_CHAMPION }, "Wave 8 must include Witch Champion")

        val finalWave = WaveDefenseGame.buildWave(WaveDefenseGame.TOTAL_WAVES)
        assertTrue(finalWave.any { it.role == WaveMobRole.BLAZE_CHAMPION }, "Wave 11 must include Blaze Champion")
        assertTrue(finalWave.any { it.role == WaveMobRole.WITCH_CHAMPION }, "Wave 11 must include Witch Champion")
        assertTrue(finalWave.any { it.role == WaveMobRole.CHAMPION }, "Wave 11 must include Dread Champion")
    }

    @Test
    fun `test unknown wave composition falls back to victory`() {
        assertTrue(WaveDefenseGame.buildWave(0).isEmpty())
        assertTrue(WaveDefenseGame.buildWave(WaveDefenseGame.TOTAL_WAVES + 1).isEmpty())
    }
}
