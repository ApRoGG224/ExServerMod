package com.fome.exservermod.waves

import com.fome.exservermod.ExServerMod
import com.fome.exservermod.afk.AfkManager
import com.fome.exservermod.game.GamePhase
import com.fome.exservermod.game.RouletteManager
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.level.ServerPlayer

class WaveCommands {
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

    private fun handleStart(player: ServerPlayer) {
        val server = player.level().server

        if (ExServerMod.game.isGameActive() || ExServerMod.game.phase == GamePhase.INTERMISSION) {
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.error.hideandseek_running").withStyle(ChatFormatting.RED),
            )
            return
        }

        // AFK chunk-loader bot must never be dragged into wave defense
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

        if (!ExServerMod.waveManager.tryStart(player, defenders)) {
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.error.already_running").withStyle(ChatFormatting.RED),
            )
            return
        }

        server.playerList.broadcastSystemMessage(
            Component
                .translatable(
                    "exservermod.waves.started",
                    WaveDefenseGame.TOTAL_WAVES,
                ).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
            false,
        )
        server.playerList.broadcastSystemMessage(
            Component.translatable("exservermod.waves.started_hint").withStyle(ChatFormatting.YELLOW),
            false,
        )
    }

    private fun handleStop(player: ServerPlayer) {
        val server = player.level().server

        if (!ExServerMod.waveManager.tryStop(server)) {
            player.sendSystemMessage(
                Component.translatable("exservermod.waves.error.not_running").withStyle(ChatFormatting.RED),
            )
            return
        }

        val stopBtn =
            Component.translatable("exservermod.waves.button.start").withStyle { style ->
                style
                    .withColor(ChatFormatting.GREEN)
                    .withClickEvent(ClickEvent.RunCommand("/waves start"))
                    .withHoverEvent(HoverEvent.ShowText(Component.translatable("exservermod.waves.button.start.hover")))
            }
        server.playerList.broadcastSystemMessage(
            Component
                .translatable("exservermod.waves.stopped_by", player.name.string)
                .withStyle(ChatFormatting.GRAY)
                .append(" ")
                .append(stopBtn),
            false,
        )
    }
}
