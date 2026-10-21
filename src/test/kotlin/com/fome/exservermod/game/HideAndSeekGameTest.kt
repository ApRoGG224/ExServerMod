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
}
