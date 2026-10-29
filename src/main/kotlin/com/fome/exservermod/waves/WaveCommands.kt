package com.fome.exservermod.waves

import com.fome.exservermod.ExServerMod
import com.fome.exservermod.afk.AfkManager
import com.fome.exservermod.game.GamePhase
import com.fome.exservermod.game.RouletteManager
import com.fome.exservermod.landmarks.LandmarkManager
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3

class WaveCommands(
    private val landmarkManager: LandmarkManager,
) {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands
                    .literal("waves")
                    .executes { ctx ->
                        val player = ctx.source.player
                        if (player != null) {
                            showStatus(player)
                        }
                        1
                    }.then(
                        Commands
                            .literal("start")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    handleStart(player, null)
                                }
                                1
                            }.then(
                                Commands
                                    .argument("landmark", StringArgumentType.string())
                                    .suggests { _, builder ->
                                        SharedSuggestionProvider.suggest(
                                            landmarkManager.getAllLandmarks().map { "\"${it.name}\"" },
                                            builder,
                                        )
                                    }.executes { ctx ->
                                        val player = ctx.source.player
                                        val landmark = StringArgumentType.getString(ctx, "landmark")
                                        if (player != null) {
                                            handleStart(player, landmark)
                                        }
                                        1
                                    },
                            ),
                    ).then(
                        Commands
                            .literal("stop")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    handleStop(player)
                                }
                                1
                            },
                    ).then(
                        Commands
                            .literal("status")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    showStatus(player)
                                }
                                1
                            },
                    ).then(
                        Commands
                            .literal("locate")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    handleLocate(player)
                                }
                                1
                            },
                    ).then(
                        Commands
                            .literal("glow")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    handleLocate(player)
                                }
                                1
                            },
                    ).then(
                        Commands
                            .literal("bell")
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    handleLocate(player)
                                }
                                1
                            },
                    ),
            )
        }
    }

    private fun showStatus(player: ServerPlayer) {
        val game = ExServerMod.waveManager
        if (!game.isRunning()) {
            val startBtn =
                Component.translatable("exservermod.waves.button.start").withStyle { style ->
                    style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(ClickEvent.RunCommand("/waves start"))
                        .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.waves.button.start.hover")))
                }
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.waves.idle_intro")
                    .withStyle(ChatFormatting.AQUA)
                    .append(" ")
                    .append(startBtn),
            )
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.rules").withStyle(ChatFormatting.GRAY),
            )
        } else {
            player.sendSystemMessage(game.statusLine())
            val stopBtn =
                Component.translatable("exservermod.waves.button.stop").withStyle { style ->
                    style
                        .withColor(ChatFormatting.RED)
                        .withClickEvent(ClickEvent.RunCommand("/waves stop"))
                        .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.waves.button.stop.hover")))
                }
            player.sendSystemMessage(stopBtn)
        }
    }

    private fun handleStart(
        player: ServerPlayer,
        landmarkName: String?,
    ) {
        val server = player.level().server

        if (ExServerMod.game.isGameActive() || ExServerMod.game.phase == GamePhase.INTERMISSION) {
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.error.hideandseek_running").withStyle(ChatFormatting.RED),
            )
            return
        }

        // AFK bot Steve is excluded from wave defense
        val defenders = server.playerList.players.filter { !AfkManager.isBotPlayer(it) }
        if (defenders.isEmpty()) {
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.error.no_defenders").withStyle(ChatFormatting.RED),
            )
            return
        }

        // Close any stale roulette wheel, clear pending disguises before the fight
        RouletteManager.abortAll(server)
        ExServerMod.disguiseManager.clearAll(server)

        var arenaPos: Vec3? = null
        var displayArenaName: String? = null

        val level = player.level()
        if (landmarkName != null) {
            val clean = landmarkName.trim().removeSurrounding("\"")
            val lm = landmarkManager.getLandmark(clean)
            if (lm == null) {
                player.sendSystemMessage(
                    Component.translatable("exservermod.afk.landmark_not_found", clean).withStyle(ChatFormatting.RED),
                )
                return
            }
            val surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, lm.x.toInt(), lm.z.toInt())
            val y = if (surfaceY > level.minY) surfaceY.toDouble() else player.y
            arenaPos = Vec3(lm.x, y, lm.z)
            displayArenaName = lm.name
        } else {
            // Check for pre-built arena landmarks if no argument was passed
            val defaultLm =
                landmarkManager.getLandmark("mansion")
                    ?: landmarkManager.getLandmark("arena")
                    ?: landmarkManager.getLandmark("horde")
            if (defaultLm != null) {
                val surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, defaultLm.x.toInt(), defaultLm.z.toInt())
                val y = if (surfaceY > level.minY) surfaceY.toDouble() else player.y
                arenaPos = Vec3(defaultLm.x, y, defaultLm.z)
                displayArenaName = defaultLm.name
            }
        }

        if (!ExServerMod.waveManager.tryStart(player, defenders, arenaPos)) {
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.error.already_running").withStyle(ChatFormatting.RED),
            )
            return
        }

        if (displayArenaName != null) {
            server.playerList.broadcastSystemMessage(
                Component
                    .translatable(
                        "exservermod.waves.started_arena",
                        displayArenaName,
                        WaveDefenseGame.TOTAL_WAVES,
                    ).withStyle(ChatFormatting.GOLD),
                false,
            )
        } else {
            server.playerList.broadcastSystemMessage(
                Component
                    .translatable(
                        "exservermod.waves.started",
                        WaveDefenseGame.TOTAL_WAVES,
                    ).withStyle(ChatFormatting.GOLD),
                false,
            )
        }

        server.playerList.broadcastSystemMessage(
            Component.translatable("exservermod.waves.started_hint").withStyle(ChatFormatting.YELLOW),
            false,
        )
    }

    private fun handleLocate(player: ServerPlayer) {
        val game = ExServerMod.waveManager
        if (!game.isRunning()) {
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.error.not_running").withStyle(ChatFormatting.RED),
            )
            return
        }
        game.triggerLocateBell(player)
    }

    private fun handleStop(player: ServerPlayer) {
        val server = player.level().server

        if (!ExServerMod.waveManager.tryStop(server)) {
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.error.not_running").withStyle(ChatFormatting.RED),
            )
            return
        }

        val startBtn =
            Component.translatable("exservermod.waves.button.start").withStyle { style ->
                style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(ClickEvent.RunCommand("/waves start"))
                    .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.waves.button.start.hover")))
            }
        server.playerList.broadcastSystemMessage(
            Component
                .translatable("exservermod.waves.stopped_by", player.name)
                .withStyle(ChatFormatting.GRAY)
                .append(" ")
                .append(startBtn),
            false,
        )
    }
}
