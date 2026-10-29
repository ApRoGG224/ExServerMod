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

        // Wave 1 clear triggers Trick or Treat
        val clearedWave1 = game.onWaveCleared()
        assertTrue(clearedWave1 is WaveTickResult.PhaseChanged)
        assertEquals(WavePhase.TRICK_OR_TREAT, (clearedWave1 as WaveTickResult.PhaseChanged).newPhase)
        assertEquals(WaveDefenseGame.TRICK_OR_TREAT_TICKS, game.remainingTicks)

        // Choosing treat moves to intermission
        assertTrue(game.chooseTreat(null))
        assertEquals(WavePhase.INTERMISSION, game.phase)
        assertEquals(WaveDefenseGame.INTERMISSION_TICKS, game.remainingTicks)

        // During intermission kills keep accumulating
        game.registerKill()
        game.registerKill()
        assertEquals(2, game.kills)

        // Start wave 2
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }
        assertEquals(2, game.currentWave)

        // Wave 2 clear moves directly to intermission
        val clearedWave2 = game.onWaveCleared()
        assertTrue(clearedWave2 is WaveTickResult.PhaseChanged)
        assertEquals(WavePhase.INTERMISSION, (clearedWave2 as WaveTickResult.PhaseChanged).newPhase)
    }

    @Test
    fun `test intermission leads to next wave`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        game.onWaveCleared()
        game.chooseTreat(null)

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

        // Countdown -> wave 1
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        assertEquals(1, game.currentWave)

        // Wave 1 cleared -> Trick or Treat -> choose Treat
        game.onWaveCleared()
        game.chooseTreat(null)
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }

        // Waves 2 through 10
        for (wave in 2 until WaveDefenseGame.TOTAL_WAVES) {
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

        // Wave 1 clear -> choice -> intermission
        game.onWaveCleared()
        game.chooseTreat(null)
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }

        for (wave in 2 until WaveDefenseGame.TOTAL_WAVES) {
            game.onWaveCleared()
            repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }
        }

        assertTrue(game.onWaveCleared() is WaveTickResult.Victory)
        assertEquals(WaveTickResult.None, game.onWaveCleared(), "Repeated victory call must be a no-op")
    }

    @Test
    fun `test trick choice replaces wave 2 with wave 6 and skips to wave 5 upon clear`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))

        // Countdown -> wave 1
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        assertEquals(1, game.currentWave)

        // Wave 1 cleared -> Trick or Treat
        assertEquals(WavePhase.TRICK_OR_TREAT, (game.onWaveCleared() as WaveTickResult.PhaseChanged).newPhase)

        // Defender chooses Trick
        assertTrue(game.chooseTrick())
        assertTrue(game.isTrickWave)
        assertEquals(HalloweenChoice.TRICK, game.halloweenChoice)
        assertEquals(WavePhase.INTERMISSION, game.phase)

        // Intermission leads to Wave 2 (Trick Wave)
        repeat(WaveDefenseGame.INTERMISSION_TICKS - 1) { game.tick() }
        val waveStarted = game.tick()
        assertTrue(waveStarted is WaveTickResult.WaveStarted)
        waveStarted as WaveTickResult.WaveStarted
        assertEquals(2, waveStarted.wave)
        assertTrue(waveStarted.isTrick)
        assertEquals(WaveDefenseGame.buildWave(6), waveStarted.groups, "Trick wave 2 must spawn wave 6 composition")

        // Clearing Trick Wave 2 skips waves 3 and 4, jumping to wave 5
        val clearResult = game.onWaveCleared()
        assertTrue(clearResult is WaveTickResult.TrickWaveCleared)
        assertEquals(5, (clearResult as WaveTickResult.TrickWaveCleared).skippedToWave)
        assertEquals(4, game.currentWave)
        assertFalse(game.isTrickWave)
        assertTrue(game.hasSecondChanceToken, "Clearing trick wave 2 must grant second chance token")
        assertEquals(WavePhase.INTERMISSION, game.phase)

        // After intermission, wave 5 begins!
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }
        assertEquals(5, game.currentWave)
        assertEquals(WavePhase.FIGHT, game.phase)
    }

    @Test
    fun `test trick second chance token triggers 60s recovery and restarts same wave`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))

        // Wave 1 -> Trick
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        game.onWaveCleared()
        game.chooseTrick()

        // Beat Trick Wave 2 -> skips to wave 5 with Second Chance token
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }
        assertEquals(2, game.currentWave)
        game.onWaveCleared()
        assertTrue(game.hasSecondChanceToken)

        // Advance through waves 5, 6, 7 to reach wave 8
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() } // starts wave 5
        assertEquals(5, game.currentWave)
        game.onWaveCleared()
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() } // starts wave 6
        assertEquals(6, game.currentWave)
        game.onWaveCleared()
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() } // starts wave 7
        assertEquals(7, game.currentWave)
        game.onWaveCleared()
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() } // starts wave 8
        assertEquals(8, game.currentWave)
        assertEquals(WavePhase.FIGHT, game.phase)

        // Defender dies on wave 8 -> second chance triggers 60s recovery
        assertTrue(game.triggerSecondChanceRecovery())
        assertFalse(game.hasSecondChanceToken, "Second chance token must be consumed")
        assertEquals(WavePhase.RECOVERY, game.phase)
        assertEquals(WaveDefenseGame.RECOVERY_TICKS, game.remainingTicks)

        // Ticking through 60s recovery restarts wave 8!
        repeat(WaveDefenseGame.RECOVERY_TICKS - 1) {
            assertEquals(WaveTickResult.None, game.tick())
        }
        val restarted = game.tick()
        assertTrue(restarted is WaveTickResult.WaveStarted)
        assertEquals(8, (restarted as WaveTickResult.WaveStarted).wave)
        assertEquals(8, game.currentWave)
        assertEquals(WavePhase.FIGHT, game.phase)

        // A subsequent death has no second chance token left
        assertFalse(game.triggerSecondChanceRecovery())
    }

    @Test
    fun `test treat choice fresh sets checkpoint flag and proceeds to wave 2`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        game.onWaveCleared()

        // Fresh treat without existing checkpoint
        assertTrue(game.chooseTreat(null))
        assertTrue(game.willSaveCheckpointOnDeath)
        assertEquals(HalloweenChoice.TREAT, game.halloweenChoice)
        assertFalse(game.isTrickWave)

        // Next wave is standard wave 2
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }
        assertEquals(2, game.currentWave)
    }

    @Test
    fun `test treat choice with checkpoint resumes directly at checkpoint wave without re-saving`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        game.onWaveCleared()

        // Resume at wave 8
        assertTrue(game.chooseTreat(8))
        assertFalse(game.willSaveCheckpointOnDeath, "Resumed run must NOT allow re-saving checkpoint")
        assertEquals(7, game.currentWave)

        // After intermission, wave 8 starts directly
        repeat(WaveDefenseGame.INTERMISSION_TICKS - 1) { game.tick() }
        val wave8 = game.tick()
        assertTrue(wave8 is WaveTickResult.WaveStarted)
        assertEquals(8, (wave8 as WaveTickResult.WaveStarted).wave)
        assertEquals(8, game.currentWave)
    }

    @Test
    fun `test trick or treat timeout defaults to normal wave 2`() {
        val game = WaveDefenseGame()
        game.start(listOf(defender1))
        repeat(WaveDefenseGame.COUNTDOWN_TICKS) { game.tick() }
        game.onWaveCleared()
        assertEquals(WavePhase.TRICK_OR_TREAT, game.phase)

        // Run through all 30 seconds of timeout
        repeat(WaveDefenseGame.TRICK_OR_TREAT_TICKS - 1) {
            assertEquals(WaveTickResult.None, game.tick())
        }
        val timeoutResult = game.tick()
        assertTrue(timeoutResult is WaveTickResult.PhaseChanged)
        assertEquals(WavePhase.INTERMISSION, (timeoutResult as WaveTickResult.PhaseChanged).newPhase)
        assertEquals(HalloweenChoice.NONE, game.halloweenChoice)
        assertFalse(game.isTrickWave)

        // Intermission starts normal wave 2
        repeat(WaveDefenseGame.INTERMISSION_TICKS) { game.tick() }
        assertEquals(2, game.currentWave)
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
