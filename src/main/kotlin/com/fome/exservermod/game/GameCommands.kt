package com.fome.exservermod.game

import com.fome.exservermod.disguise.DisguiseManager
import com.fome.exservermod.disguise.MobPool
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class GameCommands(
    private val game: HideAndSeekGame,
    private val disguiseManager: DisguiseManager = DisguiseManager(),
) {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands
                    .literal("games")
                    .executes { ctx ->
                        val player = ctx.source.player
                        if (player != null) {
                            showGameStatus(player, ctx.source.server)
                        }
                        1
                    }.then(
                        Commands
                            .literal("vote")
                            .then(
                                Commands
                                    .argument("mode", StringArgumentType.word())
                                    .executes { ctx ->
                                        val player = ctx.source.player
                                        if (player != null) {
                                            val modeArg = StringArgumentType.getString(ctx, "mode").lowercase()
                                            handleVote(player, ctx.source.server, modeArg)
                                        }
                                        1
                                    },
                            ),
                    ).then(
                        Commands
                            .literal("startsearch")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    handleStartSearch(player, ctx.source.server)
                                }
                                1
                            },
                    ).then(
                        Commands
                            .literal("continue")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    handleContinue(player, ctx.source.server)
                                }
                                1
                            },
                    ).then(
                        Commands
                            .literal("leave")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    handleLeave(player, ctx.source.server)
                                }
                                1
                            },
                    ),
            )
        }
    }

    private fun showGameStatus(
        player: ServerPlayer,
        server: MinecraftServer,
    ) {
        when (game.phase) {
            GamePhase.IDLE, GamePhase.VOTING -> {
                player.sendSystemMessage(
                    Component
                        .translatable(
                            "exservermod.game.status.voting",
                            game.votes.size,
                            HideAndSeekGame.REQUIRED_VOTES,
                        ).withStyle(ChatFormatting.GOLD),
                )
                val voteClassic =
                    Component.translatable("exservermod.game.button.vote_classic").withStyle { style ->
                        style
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(ClickEvent.RunCommand("/games vote classic"))
                            .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.game.button.vote_classic.hover")))
                    }
                val voteMob =
                    Component.translatable("exservermod.game.button.vote_mob").withStyle { style ->
                        style
                            .withColor(ChatFormatting.AQUA)
                            .withClickEvent(ClickEvent.RunCommand("/games vote mob"))
                            .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.game.button.vote_mob.hover")))
                    }
                player.sendSystemMessage(voteClassic.copy().append(" ").append(voteMob))
            }
            GamePhase.HIDING -> {
                val seconds = game.remainingTicks / HideAndSeekGame.TICKS_PER_SECOND
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.status.hiding", seconds, game.mode)
                        .withStyle(ChatFormatting.YELLOW),
                )
                if (player.uuid == game.hiderUuid) {
                    val earlyStart =
                        Component.translatable("exservermod.game.button.start_search").withStyle { style ->
                            style
                                .withColor(ChatFormatting.GOLD)
                                .withClickEvent(ClickEvent.RunCommand("/games startsearch"))
                                .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.game.button.start_search.hover")))
                        }
                    player.sendSystemMessage(earlyStart)
                }
            }
            GamePhase.SEEKING -> {
                val elapsed = game.elapsedSeekingTicks / HideAndSeekGame.TICKS_PER_SECOND
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.status.seeking", elapsed)
                        .withStyle(ChatFormatting.RED),
                )
            }
            GamePhase.INTERMISSION -> {
                val winnerName =
                    game.winnerUuid?.let {
                        server.playerList
                            .getPlayer(it)
                            ?.name
                            ?.string
                    } ?: "Seeker"
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.status.intermission", winnerName)
                        .withStyle(ChatFormatting.GOLD),
                )
                val continueBtn =
                    Component.translatable("exservermod.game.button.continue_round").withStyle { style ->
                        style
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(ClickEvent.RunCommand("/games continue"))
                            .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.game.button.continue_round.hover")))
                    }
                player.sendSystemMessage(continueBtn)
            }
        }
    }

    private fun handleVote(
        player: ServerPlayer,
        server: MinecraftServer,
        modeArg: String,
    ) {
        val mode =
            when (modeArg) {
                "classic" -> GameMode.CLASSIC
                "mob" -> GameMode.MOB
                else -> {
                    player.sendSystemMessage(
                        Component
                            .translatable("exservermod.game.vote.unknown_mode", modeArg)
                            .withStyle(ChatFormatting.RED),
                    )
                    return
                }
            }

        when (val result = game.vote(player.uuid, mode)) {
            is VoteResult.VoteRecorded -> {
                server.playerList.broadcastSystemMessage(
                    Component
                        .translatable(
                            "exservermod.game.vote.recorded",
                            player.name.string,
                            mode,
                            result.votedPlayers,
                            result.requiredPlayers,
                        ).withStyle(ChatFormatting.YELLOW),
                    false,
                )
            }
            is VoteResult.GameStarted -> {
                if (result.mode == GameMode.MOB) {
                    val hider = server.playerList.getPlayer(result.hiderUuid)
                    if (hider != null) {
                        disguiseManager.applyDisguise(hider, MobPool.getRandomMob())
                    }
                }

                val hiderName =
                    server.playerList
                        .getPlayer(result.hiderUuid)
                        ?.name
                        ?.string ?: "Hider"
                val seekerName =
                    server.playerList
                        .getPlayer(result.seekerUuid)
                        ?.name
                        ?.string ?: "Seeker"

                server.playerList.broadcastSystemMessage(
                    Component
                        .translatable("exservermod.game.vote.started", result.mode)
                        .withStyle(ChatFormatting.GREEN),
                    false,
                )
                server.playerList.broadcastSystemMessage(
                    Component
                        .translatable("exservermod.game.players_assigned", hiderName, seekerName)
                        .withStyle(ChatFormatting.GOLD),
                    false,
                )
                server.playerList.broadcastSystemMessage(
                    Component
                        .translatable("exservermod.game.hiding_announcement", HideAndSeekGame.HIDING_DURATION_SECONDS)
                        .withStyle(ChatFormatting.YELLOW),
                    false,
                )

                val earlyStartBtn =
                    Component.translatable("exservermod.game.button.start_search").withStyle { style ->
                        style
                            .withColor(ChatFormatting.GOLD)
                            .withClickEvent(ClickEvent.RunCommand("/games startsearch"))
                            .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.game.button.start_search.hover_hider")))
                    }
                server.playerList.getPlayer(result.hiderUuid)?.sendSystemMessage(earlyStartBtn)
            }
            is VoteResult.ModeMismatch -> {
                server.playerList.broadcastSystemMessage(
                    Component
                        .translatable("exservermod.game.vote.mismatch")
                        .withStyle(ChatFormatting.RED),
                    false,
                )
            }
            is VoteResult.InvalidPhase -> {
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.vote.in_progress")
                        .withStyle(ChatFormatting.RED),
                )
            }
        }
    }

    private fun handleStartSearch(
        player: ServerPlayer,
        server: MinecraftServer,
    ) {
        val started = game.startSearch(player.uuid)
        if (started) {
            server.playerList.broadcastSystemMessage(
                Component
                    .translatable("exservermod.game.search_started")
                    .withStyle(ChatFormatting.RED),
                false,
            )
        } else {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.game.search_cannot_start")
                    .withStyle(ChatFormatting.RED),
            )
        }
    }

    private fun handleContinue(
        player: ServerPlayer,
        server: MinecraftServer,
    ) {
        val continued = game.continueNextRound(player.uuid)
        if (continued) {
            if (game.mode == GameMode.MOB) {
                val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }
                if (hider != null) {
                    disguiseManager.applyDisguise(hider, MobPool.getRandomMob())
                }
            }

            val hiderName =
                game.hiderUuid?.let {
                    server.playerList
                        .getPlayer(it)
                        ?.name
                        ?.string
                } ?: "Hider"
            val seekerName =
                game.seekerUuid?.let {
                    server.playerList
                        .getPlayer(it)
                        ?.name
                        ?.string
                } ?: "Seeker"

            server.playerList.broadcastSystemMessage(
                Component
                    .translatable("exservermod.game.next_round")
                    .withStyle(ChatFormatting.GREEN),
                false,
            )
            server.playerList.broadcastSystemMessage(
                Component
                    .translatable("exservermod.game.players_assigned", hiderName, seekerName)
                    .withStyle(ChatFormatting.GOLD),
                false,
            )
        } else {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.game.cannot_continue")
                    .withStyle(ChatFormatting.RED),
            )
        }
    }

    private fun handleLeave(
        player: ServerPlayer,
        server: MinecraftServer,
    ) {
        val left = game.leave(player.uuid)
        if (left) {
            disguiseManager.clearAll(server)
            server.playerList.broadcastSystemMessage(
                Component
                    .translatable("exservermod.game.cancelled", player.name.string)
                    .withStyle(ChatFormatting.GRAY),
                false,
            )
        } else {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.game.not_in_game")
                    .withStyle(ChatFormatting.RED),
            )
        }
    }
}
