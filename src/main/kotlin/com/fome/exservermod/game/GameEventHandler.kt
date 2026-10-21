package com.fome.exservermod.game

import com.fome.exservermod.disguise.DisguiseManager
import com.fome.exservermod.disguise.MobPool
import com.fome.exservermod.landmarks.LandmarkManager
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import kotlin.math.hypot

class GameEventHandler(
    private val game: HideAndSeekGame,
    private val landmarks: LandmarkManager,
    private val disguiseManager: DisguiseManager = DisguiseManager(),
) {
    companion object {
        const val HINT_INTERVAL_TICKS = 50 * HideAndSeekGame.TICKS_PER_SECOND // 50 seconds = 1000 ticks
        const val ACOUSTIC_TAUNT_INTERVAL_TICKS = 45 * HideAndSeekGame.TICKS_PER_SECOND // 45 seconds = 900 ticks
        const val INTELLECTUAL_HINT_INTERVAL_TICKS = 90 * HideAndSeekGame.TICKS_PER_SECOND // 90 seconds = 1800 ticks
        const val MOVEMENT_ALERT_DISTANCE = 20.0
    }

    private var hiderInitialX: Double? = null
    private var hiderInitialZ: Double? = null
    private var hasAlertedMovedTwentyBlocks: Boolean = false

    fun register() {
        registerDamageProtection()
        registerBlockBreakProtection()
        registerAttackHandler()
        registerServerTick()
        registerDisconnect()
        registerServerStopping()
    }

    private fun registerDamageProtection() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, _, _ ->
            if (entity is ServerPlayer && game.isGameActive() && game.isParticipant(entity.uuid)) {
                false
            } else {
                true
            }
        }
    }

    private fun registerBlockBreakProtection() {
        PlayerBlockBreakEvents.BEFORE.register { _, player, _, _, _ ->
            if (player is ServerPlayer && game.isGameActive() && game.isParticipant(player.uuid)) {
                false
            } else {
                true
            }
        }
    }

    private fun registerAttackHandler() {
        AttackEntityCallback.EVENT.register { player, level, _, entity, _ ->
            if (!level.isClientSide && player is ServerPlayer && entity is ServerPlayer) {
                if (game.phase == GamePhase.SEEKING && player.uuid == game.seekerUuid && entity.uuid == game.hiderUuid) {
                    val caught = game.onSeekerHitHider(player.uuid, entity.uuid)
                    if (caught) {
                        hiderInitialX = null
                        hiderInitialZ = null
                        hasAlertedMovedTwentyBlocks = false

                        val seekerName = player.name.string
                        val hiderName = entity.name.string
                        val elapsedSeconds = game.elapsedSeekingTicks / HideAndSeekGame.TICKS_PER_SECOND

                        val catchMessage =
                            Component
                                .translatable(
                                    "exservermod.game.catch_announcement",
                                    seekerName,
                                    hiderName,
                                    elapsedSeconds,
                                ).withStyle(ChatFormatting.GOLD)
                        level.server?.playerList?.broadcastSystemMessage(catchMessage, false)

                        val continueButton =
                            Component.translatable("exservermod.game.button.continue_round").withStyle { style ->
                                style
                                    .withColor(ChatFormatting.GREEN)
                                    .withClickEvent(ClickEvent.RunCommand("/games continue"))
                                    .withHoverEvent(
                                        HoverEvent.ShowText(Component.translatable("exservermod.game.button.continue_round.hover_catch")),
                                    )
                            }
                        level.server?.playerList?.broadcastSystemMessage(continueButton, false)

                        if (game.mode == GameMode.MOB) {
                            val disguise = disguiseManager.getDisguise(entity.uuid)
                            if (disguise != null) {
                                val revealMsg =
                                    Component
                                        .translatable(
                                            "exservermod.disguise.revealed",
                                            entity.name.string,
                                            disguise.description,
                                        ).withStyle(ChatFormatting.GOLD)
                                level.server?.playerList?.broadcastSystemMessage(revealMsg, false)
                            }
                            disguiseManager.clearDisguise(entity)
                        }

                        return@register InteractionResult.SUCCESS
                    }
                }
            }
            InteractionResult.PASS
        }
    }

    private fun registerServerTick() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            val transition = game.tick()
            if (transition is TickResult.PhaseChanged) {
                when (transition.newPhase) {
                    GamePhase.SEEKING -> {
                        val seekerName =
                            game.seekerUuid?.let {
                                server.playerList
                                    .getPlayer(it)
                                    ?.name
                                    ?.string
                            } ?: "Seeker"
                        server.playerList.broadcastSystemMessage(
                            Component
                                .translatable("exservermod.game.seeking_announcement", seekerName)
                                .withStyle(ChatFormatting.RED),
                            false,
                        )

                        // Record initial hider position for 20-block movement warning
                        val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }
                        if (hider != null) {
                            hiderInitialX = hider.x
                            hiderInitialZ = hider.z
                            hasAlertedMovedTwentyBlocks = false
                        }
                    }
                    GamePhase.IDLE -> {
                        disguiseManager.clearAll(server)
                        hiderInitialX = null
                        hiderInitialZ = null
                        hasAlertedMovedTwentyBlocks = false
                        server.playerList.broadcastSystemMessage(
                            Component
                                .translatable("exservermod.game.reset_idle")
                                .withStyle(ChatFormatting.GRAY),
                            false,
                        )
                    }
                    GamePhase.HIDING, GamePhase.VOTING, GamePhase.INTERMISSION -> {
                        hiderInitialX = null
                        hiderInitialZ = null
                        hasAlertedMovedTwentyBlocks = false
                    }
                }
            }

            if (game.phase == GamePhase.SEEKING) {
                val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }

                // Elytra invariant: strictly forbidden for hider during seeking phase
                if (hider != null && hider.isFallFlying) {
                    hider.stopFallFlying()
                    hider.sendSystemMessage(
                        Component
                            .translatable("exservermod.game.elytra_forbidden")
                            .withStyle(ChatFormatting.RED),
                        true,
                    )
                }

                // 20-block movement alert from starting hiding spot
                if (hider != null && !hasAlertedMovedTwentyBlocks) {
                    val startX = hiderInitialX
                    val startZ = hiderInitialZ
                    if (startX != null && startZ != null) {
                        val distance = hypot(hider.x - startX, hider.z - startZ)
                        if (distance > MOVEMENT_ALERT_DISTANCE) {
                            hasAlertedMovedTwentyBlocks = true
                            server.playerList.broadcastSystemMessage(
                                Component
                                    .translatable("exservermod.hideandseek.alert.moved_twenty_blocks")
                                    .withStyle(ChatFormatting.GOLD),
                                false,
                            )
                        }
                    }
                }

                // Periodic proximity hint (every 50 seconds = 1000 ticks)
                if (game.elapsedSeekingTicks > 0 && game.elapsedSeekingTicks % HINT_INTERVAL_TICKS == 0) {
                    if (hider != null) {
                        val dim =
                            hider
                                .level()
                                .dimension()
                                .identifier()
                                .toString()
                        val closest = landmarks.findClosest(hider.x, hider.z, dim)
                        if (closest != null) {
                            server.playerList.broadcastSystemMessage(
                                Component
                                    .translatable("exservermod.landmarks.hint.closest", closest.name)
                                    .withStyle(ChatFormatting.AQUA),
                                false,
                            )
                        }
                    }
                }

                // Acoustic sound taunt (every 45 seconds = 900 ticks, radius 35-40 blocks)
                if (game.mode == GameMode.MOB &&
                    game.elapsedSeekingTicks > 0 &&
                    game.elapsedSeekingTicks % ACOUSTIC_TAUNT_INTERVAL_TICKS == 0
                ) {
                    if (hider != null) {
                        val mob = disguiseManager.getDisguise(hider.uuid)
                        if (mob != null) {
                            val sound = MobPool.getAmbientSound(mob)
                            hider.level().playSound(null, hider.x, hider.y, hider.z, sound, SoundSource.NEUTRAL, 2.5f, 1.0f)
                        }
                    }
                }

                // Intellectual categorical hint (every 90 seconds = 1800 ticks)
                if (game.mode == GameMode.MOB &&
                    game.elapsedSeekingTicks > 0 &&
                    game.elapsedSeekingTicks % INTELLECTUAL_HINT_INTERVAL_TICKS == 0
                ) {
                    if (hider != null) {
                        val mob = disguiseManager.getDisguise(hider.uuid)
                        if (mob != null) {
                            val hint = MobPool.getRandomHint(mob)
                            server.playerList.broadcastSystemMessage(
                                hint.withStyle(ChatFormatting.LIGHT_PURPLE),
                                false,
                            )
                        }
                    }
                }
            }

            // Anti-stuck safeguard for disguised hider
            if (game.isGameActive() && game.mode == GameMode.MOB) {
                val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }
                if (hider != null && disguiseManager.isDisguised(hider.uuid)) {
                    disguiseManager.checkAntiStuck(hider)
                }
            }

            // Action bar countdown updates every second
            if (server.tickCount % HideAndSeekGame.TICKS_PER_SECOND == 0) {
                if (game.phase == GamePhase.HIDING) {
                    val secondsLeft = game.remainingTicks / HideAndSeekGame.TICKS_PER_SECOND
                    val msg =
                        Component
                            .translatable("exservermod.game.actionbar.hiding", secondsLeft)
                            .withStyle(ChatFormatting.YELLOW)
                    game.hiderUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                    game.seekerUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                } else if (game.phase == GamePhase.INTERMISSION) {
                    val secondsLeft = game.remainingTicks / HideAndSeekGame.TICKS_PER_SECOND
                    val msg =
                        Component
                            .translatable("exservermod.game.actionbar.intermission", secondsLeft)
                            .withStyle(ChatFormatting.AQUA)
                    game.hiderUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                    game.seekerUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                }
            }
        }
    }

    private fun registerDisconnect() {
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            disguiseManager.clearDisguise(handler.player)
            if (game.isParticipant(handler.player.uuid)) {
                game.leave(handler.player.uuid)
                server.playerList.broadcastSystemMessage(
                    Component
                        .translatable("exservermod.game.cancelled", handler.player.name.string)
                        .withStyle(ChatFormatting.GRAY),
                    false,
                )
            }
        }
    }

    private fun registerServerStopping() {
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            disguiseManager.clearAll(server)
        }
    }
}
