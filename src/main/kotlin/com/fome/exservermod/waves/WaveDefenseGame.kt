package com.fome.exservermod.waves

import java.util.UUID

enum class WavePhase {
    IDLE,
    COUNTDOWN,
    FIGHT,
    INTERMISSION,
    VICTORY,
}

/**
 * One spawn group of a wave: vanilla mob id plus how many to spawn.
 * Champions get vanilla diamond gear and bonus health, no custom items.
 */
data class WaveSpawnGroup(
    val mobId: String,
    val count: Int,
    val isChampion: Boolean = false,
)

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
 * No Minecraft classes here so the whole flow is unit-testable.
 *
 * Flow: IDLE -> COUNTDOWN -> FIGHT -> (INTERMISSION -> FIGHT)* -> VICTORY -> IDLE.
 */
class WaveDefenseGame {
    companion object {
        const val TICKS_PER_SECOND = 20
        const val COUNTDOWN_SECONDS = 15
        const val INTERMISSION_SECONDS = 8
        const val VICTORY_SECONDS = 20
        const val COUNTDOWN_TICKS = COUNTDOWN_SECONDS * TICKS_PER_SECOND
        const val INTERMISSION_TICKS = INTERMISSION_SECONDS * TICKS_PER_SECOND
        const val VICTORY_TICKS = VICTORY_SECONDS * TICKS_PER_SECOND
        const val TOTAL_WAVES = 8

        /**
         * Vanilla-only, zero-grief wave composition. No withers, no ghasts,
         * no block-breaking mobs. Creepers spawn with explosion radius 0:
         * they still swell and boom but destroy nothing and hurt no one.
         */
        fun buildWave(wave: Int): List<WaveSpawnGroup> =
            when (wave) {
                1 -> listOf(WaveSpawnGroup("minecraft:zombie", 4))
                2 ->
                    listOf(
                        WaveSpawnGroup("minecraft:zombie", 3),
                        WaveSpawnGroup("minecraft:skeleton", 2),
                    )
                3 ->
                    listOf(
                        WaveSpawnGroup("minecraft:spider", 3),
                        WaveSpawnGroup("minecraft:zombie", 3),
                    )
                4 ->
                    listOf(
                        WaveSpawnGroup("minecraft:skeleton", 3),
                        WaveSpawnGroup("minecraft:zombie", 3),
                        WaveSpawnGroup("minecraft:cave_spider", 2),
                    )
                5 ->
                    listOf(
                        WaveSpawnGroup("minecraft:zombie", 1, isChampion = true),
                        WaveSpawnGroup("minecraft:zombie", 4),
                    )
                6 ->
                    listOf(
                        WaveSpawnGroup("minecraft:creeper", 2),
                        WaveSpawnGroup("minecraft:zombie", 3),
                        WaveSpawnGroup("minecraft:skeleton", 3),
                    )
                7 ->
                    listOf(
                        WaveSpawnGroup("minecraft:witch", 1),
                        WaveSpawnGroup("minecraft:skeleton", 3),
                        WaveSpawnGroup("minecraft:drowned", 3),
                    )
                8 ->
                    listOf(
                        WaveSpawnGroup("minecraft:zombie", 1, isChampion = true),
                        WaveSpawnGroup("minecraft:skeleton", 1, isChampion = true),
                        WaveSpawnGroup("minecraft:spider", 3),
                        WaveSpawnGroup("minecraft:creeper", 2),
                        WaveSpawnGroup("minecraft:zombie", 3),
                    )
                else -> emptyList()
            }

        fun isBossWave(wave: Int): Boolean = wave == 5 || wave == TOTAL_WAVES

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
     * Advances to VICTORY on the final wave, otherwise starts INTERMISSION.
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
            // Defensive: unknown wave number means instant victory
            currentWave = nextWave
            return finishVictory()
        }

        val oldPhase = phase
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
