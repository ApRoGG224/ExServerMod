package com.fome.exservermod.game

import java.util.UUID
import kotlin.random.Random

class HideAndSeekGame(
    private val roleAssigner: (UUID, UUID) -> Pair<UUID, UUID> = { p1, p2 ->
        if (Random.nextBoolean()) p1 to p2 else p2 to p1
    },
) {
    companion object {
        const val TICKS_PER_SECOND = 20
        const val HIDING_DURATION_SECONDS = 120
        const val INTERMISSION_DURATION_SECONDS = 60
        const val HIDING_DURATION_TICKS = HIDING_DURATION_SECONDS * TICKS_PER_SECOND
        const val INTERMISSION_DURATION_TICKS = INTERMISSION_DURATION_SECONDS * TICKS_PER_SECOND
        const val REQUIRED_VOTES = 2
    }

    var phase: GamePhase = GamePhase.IDLE
        private set

    var mode: GameMode = GameMode.CLASSIC
        private set

    var hiderUuid: UUID? = null
        private set

    var seekerUuid: UUID? = null
        private set

    var winnerUuid: UUID? = null
        private set

    var remainingTicks: Int = 0
        private set

    var elapsedSeekingTicks: Int = 0
        private set

    val votes: MutableMap<UUID, GameMode> = mutableMapOf()

    fun vote(
        playerUuid: UUID,
        selectedMode: GameMode,
    ): VoteResult {
        if (phase != GamePhase.IDLE && phase != GamePhase.VOTING) {
            return VoteResult.InvalidPhase
        }

        phase = GamePhase.VOTING
        votes[playerUuid] = selectedMode

        val matchingVoters = votes.entries.filter { it.value == selectedMode }.map { it.key }
        if (matchingVoters.size >= REQUIRED_VOTES) {
            val p1 = matchingVoters[0]
            val p2 = matchingVoters[1]
            val (hider, seeker) = roleAssigner(p1, p2)

            mode = selectedMode
            hiderUuid = hider
            seekerUuid = seeker
            winnerUuid = null
            remainingTicks = HIDING_DURATION_TICKS
            elapsedSeekingTicks = 0
            phase = GamePhase.HIDING

            return VoteResult.GameStarted(selectedMode, hider, seeker)
        }

        if (votes.size >= REQUIRED_VOTES) {
            return VoteResult.ModeMismatch(votes.size)
        }

        return VoteResult.VoteRecorded(votes.size, REQUIRED_VOTES, selectedMode)
    }

    fun startSearch(playerUuid: UUID): Boolean {
        if (phase != GamePhase.HIDING || !isParticipant(playerUuid)) {
            return false
        }
        phase = GamePhase.SEEKING
        remainingTicks = 0
        return true
    }

    fun onSeekerHitHider(
        attackerUuid: UUID,
        victimUuid: UUID,
    ): Boolean {
        if (phase != GamePhase.SEEKING) {
            return false
        }
        if (attackerUuid == seekerUuid && victimUuid == hiderUuid) {
            phase = GamePhase.INTERMISSION
            winnerUuid = seekerUuid
            remainingTicks = INTERMISSION_DURATION_TICKS
            return true
        }
        return false
    }

    fun continueNextRound(playerUuid: UUID): Boolean {
        if (phase != GamePhase.INTERMISSION || !isParticipant(playerUuid)) {
            return false
        }
        val prevHider = hiderUuid ?: return false
        val prevSeeker = seekerUuid ?: return false

        hiderUuid = prevSeeker
        seekerUuid = prevHider
        winnerUuid = null
        remainingTicks = HIDING_DURATION_TICKS
        elapsedSeekingTicks = 0
        phase = GamePhase.HIDING

        return true
    }

    fun leave(playerUuid: UUID): Boolean {
        if (isParticipant(playerUuid) || phase == GamePhase.VOTING) {
            reset()
            return true
        }
        return false
    }

    fun tick(): TickResult =
        when (phase) {
            GamePhase.HIDING -> {
                remainingTicks--
                if (remainingTicks <= 0) {
                    phase = GamePhase.SEEKING
                    TickResult.PhaseChanged(GamePhase.HIDING, GamePhase.SEEKING)
                } else {
                    TickResult.None
                }
            }
            GamePhase.SEEKING -> {
                elapsedSeekingTicks++
                TickResult.None
            }
            GamePhase.INTERMISSION -> {
                remainingTicks--
                if (remainingTicks <= 0) {
                    reset()
                    TickResult.PhaseChanged(GamePhase.INTERMISSION, GamePhase.IDLE)
                } else {
                    TickResult.None
                }
            }
            GamePhase.IDLE, GamePhase.VOTING -> TickResult.None
        }

    fun isParticipant(uuid: UUID): Boolean = uuid == hiderUuid || uuid == seekerUuid

    fun isGameActive(): Boolean = phase == GamePhase.HIDING || phase == GamePhase.SEEKING

    fun reset() {
        phase = GamePhase.IDLE
        mode = GameMode.CLASSIC
        hiderUuid = null
        seekerUuid = null
        winnerUuid = null
        remainingTicks = 0
        elapsedSeekingTicks = 0
        votes.clear()
    }
}
