package com.fome.exservermod.waves

import java.util.UUID

enum class WavePhase {
    IDLE,
    COUNTDOWN,
    FIGHT,
    INTERMISSION,
    VICTORY,
}

enum class WaveMobRole {
    STANDARD,
    CHAMPION,
    BLAZE_CHAMPION,
    WITCH_CHAMPION,
    GHOST_CREEPER,
    SPECTRAL_VEX,
}

/**
 * One spawn group of a wave: vanilla mob id, spawn count, and role/mechanics archetype.
 */
data class WaveSpawnGroup(
    val mobId: String,
    val count: Int,
    val role: WaveMobRole = WaveMobRole.STANDARD,
) {
    val isChampion: Boolean
        get() = role != WaveMobRole.STANDARD
}

sealed interface WaveTickResult {
    data object None : WaveTickResult

    data class PhaseChanged(
        val oldPhase: WavePhase,
        val newPhase: WavePhase,
    ) : WaveTickResult

    data class WaveStarted(
        val wave: Int,
        val totalWaves: Int,
        val groups: List<WaveSpawnGroup>,
    ) : WaveTickResult

    data object Victory : WaveTickResult
}

/**
 * Pure state machine for the mob-wave defense mini-game.
 * Zero Minecraft classes here so the whole flow is unit-testable.
 *
 * Flow: IDLE -> COUNTDOWN -> FIGHT -> (INTERMISSION -> FIGHT)* -> VICTORY -> IDLE.
 */
class WaveDefenseGame {
    companion object {
        const val TICKS_PER_SECOND = 20
        const val COUNTDOWN_SECONDS = 15
        const val INTERMISSION_SECONDS = 15
        const val VICTORY_SECONDS = 20
        const val COUNTDOWN_TICKS = COUNTDOWN_SECONDS * TICKS_PER_SECOND
        const val INTERMISSION_TICKS = INTERMISSION_SECONDS * TICKS_PER_SECOND
        const val VICTORY_TICKS = VICTORY_SECONDS * TICKS_PER_SECOND
        const val TOTAL_WAVES = 11

        /**
         * 11-wave nightmare gauntlet. Vanilla living hostile mobs only; zero world damage.
         * Creepers spawn with explosion radius 0 and lingering spectral clouds.
         * Vexes phase through mansion walls. Blazes and Witches cast non-destructive spectral spells.
         */
        fun buildWave(wave: Int): List<WaveSpawnGroup> =
            when (wave) {
                1 ->
                    listOf(
                        WaveSpawnGroup("minecraft:zombie", 8),
                        WaveSpawnGroup("minecraft:skeleton", 5),
                        WaveSpawnGroup("minecraft:cave_spider", 2),
                    )
                2 ->
                    listOf(
                        WaveSpawnGroup("minecraft:zombie", 8),
                        WaveSpawnGroup("minecraft:skeleton", 6),
                        WaveSpawnGroup("minecraft:husk", 4),
                    )
                3 ->
                    listOf(
                        WaveSpawnGroup("minecraft:bogged", 6),
                        WaveSpawnGroup("minecraft:vindicator", 6),
                        WaveSpawnGroup("minecraft:husk", 4),
                        WaveSpawnGroup("minecraft:creeper", 4, WaveMobRole.GHOST_CREEPER),
                    )
                4 ->
                    listOf(
                        WaveSpawnGroup("minecraft:wither_skeleton", 8),
                        WaveSpawnGroup("minecraft:piglin_brute", 6),
                        WaveSpawnGroup("minecraft:hoglin", 4),
                        WaveSpawnGroup("minecraft:vex", 4, WaveMobRole.SPECTRAL_VEX),
                    )
                5 ->
                    listOf(
                        WaveSpawnGroup("minecraft:blaze", 1, WaveMobRole.BLAZE_CHAMPION),
                        WaveSpawnGroup("minecraft:blaze", 6),
                        WaveSpawnGroup("minecraft:skeleton", 8),
                        WaveSpawnGroup("minecraft:wither_skeleton", 6),
                        WaveSpawnGroup("minecraft:creeper", 4, WaveMobRole.GHOST_CREEPER),
                    )
                6 ->
                    listOf(
                        WaveSpawnGroup("minecraft:evoker", 2),
                        WaveSpawnGroup("minecraft:vindicator", 8),
                        WaveSpawnGroup("minecraft:bogged", 6),
                        WaveSpawnGroup("minecraft:vex", 4, WaveMobRole.SPECTRAL_VEX),
                        WaveSpawnGroup("minecraft:creeper", 4, WaveMobRole.GHOST_CREEPER),
                    )
                7 ->
                    listOf(
                        WaveSpawnGroup("minecraft:warden", 1),
                        WaveSpawnGroup("minecraft:zombie", 8),
                        WaveSpawnGroup("minecraft:skeleton", 8),
                        WaveSpawnGroup("minecraft:husk", 5),
                        WaveSpawnGroup("minecraft:creeper", 4, WaveMobRole.GHOST_CREEPER),
                    )
                8 ->
                    listOf(
                        WaveSpawnGroup("minecraft:witch", 1, WaveMobRole.WITCH_CHAMPION),
                        WaveSpawnGroup("minecraft:witch", 6),
                        WaveSpawnGroup("minecraft:wither_skeleton", 8),
                        WaveSpawnGroup("minecraft:piglin_brute", 8),
                        WaveSpawnGroup("minecraft:vex", 5, WaveMobRole.SPECTRAL_VEX),
                    )
                9 ->
                    listOf(
                        WaveSpawnGroup("minecraft:zombie", 12),
                        WaveSpawnGroup("minecraft:skeleton", 8),
                        WaveSpawnGroup("minecraft:hoglin", 6),
                        WaveSpawnGroup("minecraft:creeper", 4, WaveMobRole.GHOST_CREEPER),
                    )
                10 ->
                    listOf(
                        WaveSpawnGroup("minecraft:zombie", 2, WaveMobRole.CHAMPION),
                        WaveSpawnGroup("minecraft:vindicator", 8),
                        WaveSpawnGroup("minecraft:evoker", 2),
                        WaveSpawnGroup("minecraft:bogged", 8),
                        WaveSpawnGroup("minecraft:blaze", 6),
                        WaveSpawnGroup("minecraft:vex", 6, WaveMobRole.SPECTRAL_VEX),
                    )
                11 ->
                    listOf(
                        WaveSpawnGroup("minecraft:blaze", 1, WaveMobRole.BLAZE_CHAMPION),
                        WaveSpawnGroup("minecraft:witch", 1, WaveMobRole.WITCH_CHAMPION),
                        WaveSpawnGroup("minecraft:zombie", 2, WaveMobRole.CHAMPION),
                        WaveSpawnGroup("minecraft:zombie", 8),
                        WaveSpawnGroup("minecraft:skeleton", 8),
                        WaveSpawnGroup("minecraft:wither_skeleton", 6),
                        WaveSpawnGroup("minecraft:vex", 5, WaveMobRole.SPECTRAL_VEX),
                        WaveSpawnGroup("minecraft:creeper", 4, WaveMobRole.GHOST_CREEPER),
                    )
                else -> emptyList()
            }

        fun isBossWave(wave: Int): Boolean = wave == 5 || wave == 7 || wave == 8 || wave == TOTAL_WAVES

        fun totalMobsInWave(groups: List<WaveSpawnGroup>): Int = groups.sumOf { it.count }
    }

    var phase: WavePhase = WavePhase.IDLE
        private set

    var currentWave: Int = 0
        private set

    var remainingTicks: Int = 0
        private set

    var kills: Int = 0
        private set

    val defenders: MutableSet<UUID> = mutableSetOf()

    fun start(defenderUuids: List<UUID>): Boolean {
        if (phase != WavePhase.IDLE || defenderUuids.isEmpty()) {
            return false
        }
        defenders.clear()
        defenders.addAll(defenderUuids)
        currentWave = 0
        kills = 0
        remainingTicks = COUNTDOWN_TICKS
        phase = WavePhase.COUNTDOWN
        return true
    }

    fun isActive(): Boolean = phase != WavePhase.IDLE

    fun isFighting(): Boolean = phase == WavePhase.FIGHT

    /**
     * Called by the manager when every mob of the current wave is dead.
     * Advances to VICTORY on wave 11, otherwise starts INTERMISSION.
     */
    fun onWaveCleared(): WaveTickResult {
        if (phase != WavePhase.FIGHT) {
            return WaveTickResult.None
        }
        return if (currentWave >= TOTAL_WAVES) {
            finishVictory()
        } else {
            val oldPhase = phase
            phase = WavePhase.INTERMISSION
            remainingTicks = INTERMISSION_TICKS
            WaveTickResult.PhaseChanged(oldPhase, WavePhase.INTERMISSION)
        }
    }

    fun stop() {
        phase = WavePhase.IDLE
        remainingTicks = 0
        currentWave = 0
        kills = 0
        defenders.clear()
    }

    fun tick(): WaveTickResult =
        when (phase) {
            WavePhase.COUNTDOWN -> {
                remainingTicks--
                if (remainingTicks <= 0) {
                    beginWave()
                } else {
                    WaveTickResult.None
                }
            }
            WavePhase.INTERMISSION -> {
                remainingTicks--
                if (remainingTicks <= 0) {
                    beginWave()
                } else {
                    WaveTickResult.None
                }
            }
            WavePhase.FIGHT -> WaveTickResult.None
            WavePhase.VICTORY -> {
                remainingTicks--
                if (remainingTicks <= 0) {
                    val oldPhase = phase
                    phase = WavePhase.IDLE
                    WaveTickResult.PhaseChanged(oldPhase, WavePhase.IDLE)
                } else {
                    WaveTickResult.None
                }
            }
            WavePhase.IDLE -> WaveTickResult.None
        }

    private fun beginWave(): WaveTickResult {
        val nextWave = currentWave + 1
        val groups = buildWave(nextWave)
        if (groups.isEmpty()) {
            currentWave = nextWave
            return finishVictory()
        }

        currentWave = nextWave
        phase = WavePhase.FIGHT

        return WaveTickResult.WaveStarted(nextWave, TOTAL_WAVES, groups)
    }

    fun finishVictory(): WaveTickResult {
        if (phase == WavePhase.VICTORY) {
            return WaveTickResult.None
        }
        phase = WavePhase.VICTORY
        remainingTicks = VICTORY_TICKS
        return WaveTickResult.Victory
    }

    fun registerKill() {
        kills++
    }

    fun areAllDefendersOffline(onlineUuids: Set<UUID>): Boolean = defenders.isNotEmpty() && defenders.none { it in onlineUuids }
}
