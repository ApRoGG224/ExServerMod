package com.fome.exservermod.game

import com.fome.exservermod.compat.bluemap.BlueMapCompat
import com.fome.exservermod.disguise.DisguiseManager
import com.fome.exservermod.disguise.MobPool
import com.fome.exservermod.landmarks.LandmarkManager
import com.fome.exservermod.network.DisguiseSyncPayload
import com.fome.exservermod.network.RouletteFinishedPayload
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.item.component.Fireworks
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
        const val DISTANCE_HINT_INTERVAL_TICKS = 180 * HideAndSeekGame.TICKS_PER_SECOND // 180 seconds = 3600 ticks
        const val MOVEMENT_ALERT_DISTANCE = 25.0
    }

    private var hiderInitialX: Double? = null
    private var hiderInitialZ: Double? = null
    private var hasAlertedMovedTwentyFiveBlocks: Boolean = false

    fun register() {
        registerDamageProtection()
        registerBlockBreakProtection()
        registerAttackHandler()
        registerServerTick()
        registerJoin()
        registerDisconnect()
        registerServerStopping()
        registerRouletteFinished()
    }

    private fun registerDamageProtection() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, _, _ ->
            if (entity is ServerPlayer &&
                (game.isGameActive() || game.phase == GamePhase.INTERMISSION) &&
                game.isParticipant(entity.uuid)
            ) {
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
                        hasAlertedMovedTwentyFiveBlocks = false

                        val seekerName = player.name.string
                        val hiderName = entity.name.string
                        val elapsedSeconds = game.elapsedSeekingTicks / HideAndSeekGame.TICKS_PER_SECOND
                        val minutes = elapsedSeconds / 60
                        val seconds = elapsedSeconds % 60
                        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

                        // Prominent screen TITLE to both participants
                        val animPacket = ClientboundSetTitlesAnimationPacket(10, 100, 20)
                        val titlePacket =
                            ClientboundSetTitleTextPacket(
                                Component.translatable("exservermod.game.title.caught").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                            )
                        val subtitlePacket =
                            ClientboundSetSubtitleTextPacket(
                                Component
                                    .translatable(
                                        "exservermod.game.subtitle.caught",
                                        timeFormatted,
                                    ).withStyle(ChatFormatting.GOLD),
                            )

                        player.connection.send(animPacket)
                        player.connection.send(titlePacket)
                        player.connection.send(subtitlePacket)

                        entity.connection.send(animPacket)
                        entity.connection.send(titlePacket)
                        entity.connection.send(subtitlePacket)

                        val server = level.server
                        if (server != null) {
                            playSoundToParticipants(server, SoundEvents.PLAYER_LEVELUP, 1.2f, 1.0f)
                            playSoundToParticipants(server, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
                        }

                        // Spawn celebration fireworks over seeker
                        val fireworkStack = ItemStack(Items.FIREWORK_ROCKET)
                        val explosions =
                            listOf(
                                FireworkExplosion(
                                    FireworkExplosion.Shape.LARGE_BALL,
                                    IntArrayList(intArrayOf(0xFFD700, 0xFF4500, 0x00FF00)),
                                    IntArrayList(intArrayOf(0xFFFFFF)),
                                    true,
                                    true,
                                ),
                            )
                        fireworkStack.set(DataComponents.FIREWORKS, Fireworks(1, explosions))
                        val firework = FireworkRocketEntity(player.level(), player.x, player.y + 1.5, player.z, fireworkStack)
                        player.level().addFreshEntity(firework)

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
                            Component.translatable("exservermod.game.button.continue_round_action").withStyle { style ->
                                style
                                    .withColor(ChatFormatting.GREEN)
                                    .withClickEvent(ClickEvent.RunCommand("/games continue"))
                                    .withHoverEvent(
                                        HoverEvent.ShowText(Component.translatable("exservermod.game.button.continue_round.hover_catch")),
                                    )
                            }
                        val leaveButton =
                            Component.translatable("exservermod.game.button.leave_action").withStyle { style ->
                                style
                                    .withColor(ChatFormatting.RED)
                                    .withClickEvent(ClickEvent.RunCommand("/games leave"))
                                    .withHoverEvent(
                                        HoverEvent.ShowText(Component.translatable("exservermod.game.button.leave.hover")),
                                    )
                            }
                        val buttonsRow =
                            Component
                                .empty()
                                .append(continueButton)
                                .append("   ")
                                .append(leaveButton)
                        level.server?.playerList?.broadcastSystemMessage(buttonsRow, false)

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

                        BlueMapCompat.setPlayersVisibility(listOf(player.uuid, entity.uuid), true)

                        return@register InteractionResult.SUCCESS
                    }
                }
            }
            InteractionResult.PASS
        }
    }

    private fun registerServerTick() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            RouletteManager.checkTimeouts(server, disguiseManager)
            val transition = game.tick()
            if (transition is TickResult.PhaseChanged) {
                when (transition.newPhase) {
                    GamePhase.SEEKING -> {
                        val participants = listOfNotNull(game.hiderUuid, game.seekerUuid)
                        BlueMapCompat.setPlayersVisibility(participants, false)
                        playSoundToParticipants(server, SoundEvents.RAID_HORN, 2.0f, 1.0f)

                        val seeker = game.seekerUuid?.let { server.playerList.getPlayer(it) }
                        if (seeker != null) {
                            val emergencyHintBtn =
                                Component.translatable("exservermod.game.button.hint_emergency").withStyle { style ->
                                    style
                                        .withColor(ChatFormatting.LIGHT_PURPLE)
                                        .withClickEvent(ClickEvent.RunCommand("/games hint"))
                                        .withHoverEvent(
                                            HoverEvent.ShowText(Component.translatable("exservermod.game.button.hint_emergency.hover")),
                                        )
                                }
                            seeker.sendSystemMessage(emergencyHintBtn)
                        }

                        val seekerName = seeker?.name?.string ?: "Seeker"
                        server.playerList.broadcastSystemMessage(
                            Component
                                .translatable("exservermod.game.seeking_announcement", seekerName)
                                .withStyle(ChatFormatting.RED),
                            false,
                        )

                        // Record initial hider position for 25-block movement warning
                        val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }
                        if (hider != null) {
                            hiderInitialX = hider.x
                            hiderInitialZ = hider.z
                            hasAlertedMovedTwentyFiveBlocks = false
                        }
                    }
                    GamePhase.HIDING -> {
                        val participants = listOfNotNull(game.hiderUuid, game.seekerUuid)
                        BlueMapCompat.setPlayersVisibility(participants, false)
                        hiderInitialX = null
                        hiderInitialZ = null
                        hasAlertedMovedTwentyFiveBlocks = false
                        disguiseManager.resetUnstuckCounters()
                    }
                    GamePhase.INTERMISSION -> {
                        val participants = listOfNotNull(game.hiderUuid, game.seekerUuid)
                        BlueMapCompat.setPlayersVisibility(participants, true)
                        hiderInitialX = null
                        hiderInitialZ = null
                        hasAlertedMovedTwentyFiveBlocks = false
                    }
                    GamePhase.IDLE -> {
                        val participants = listOfNotNull(game.hiderUuid, game.seekerUuid)
                        BlueMapCompat.setPlayersVisibility(participants, true)
                        disguiseManager.clearAll(server)
                        hiderInitialX = null
                        hiderInitialZ = null
                        hasAlertedMovedTwentyFiveBlocks = false
                        server.playerList.broadcastSystemMessage(
                            Component
                                .translatable("exservermod.game.reset_idle")
                                .withStyle(ChatFormatting.GRAY),
                            false,
                        )
                    }
                    GamePhase.VOTING -> {
                        hiderInitialX = null
                        hiderInitialZ = null
                        hasAlertedMovedTwentyFiveBlocks = false
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

                // 25-block movement alert from starting hiding spot
                if (hider != null && !hasAlertedMovedTwentyFiveBlocks) {
                    val startX = hiderInitialX
                    val startZ = hiderInitialZ
                    if (startX != null && startZ != null) {
                        val distance = hypot(hider.x - startX, hider.z - startZ)
                        if (distance > MOVEMENT_ALERT_DISTANCE) {
                            hasAlertedMovedTwentyFiveBlocks = true
                            server.playerList.broadcastSystemMessage(
                                Component
                                    .translatable("exservermod.hideandseek.alert.moved_twenty_five_blocks")
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                                false,
                            )

                            val seeker = game.seekerUuid?.let { server.playerList.getPlayer(it) }
                            if (seeker != null) {
                                if (seeker.level().dimension() != hider.level().dimension()) {
                                    server.playerList.broadcastSystemMessage(
                                        Component
                                            .translatable("exservermod.hideandseek.hint.distance_diff_dimension")
                                            .withStyle(ChatFormatting.YELLOW),
                                        false,
                                    )
                                } else {
                                    val distBlocks = hypot(hider.x - seeker.x, hider.z - seeker.z).toInt()
                                    server.playerList.broadcastSystemMessage(
                                        Component
                                            .translatable("exservermod.hideandseek.hint.seeker_distance", distBlocks)
                                            .withStyle(ChatFormatting.YELLOW),
                                        false,
                                    )
                                }
                            }

                            val dim =
                                hider
                                    .level()
                                    .dimension()
                                    .identifier()
                                    .toString()
                            val closest = landmarks.findClosest(hider.x, hider.z, dim)
                            if (closest != null) {
                                val landmarkDist = hypot(hider.x - closest.x, hider.z - closest.z).toInt()
                                server.playerList.broadcastSystemMessage(
                                    Component
                                        .translatable(
                                            "exservermod.hideandseek.hint.closest_landmark",
                                            closest.name,
                                            landmarkDist,
                                        ).withStyle(ChatFormatting.AQUA),
                                    false,
                                )
                            } else {
                                server.playerList.broadcastSystemMessage(
                                    Component
                                        .translatable("exservermod.hideandseek.hint.no_landmarks")
                                        .withStyle(ChatFormatting.GRAY),
                                    false,
                                )
                            }

                            playSoundToParticipants(server, SoundEvents.BELL_BLOCK, 1.2f, 1.0f)
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
                            val (hint, category) = MobPool.getNextHint(mob, game.lastHintCategory)
                            game.recordHintCategory(category)
                            server.playerList.broadcastSystemMessage(
                                hint.withStyle(ChatFormatting.LIGHT_PURPLE),
                                false,
                            )
                        }
                    }
                }

                // Periodic distance hint (every 3 minutes = 180s = 3600 ticks)
                if (game.elapsedSeekingTicks > 0 && game.elapsedSeekingTicks % DISTANCE_HINT_INTERVAL_TICKS == 0) {
                    val seeker = game.seekerUuid?.let { server.playerList.getPlayer(it) }
                    if (seeker != null && hider != null) {
                        if (seeker.level().dimension() != hider.level().dimension()) {
                            server.playerList.broadcastSystemMessage(
                                Component
                                    .translatable("exservermod.hideandseek.hint.distance_diff_dimension")
                                    .withStyle(ChatFormatting.GOLD),
                                false,
                            )
                        } else {
                            val dx = seeker.x - hider.x
                            val dy = seeker.y - hider.y
                            val dz = seeker.z - hider.z
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                            val tier = HideAndSeekGame.calculateDistanceTier(dist)
                            server.playerList.broadcastSystemMessage(
                                Component
                                    .translatable("exservermod.hideandseek.hint.distance", tier)
                                    .withStyle(ChatFormatting.GOLD),
                                false,
                            )
                            playSoundToParticipants(server, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f)
                        }
                    }
                }
            }

            // Anti-stuck reminder for disguised hider
            if (game.isGameActive() && game.mode == GameMode.MOB && server.tickCount % 40 == 0) {
                val hider = game.hiderUuid?.let { server.playerList.getPlayer(it) }
                if (hider != null && disguiseManager.isDisguised(hider.uuid) && disguiseManager.isPlayerStuck(hider)) {
                    val unstuckBtn =
                        Component.translatable("exservermod.disguise.button.unstuck").withStyle { style ->
                            style
                                .withColor(ChatFormatting.AQUA)
                                .withClickEvent(ClickEvent.RunCommand("/games unstuck"))
                                .withHoverEvent(
                                    HoverEvent.ShowText(Component.translatable("exservermod.disguise.button.unstuck.hover")),
                                )
                        }
                    hider.sendSystemMessage(
                        Component
                            .translatable("exservermod.disguise.stuck_reminder")
                            .withStyle(ChatFormatting.RED)
                            .append(" ")
                            .append(unstuckBtn),
                    )
                }
            }

            // Action bar countdown & seeking timer every second
            if (server.tickCount % HideAndSeekGame.TICKS_PER_SECOND == 0) {
                if (game.phase == GamePhase.HIDING) {
                    val secondsLeft = game.remainingTicks / HideAndSeekGame.TICKS_PER_SECOND
                    val msg =
                        Component
                            .translatable("exservermod.game.actionbar.hiding", secondsLeft)
                            .withStyle(ChatFormatting.YELLOW)
                    game.hiderUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                    game.seekerUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }

                    // Ticking note block sound for 10..1 seconds with rising pitch
                    if (secondsLeft in 1..10) {
                        val pitch = 0.6f + (10 - secondsLeft) * 0.14f
                        playSoundToParticipants(server, SoundEvents.NOTE_BLOCK_PLING, 1.0f, pitch)
                    }
                } else if (game.phase == GamePhase.SEEKING) {
                    val elapsedSeconds = game.elapsedSeekingTicks / HideAndSeekGame.TICKS_PER_SECOND
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                    val seekerName =
                        game.seekerUuid?.let {
                            server.playerList
                                .getPlayer(it)
                                ?.name
                                ?.string
                        } ?: "Искатель"
                    val msg =
                        Component
                            .translatable("exservermod.game.actionbar.seeking", timeFormatted, seekerName)
                            .withStyle(ChatFormatting.GOLD)
                    game.hiderUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                    game.seekerUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                } else if (game.phase == GamePhase.INTERMISSION) {
                    val secondsLeft = game.remainingTicks / HideAndSeekGame.TICKS_PER_SECOND
                    val nextHiderName =
                        game.seekerUuid?.let {
                            server.playerList
                                .getPlayer(it)
                                ?.name
                                ?.string
                        } ?: "Прячущийся"
                    val msg =
                        Component
                            .translatable("exservermod.game.actionbar.intermission", nextHiderName, secondsLeft)
                            .withStyle(ChatFormatting.AQUA)
                    game.hiderUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                    game.seekerUuid?.let { server.playerList.getPlayer(it)?.sendSystemMessage(msg, true) }
                }
            }
        }
    }

    private fun registerJoin() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            for ((uuid, mobType) in disguiseManager.disguises) {
                val mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mobType).toString()
                ServerPlayNetworking.send(handler.player, DisguiseSyncPayload(uuid, mobId, true))
            }
        }
    }

    private fun playSoundToParticipants(
        server: MinecraftServer,
        sound: Holder<SoundEvent>,
        volume: Float = 1.0f,
        pitch: Float = 1.0f,
    ) {
        val participants = listOfNotNull(game.hiderUuid, game.seekerUuid)
        for (uuid in participants) {
            val player = server.playerList.getPlayer(uuid) ?: continue
            player.connection.send(
                ClientboundSoundPacket(
                    sound,
                    SoundSource.PLAYERS,
                    player.x,
                    player.y,
                    player.z,
                    volume,
                    pitch,
                    player.level().random.nextLong(),
                ),
            )
        }
    }

    private fun playSoundToParticipants(
        server: MinecraftServer,
        sound: SoundEvent,
        volume: Float = 1.0f,
        pitch: Float = 1.0f,
    ) {
        playSoundToParticipants(server, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), volume, pitch)
    }

    private fun registerDisconnect() {
        ServerPlayConnectionEvents.DISCONNECT.register { handler, server ->
            disguiseManager.clearDisguise(handler.player)
            BlueMapCompat.setPlayerVisibility(handler.player.uuid, true)
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
            RouletteManager.clear()
            disguiseManager.clearAll(server)
            val participants = listOfNotNull(game.hiderUuid, game.seekerUuid)
            BlueMapCompat.setPlayersVisibility(participants, true)
        }
    }

    private fun registerRouletteFinished() {
        ServerPlayNetworking.registerGlobalReceiver(RouletteFinishedPayload.TYPE) { _, ctx ->
            ctx.server().execute {
                RouletteManager.applyDisguise(ctx.player(), disguiseManager)
            }
        }
    }
}
