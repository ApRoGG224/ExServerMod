package com.fome.exservermod.game

import com.fome.exservermod.afk.AfkManager
import com.fome.exservermod.compat.bluemap.BlueMapCompat
import com.fome.exservermod.disguise.DisguiseManager
import com.fome.exservermod.disguise.MobPool
import com.fome.exservermod.landmarks.LandmarkManager
import com.fome.exservermod.network.OpenRoulettePayload
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

class GameCommands(
    private val game: HideAndSeekGame,
    private val disguiseManager: DisguiseManager = DisguiseManager(),
    private val landmarkManager: LandmarkManager? = null,
) {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            val gamesNode =
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
                                .literal("classic")
                                .executes { ctx ->
                                    val player = ctx.source.player
                                    if (player != null) {
                                        handleStartMode(player, ctx.source.server, GameMode.CLASSIC)
                                    }
                                    1
                                },
                        ).then(
                            Commands
                                .literal("mob")
                                .executes { ctx ->
                                    val player = ctx.source.player
                                    if (player != null) {
                                        handleStartMode(player, ctx.source.server, GameMode.MOB)
                                    }
                                    1
                                },
                        ).then(
                            Commands
                                .literal("start")
                                .then(
                                    Commands
                                        .literal("classic")
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                            if (player != null) {
                                                handleStartMode(player, ctx.source.server, GameMode.CLASSIC)
                                            }
                                            1
                                        },
                                ).then(
                                    Commands
                                        .literal("mob")
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                            if (player != null) {
                                                handleStartMode(player, ctx.source.server, GameMode.MOB)
                                            }
                                            1
                                        },
                                ).then(
                                    Commands
                                        .argument("mode", StringArgumentType.word())
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                            if (player != null) {
                                                val modeArg = StringArgumentType.getString(ctx, "mode").lowercase()
                                                handleVoteOrStart(player, ctx.source.server, modeArg)
                                            }
                                            1
                                        },
                                ),
                        ).then(
                            Commands
                                .literal("vote")
                                .then(
                                    Commands
                                        .literal("classic")
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                            if (player != null) {
                                                handleStartMode(player, ctx.source.server, GameMode.CLASSIC)
                                            }
                                            1
                                        },
                                ).then(
                                    Commands
                                        .literal("mob")
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                            if (player != null) {
                                                handleStartMode(player, ctx.source.server, GameMode.MOB)
                                            }
                                            1
                                        },
                                ).then(
                                    Commands
                                        .argument("mode", StringArgumentType.word())
                                        .executes { ctx ->
                                            val player = ctx.source.player
                                            if (player != null) {
                                                val modeArg = StringArgumentType.getString(ctx, "mode").lowercase()
                                                handleVoteOrStart(player, ctx.source.server, modeArg)
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
                        ).then(
                            Commands
                                .literal("hint")
                                .executes { ctx ->
                                    val player = ctx.source.player
                                    if (player != null) {
                                        handleHint(player, ctx.source.server)
                                    }
                                    1
                                },
                        ).then(
                            Commands
                                .literal("unstuck")
                                .executes { ctx ->
                                    val player = ctx.source.player
                                    if (player != null) {
                                        handleUnstuck(player)
                                    }
                                    1
                                },
                        ),
                )
            dispatcher.register(Commands.literal("game").redirect(gamesNode))
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
                        .translatable("exservermod.game.status.choose_mode")
                        .withStyle(ChatFormatting.GOLD),
                )
                player.sendSystemMessage(createVotingButtons())
            }
            GamePhase.HIDING -> {
                val seconds = game.remainingTicks / HideAndSeekGame.TICKS_PER_SECOND
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.status.hiding", seconds, game.mode)
                        .withStyle(ChatFormatting.YELLOW),
                )
                if (game.isParticipant(player.uuid)) {
                    val startSearchBtn =
                        Component.translatable("exservermod.game.button.start_search_preparation").withStyle { style ->
                            style
                                .withColor(ChatFormatting.GOLD)
                                .withClickEvent(ClickEvent.RunCommand("/games startsearch"))
                                .withHoverEvent(
                                    HoverEvent.ShowText(Component.translatable("exservermod.game.button.start_search.hover_hider")),
                                )
                        }
                    val leaveBtn =
                        Component.translatable("exservermod.game.button.cancel_game").withStyle { style ->
                            style
                                .withColor(ChatFormatting.RED)
                                .withClickEvent(ClickEvent.RunCommand("/games leave"))
                                .withHoverEvent(
                                    HoverEvent.ShowText(Component.translatable("exservermod.game.button.cancel_game.hover")),
                                )
                        }
                    player.sendSystemMessage(
                        Component
                            .empty()
                            .append(startSearchBtn)
                            .append("   ")
                            .append(leaveBtn),
                    )
                }
            }
            GamePhase.SEEKING -> {
                val elapsed = game.elapsedSeekingTicks / HideAndSeekGame.TICKS_PER_SECOND
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.status.seeking", elapsed)
                        .withStyle(ChatFormatting.RED),
                )
                if (player.uuid == game.seekerUuid) {
                    val hintBtn =
                        Component.translatable("exservermod.game.button.hint_emergency").withStyle { style ->
                            style
                                .withColor(ChatFormatting.LIGHT_PURPLE)
                                .withClickEvent(ClickEvent.RunCommand("/games hint"))
                                .withHoverEvent(
                                    HoverEvent.ShowText(Component.translatable("exservermod.game.button.hint_emergency.hover")),
                                )
                        }
                    player.sendSystemMessage(hintBtn)
                }
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
                    Component.translatable("exservermod.game.button.continue_round_action").withStyle { style ->
                        style
                            .withColor(ChatFormatting.GREEN)
                            .withClickEvent(ClickEvent.RunCommand("/games continue"))
                            .withHoverEvent(
                                HoverEvent.ShowText(Component.translatable("exservermod.game.button.continue_round.hover_catch")),
                            )
                    }
                val leaveBtn =
                    Component.translatable("exservermod.game.button.leave_action").withStyle { style ->
                        style
                            .withColor(ChatFormatting.RED)
                            .withClickEvent(ClickEvent.RunCommand("/games leave"))
                            .withHoverEvent(
                                HoverEvent.ShowText(Component.translatable("exservermod.game.button.leave.hover")),
                            )
                    }
                player.sendSystemMessage(
                    Component
                        .empty()
                        .append(continueBtn)
                        .append("   ")
                        .append(leaveBtn),
                )
            }
        }
    }

    private fun handleVoteOrStart(
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
        handleStartMode(player, server, mode)
    }

    private fun handleStartMode(
        player: ServerPlayer,
        server: MinecraftServer,
        mode: GameMode,
    ) {
        // AFK chunk-loader bot must never be dragged into mini-games
        val onlinePlayers = server.playerList.players.filter { !AfkManager.isBotPlayer(it) }
        if (onlinePlayers.size < 2) {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.game.vote.need_two_players")
                    .withStyle(ChatFormatting.RED),
            )
            return
        }

        val otherPlayer = onlinePlayers.firstOrNull { it.uuid != player.uuid } ?: onlinePlayers[1]

        // Kill any stale roulette wheel from an aborted round before opening a new one
        RouletteManager.abortAll(server)

        when (val result = game.startGameDirectly(player.uuid, otherPlayer.uuid, mode)) {
            is VoteResult.GameStarted -> {
                if (result.mode == GameMode.MOB) {
                    val hider = server.playerList.getPlayer(result.hiderUuid)
                    if (hider != null) {
                        val targetMob = MobPool.getRandomMob()
                        val targetMobId = BuiltInRegistries.ENTITY_TYPE.getKey(targetMob).toString()
                        val poolCandidates =
                            MobPool.POOL
                                .filter { it != targetMob }
                                .shuffled()
                                .take(29)
                        val candidates = (poolCandidates + targetMob).shuffled().map { BuiltInRegistries.ENTITY_TYPE.getKey(it).toString() }
                        RouletteManager.queueDisguise(hider.uuid, targetMob)
                        ServerPlayNetworking.send(hider, OpenRoulettePayload(targetMobId, candidates))
                    }
                }

                BlueMapCompat.setPlayersVisibility(listOf(result.hiderUuid, result.seekerUuid), false)

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

                broadcastPreparationButtons(server)
            }
            is VoteResult.InvalidPhase -> {
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.vote.in_progress")
                        .withStyle(ChatFormatting.RED),
                )
            }
            else -> {}
        }
    }

    private fun handleStartSearch(
        player: ServerPlayer,
        server: MinecraftServer,
    ) {
        val started = game.startSearch(player.uuid)
        if (started) {
            val participants = listOfNotNull(game.hiderUuid, game.seekerUuid)
            for (uuid in participants) {
                val p = server.playerList.getPlayer(uuid) ?: continue
                p.connection.send(
                    ClientboundSoundPacket(
                        SoundEvents.RAID_HORN,
                        SoundSource.PLAYERS,
                        p.x,
                        p.y,
                        p.z,
                        2.0f,
                        1.0f,
                        p.level().random.nextLong(),
                    ),
                )
            }
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
            // Close any stale wheel before re-rolling the disguise for the new hider
            RouletteManager.abortAll(server)

            if (game.mode == GameMode.MOB) {
                val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }
                if (hider != null) {
                    val targetMob = MobPool.getRandomMob()
                    val targetMobId = BuiltInRegistries.ENTITY_TYPE.getKey(targetMob).toString()
                    val poolCandidates =
                        MobPool.POOL
                            .filter { it != targetMob }
                            .shuffled()
                            .take(29)
                    val candidates = (poolCandidates + targetMob).shuffled().map { BuiltInRegistries.ENTITY_TYPE.getKey(it).toString() }
                    RouletteManager.queueDisguise(hider.uuid, targetMob)
                    ServerPlayNetworking.send(hider, OpenRoulettePayload(targetMobId, candidates))
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
            server.playerList.broadcastSystemMessage(
                Component
                    .translatable("exservermod.game.hiding_announcement", HideAndSeekGame.HIDING_DURATION_SECONDS)
                    .withStyle(ChatFormatting.YELLOW),
                false,
            )

            val participants = listOfNotNull(game.hiderUuid, game.seekerUuid)
            BlueMapCompat.setPlayersVisibility(participants, false)
            broadcastPreparationButtons(server)
        } else {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.game.cannot_continue")
                    .withStyle(ChatFormatting.RED),
            )
        }
    }

    private fun createVotingButtons(): Component {
        val voteClassic =
            Component.translatable("exservermod.game.button.vote_classic").withStyle { style ->
                style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(ClickEvent.RunCommand("/games classic"))
                    .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.game.button.vote_classic.hover")))
            }
        val voteMob =
            Component.translatable("exservermod.game.button.vote_mob").withStyle { style ->
                style
                    .withColor(ChatFormatting.AQUA)
                    .withClickEvent(ClickEvent.RunCommand("/games mob"))
                    .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.game.button.vote_mob.hover")))
            }
        return Component
            .empty()
            .append(voteClassic)
            .append("   ")
            .append(voteMob)
    }

    private fun broadcastPreparationButtons(server: MinecraftServer) {
        val startSearchBtn =
            Component.translatable("exservermod.game.button.start_search_preparation").withStyle { style ->
                style
                    .withColor(ChatFormatting.GOLD)
                    .withClickEvent(ClickEvent.RunCommand("/games startsearch"))
                    .withHoverEvent(
                        HoverEvent.ShowText(Component.translatable("exservermod.game.button.start_search.hover_hider")),
                    )
            }
        val leaveBtn =
            Component.translatable("exservermod.game.button.cancel_game").withStyle { style ->
                style
                    .withColor(ChatFormatting.RED)
                    .withClickEvent(ClickEvent.RunCommand("/games leave"))
                    .withHoverEvent(
                        HoverEvent.ShowText(Component.translatable("exservermod.game.button.cancel_game.hover")),
                    )
            }
        val prepRow =
            Component
                .empty()
                .append(startSearchBtn)
                .append("   ")
                .append(leaveBtn)
        server.playerList.broadcastSystemMessage(prepRow, false)
    }

    private fun handleHint(
        player: ServerPlayer,
        server: MinecraftServer,
    ) {
        when (val result = game.requestHint(player.uuid)) {
            is HintResult.NotSeekingPhase -> {
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.hint.only_seeking")
                        .withStyle(ChatFormatting.RED),
                )
            }
            is HintResult.NotSeeker -> {
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.hint.only_seeker")
                        .withStyle(ChatFormatting.RED),
                )
            }
            is HintResult.OnCooldown -> {
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.game.hint.cooldown", result.remainingSeconds)
                        .withStyle(ChatFormatting.RED),
                )
            }
            is HintResult.Success -> {
                if (game.mode == GameMode.MOB) {
                    val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }
                    val mobType = hider?.let { disguiseManager.getDisguise(it.uuid) }
                    if (mobType != null) {
                        val (hintMsg, category) = MobPool.getNextHint(mobType, game.lastHintCategory)
                        game.recordHintCategory(category)
                        val prefix = Component.translatable("exservermod.game.hint.prefix").withStyle(ChatFormatting.LIGHT_PURPLE)
                        val full = Component.empty().append(prefix).append(hintMsg.withStyle(ChatFormatting.LIGHT_PURPLE))
                        server.playerList.broadcastSystemMessage(full, false)
                    }
                } else {
                    val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }
                    if (hider != null) {
                        val dim =
                            hider
                                .level()
                                .dimension()
                                .identifier()
                                .toString()
                        val closest = landmarkManager?.findClosest(hider.x, hider.z, dim)
                        if (closest != null) {
                            val prefix = Component.translatable("exservermod.game.hint.prefix").withStyle(ChatFormatting.AQUA)
                            val landmarkMsg =
                                Component
                                    .translatable(
                                        "exservermod.landmarks.hint.closest",
                                        closest.name,
                                    ).withStyle(ChatFormatting.AQUA)
                            val full = Component.empty().append(prefix).append(landmarkMsg)
                            server.playerList.broadcastSystemMessage(full, false)
                        }
                    }
                }
            }
        }
    }

    private fun handleLeave(
        player: ServerPlayer,
        server: MinecraftServer,
    ) {
        val participants = listOfNotNull(game.hiderUuid, game.seekerUuid, player.uuid)
        TrackerCompassManager.purgeAll(server)
        val left = game.leave(player.uuid)
        if (left) {
            disguiseManager.clearAll(server)
            TrackerCompassManager.purgeAll(server)
            BlueMapCompat.setPlayersVisibility(participants, true)
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

    private fun handleUnstuck(player: ServerPlayer) {
        if (!disguiseManager.isDisguised(player.uuid)) {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.disguise.not_disguised")
                    .withStyle(ChatFormatting.RED),
            )
            return
        }

        val isSeeking = game.phase == GamePhase.SEEKING
        disguiseManager.unstuckPlayer(player, isAutomatic = false, isSeekingPhase = isSeeking)
    }
}
