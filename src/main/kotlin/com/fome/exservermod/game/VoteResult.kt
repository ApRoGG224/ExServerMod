package com.fome.exservermod.game

import java.util.UUID

sealed interface VoteResult {
    data class VoteRecorded(
        val votedPlayers: Int,
        val requiredPlayers: Int,
        val mode: GameMode,
    ) : VoteResult

    data class GameStarted(
        val mode: GameMode,
        val hiderUuid: UUID,
        val seekerUuid: UUID,
    ) : VoteResult

    data class ModeMismatch(
        val totalVotes: Int,
    ) : VoteResult

    object InvalidPhase : VoteResult
}
