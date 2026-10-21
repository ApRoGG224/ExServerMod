package com.fome.exservermod.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class HideAndSeekGameTest {
    private val player1: UUID = UUID.randomUUID()
    private val player2: UUID = UUID.randomUUID()
    private val outsider: UUID = UUID.randomUUID()

    // Deterministic role assigner: first argument is always hider, second is seeker
    private val deterministicGame = HideAndSeekGame(roleAssigner = { p1, p2 -> p1 to p2 })

    @Test
    fun testInitialState() {
        val game = HideAndSeekGame()
        assertEquals(GamePhase.IDLE, game.phase)
        assertFalse(game.isGameActive())
        assertNull(game.hiderUuid)
        assertNull(game.seekerUuid)
    }

    @Test
    fun testVotingSingleVote() {
        val game = HideAndSeekGame()
        val result = game.vote(player1, GameMode.CLASSIC)

        assertTrue(result is VoteResult.VoteRecorded)
        assertEquals(GamePhase.VOTING, game.phase)
        assertEquals(1, game.votes.size)
    }

    @Test
    fun testVotingMismatchedModes() {
        val game = HideAndSeekGame()
        game.vote(player1, GameMode.CLASSIC)
        val result = game.vote(player2, GameMode.MOB)

        assertTrue(result is VoteResult.ModeMismatch)
        assertEquals(GamePhase.VOTING, game.phase)
    }

    @Test
    fun testVotingSuccessfulStart() {
        val game = deterministicGame
        game.vote(player1, GameMode.MOB)
        val result = game.vote(player2, GameMode.MOB)

        assertTrue(result is VoteResult.GameStarted)
        val started = result as VoteResult.GameStarted
        assertEquals(GameMode.MOB, started.mode)
        assertEquals(player1, started.hiderUuid)
        assertEquals(player2, started.seekerUuid)

        assertEquals(GamePhase.HIDING, game.phase)
        assertTrue(game.isGameActive())
        assertTrue(game.isParticipant(player1))
        assertTrue(game.isParticipant(player2))
        assertFalse(game.isParticipant(outsider))
        assertEquals(HideAndSeekGame.HIDING_DURATION_TICKS, game.remainingTicks)
    }

    @Test
    fun testEarlySearchStart() {
        val game = deterministicGame
        game.vote(player1, GameMode.CLASSIC)
        game.vote(player2, GameMode.CLASSIC)

        assertFalse(game.startSearch(outsider))
        assertEquals(GamePhase.HIDING, game.phase)

        assertTrue(game.startSearch(player1))
        assertEquals(GamePhase.SEEKING, game.phase)
        assertTrue(game.isGameActive())
    }

    @Test
    fun testHidingTimerTransition() {
        val game = deterministicGame
        game.vote(player1, GameMode.CLASSIC)
        game.vote(player2, GameMode.CLASSIC)

        // Advance ticks until 1 tick before transition
        for (i in 1 until HideAndSeekGame.HIDING_DURATION_TICKS) {
            val tickResult = game.tick()
            assertEquals(TickResult.None, tickResult)
            assertEquals(GamePhase.HIDING, game.phase)
        }

        // Final tick triggers transition to SEEKING
        val transition = game.tick()
        assertTrue(transition is TickResult.PhaseChanged)
        val changed = transition as TickResult.PhaseChanged
        assertEquals(GamePhase.HIDING, changed.oldPhase)
        assertEquals(GamePhase.SEEKING, changed.newPhase)
        assertEquals(GamePhase.SEEKING, game.phase)
    }

    @Test
    fun testSeekerHitsHiderTransitionsToIntermission() {
        val game = deterministicGame
        game.vote(player1, GameMode.CLASSIC)
        game.vote(player2, GameMode.CLASSIC)
        game.startSearch(player1)

        // Invalid hits (wrong attacker or outsider)
        assertFalse(game.onSeekerHitHider(player1, player2))
        assertFalse(game.onSeekerHitHider(outsider, player1))

        // Valid hit: seeker hits hider
        assertTrue(game.onSeekerHitHider(player2, player1))
        assertEquals(GamePhase.INTERMISSION, game.phase)
        assertFalse(game.isGameActive())
        assertEquals(player2, game.winnerUuid)
        assertEquals(HideAndSeekGame.INTERMISSION_DURATION_TICKS, game.remainingTicks)
    }

    @Test
    fun testRoleSwapOnContinueNextRound() {
        val game = deterministicGame
        game.vote(player1, GameMode.CLASSIC)
        game.vote(player2, GameMode.CLASSIC)
        game.startSearch(player1)
        game.onSeekerHitHider(player2, player1)

        // Initially: player1 = hider, player2 = seeker
        assertEquals(player1, game.hiderUuid)
        assertEquals(player2, game.seekerUuid)

        assertFalse(game.continueNextRound(outsider))

        assertTrue(game.continueNextRound(player1))
        assertEquals(GamePhase.HIDING, game.phase)
        assertTrue(game.isGameActive())

        // Roles must now be swapped: player2 = hider, player1 = seeker
        assertEquals(player2, game.hiderUuid)
        assertEquals(player1, game.seekerUuid)
        assertNull(game.winnerUuid)
        assertEquals(HideAndSeekGame.HIDING_DURATION_TICKS, game.remainingTicks)
    }

    @Test
    fun testLeaveResetsGame() {
        val game = deterministicGame
        game.vote(player1, GameMode.CLASSIC)
        game.vote(player2, GameMode.CLASSIC)

        assertTrue(game.leave(player1))
        assertEquals(GamePhase.IDLE, game.phase)
        assertFalse(game.isGameActive())
        assertNull(game.hiderUuid)
        assertNull(game.seekerUuid)
    }

    @Test
    fun testIntermissionTimeoutResetsToIdle() {
        val game = deterministicGame
        game.vote(player1, GameMode.CLASSIC)
        game.vote(player2, GameMode.CLASSIC)
        game.startSearch(player1)
        game.onSeekerHitHider(player2, player1)

        for (i in 1 until HideAndSeekGame.INTERMISSION_DURATION_TICKS) {
            val res = game.tick()
            assertEquals(TickResult.None, res)
            assertEquals(GamePhase.INTERMISSION, game.phase)
        }

        val res = game.tick()
        assertTrue(res is TickResult.PhaseChanged)
        val changed = res as TickResult.PhaseChanged
        assertEquals(GamePhase.INTERMISSION, changed.oldPhase)
        assertEquals(GamePhase.IDLE, changed.newPhase)
        assertEquals(GamePhase.IDLE, game.phase)
    }

    @Test
    fun testRequestHintPhasesAndRoles() {
        val game = deterministicGame
        // In IDLE phase
        assertTrue(game.requestHint(player2) is HintResult.NotSeekingPhase)

        // Start game -> HIDING phase (player1 = hider, player2 = seeker)
        game.vote(player1, GameMode.MOB)
        game.vote(player2, GameMode.MOB)
        assertEquals(GamePhase.HIDING, game.phase)
        assertTrue(game.requestHint(player2) is HintResult.NotSeekingPhase)

        // Start search -> SEEKING phase
        game.startSearch(player1)
        assertEquals(GamePhase.SEEKING, game.phase)

        // Hider or outsider cannot request hint
        assertTrue(game.requestHint(player1) is HintResult.NotSeeker)
        assertTrue(game.requestHint(outsider) is HintResult.NotSeeker)

        // Seeker requests hint successfully
        val result1 = game.requestHint(player2)
        assertTrue(result1 is HintResult.Success)

        // Requesting immediately is on cooldown
        val result2 = game.requestHint(player2)
        assertTrue(result2 is HintResult.OnCooldown)
        val cooldown = result2 as HintResult.OnCooldown
        assertEquals(HideAndSeekGame.HINT_COOLDOWN_SECONDS, cooldown.remainingSeconds)

        // Advance 299 seconds (5980 ticks) -> still on cooldown
        for (i in 0 until 5980) {
            game.tick()
        }
        val result3 = game.requestHint(player2)
        assertTrue(result3 is HintResult.OnCooldown)

        // Advance 20 more ticks (total 300 seconds) -> ready again
        for (i in 0 until 20) {
            game.tick()
        }
        val result4 = game.requestHint(player2)
        assertTrue(result4 is HintResult.Success)
    }

    @Test
    fun testStartGameDirectly() {
        val game = deterministicGame
        val result = game.startGameDirectly(player1, player2, GameMode.MOB)

        assertTrue(result is VoteResult.GameStarted)
        val started = result as VoteResult.GameStarted
        assertEquals(GameMode.MOB, started.mode)
        assertEquals(player1, started.hiderUuid)
        assertEquals(player2, started.seekerUuid)
        assertEquals(GamePhase.HIDING, game.phase)
        assertTrue(game.isGameActive())
    }

    @Test
    fun testStartGameDirectlyInvalidPhase() {
        val game = deterministicGame
        game.startGameDirectly(player1, player2, GameMode.CLASSIC)
        assertEquals(GamePhase.HIDING, game.phase)

        val secondStart = game.startGameDirectly(player1, player2, GameMode.MOB)
        assertTrue(secondStart is VoteResult.InvalidPhase)
    }

    @Test
    fun testCalculateDistanceTier() {
        assertEquals(1, HideAndSeekGame.calculateDistanceTier(0.0))
        assertEquals(1, HideAndSeekGame.calculateDistanceTier(1.0))
        assertEquals(1, HideAndSeekGame.calculateDistanceTier(2.5))
        assertEquals(5, HideAndSeekGame.calculateDistanceTier(3.0))
        assertEquals(5, HideAndSeekGame.calculateDistanceTier(5.0))
        assertEquals(5, HideAndSeekGame.calculateDistanceTier(7.4))
        assertEquals(10, HideAndSeekGame.calculateDistanceTier(7.6))
        assertEquals(10, HideAndSeekGame.calculateDistanceTier(10.0))
        assertEquals(15, HideAndSeekGame.calculateDistanceTier(14.8))
        assertEquals(20, HideAndSeekGame.calculateDistanceTier(21.0))
        assertEquals(25, HideAndSeekGame.calculateDistanceTier(26.0))
        assertEquals(30, HideAndSeekGame.calculateDistanceTier(32.0))
        assertEquals(35, HideAndSeekGame.calculateDistanceTier(34.0))
        assertEquals(40, HideAndSeekGame.calculateDistanceTier(41.0))
        assertEquals(45, HideAndSeekGame.calculateDistanceTier(44.0))
        assertEquals(50, HideAndSeekGame.calculateDistanceTier(49.0))
        assertEquals(55, HideAndSeekGame.calculateDistanceTier(53.0))
        assertEquals(60, HideAndSeekGame.calculateDistanceTier(61.0))
        assertEquals(85, HideAndSeekGame.calculateDistanceTier(87.0))
    }
}
