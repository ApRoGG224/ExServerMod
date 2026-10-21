package com.fome.exservermod.game

sealed interface TickResult {
    data class PhaseChanged(
        val oldPhase: GamePhase,
        val newPhase: GamePhase,
    ) : TickResult

    object None : TickResult
}
