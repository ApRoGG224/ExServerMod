package com.fome.exservermod.game

import com.fome.exservermod.disguise.HintCategory

sealed class HintResult {
    data object NotSeekingPhase : HintResult()

    data object NotSeeker : HintResult()

    data class OnCooldown(
        val remainingSeconds: Int,
    ) : HintResult()

    data class Success(
        val lastCategory: HintCategory?,
    ) : HintResult()
}
