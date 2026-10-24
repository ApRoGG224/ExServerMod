package com.fome.exservermod.waves

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.item.component.Fireworks
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import java.lang.reflect.Field
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Server-side glue for the wave defense mini-game: spawning vanilla mobs,
 * zero-grief explosion guard, cleanup, rewards, and action bar HUD.
 */
class WaveManager {
    companion object {
        const val SPAWN_RING_MIN = 10.0
        const val SPAWN_RING_MAX = 18.0
        const val CHAMPION_BONUS_HEALTH = 40.0
        const val VICTORY_XP = 250
        const val CLEANUP_CHECK_INTERVAL_TICKS = 20
    }

    private val game = WaveDefenseGame()

    /** Mobs spawned by the current wave, keyed by entity UUID. */
    private val trackedMobs: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /**
     * Zero world damage for creepers: their private explosionRadius field is
     * zeroed via reflection, so they still swell and boom but destroy nothing
     * and hurt no one. If the field ever moves, creepers are skipped entirely.
     */
    private val creeperRadiusField: Field? by lazy {
        try {
            Creeper::class.java.getDeclaredField("explosionRadius").apply { isAccessible = true }
        } catch (_: Throwable) {
            null
        }
    }

    private var arenaCenter: Vec3? = null
    private var arenaLevel: ServerLevel? = null

    fun register() {
        registerTick()
        registerMobDeath()
        registerServerStopping()
    }

    private fun registerTick() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            when (val result = game.tick()) {
                is WaveTickResult.WaveStarted -> spawnWave(server, result)
                is WaveTickResult.Victory -> onVictory(server)
                else -> {}
            }

            if (server.tickCount % CLEANUP_CHECK_INTERVAL_TICKS == 0) {
                reapMissingMobs()
                if (game.isFighting() && trackedMobs.isEmpty()) {
                    onWaveCleared(server)
                }

                if (game.isActive()) {
                    val onlineUuids =
                        server.playerList.players
                            .map { it.uuid }
                            .toSet()
                    if (game.areAllDefendersOffline(onlineUuids)) {
                        abort(server, Component.translatable("exservermod.waves.abort_no_defenders"))
                    }
                }
            }

            if (server.tickCount % WaveDefenseGame.TICKS_PER_SECOND == 0) {
                renderHud(server)
            }
        }
    }

    private fun registerMobDeath() {
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            if (trackedMobs.remove(entity.uuid)) {
                game.registerKill()
            }
        }
    }

    private fun registerServerStopping() {
        ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
            clearMobs()
            game.stop()
        }
    }

    fun tryStart(
        source: ServerPlayer,
        defenders: List<ServerPlayer>,
    ): Boolean {
        if (game.isActive()) {
            return false
        }
        arenaCenter = source.position()
        arenaLevel = source.level() as? ServerLevel ?: return false
        return game.start(defenders.map { it.uuid })
    }

    fun tryStop(server: MinecraftServer): Boolean {
        if (!game.isActive()) {
            return false
        }
        abort(server, Component.translatable("exservermod.waves.stopped_generic"))
        return true
    }

    fun isRunning(): Boolean = game.isActive()

    fun statusLine(): Component =
        Component
            .translatable(
                "exservermod.waves.status",
                game.currentWave.coerceAtLeast(1),
                WaveDefenseGame.TOTAL_WAVES,
                game.kills,
                game.phase.name,
            ).withStyle(ChatFormatting.GOLD)

    private fun abort(
        server: MinecraftServer,
        reason: MutableComponent,
    ) {
        clearMobs()
        game.stop()
        server.playerList.broadcastSystemMessage(reason.withStyle(ChatFormatting.GRAY), false)
    }

    private fun clearMobs() {
        val level = arenaLevel
        if (level != null) {
            for (uuid in trackedMobs) {
                val entity = level.getEntity(uuid)
                if (entity is Mob && entity.isAlive) {
                    entity.discard()
                }
            }
        }
        trackedMobs.clear()
    }

    /** Removes mobs that died without the death event or got unloaded/removed. */
    private fun reapMissingMobs() {
        val level = arenaLevel ?: return
        val iterator = trackedMobs.iterator()
        while (iterator.hasNext()) {
            val uuid = iterator.next()
            val entity = level.getEntity(uuid)
            if (entity == null || !entity.isAlive) {
                iterator.remove()
                game.registerKill()
            }
        }
    }

    private fun spawnWave(
        server: MinecraftServer,
        result: WaveTickResult.WaveStarted,
    ) {
        val level = arenaLevel ?: return
        val center = arenaCenter ?: return

        announceWave(server, result.wave, result.totalWaves, WaveDefenseGame.isBossWave(result.wave))

        var spawnedAny = false
        for (group in result.groups) {
            val mobId =
                Identifier.tryParse(group.mobId) ?: continue
            val mobType = BuiltInRegistries.ENTITY_TYPE.getOptional(mobId).orElse(null) ?: continue
            repeat(group.count) {
                val pos = findSpawnPos(level, center) ?: return@repeat
                val mob = spawnMob(level, mobType, pos, group.isChampion) ?: return@repeat
                trackedMobs.add(mob.uuid)
                spawnedAny = true
            }
        }

        if (!spawnedAny) {
            // Nothing could be spawned (blocked arena) - treat wave as cleared to avoid a stuck fight
            trackedMobs.clear()
            onWaveCleared(server)
        }
    }

    private fun spawnMob(
        level: ServerLevel,
        mobType: EntityType<*>,
        pos: BlockPos,
        isChampion: Boolean,
    ): Mob? {
        val mob = mobType.create(level, EntitySpawnReason.EVENT) as? Mob ?: return null

        // Zero world damage: creepers still swell and pop but destroy nothing
        if (mob is Creeper) {
            val field = creeperRadiusField
            if (field == null) {
                // Cannot guarantee a harmless explosion - skip this creeper
                return null
            }
            field.setInt(mob, 0)
        }

        mob.setPos(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        mob.yRot = Random.nextFloat() * 360f
        mob.xRot = 0f
        mob.setPersistenceRequired()

        if (isChampion) {
            mob.setCustomName(Component.translatable("exservermod.waves.champion_name").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
            mob.setCustomNameVisible(true)
            mob.getAttribute(Attributes.MAX_HEALTH)?.let { attribute ->
                attribute.baseValue = CHAMPION_BONUS_HEALTH
            }
            mob.setHealth(CHAMPION_BONUS_HEALTH.toFloat())

            val gear =
                mapOf(
                    EquipmentSlot.HEAD to Items.DIAMOND_HELMET,
                    EquipmentSlot.CHEST to Items.DIAMOND_CHESTPLATE,
                    EquipmentSlot.LEGS to Items.DIAMOND_LEGGINGS,
                    EquipmentSlot.FEET to Items.DIAMOND_BOOTS,
                    EquipmentSlot.MAINHAND to Items.DIAMOND_SWORD,
                )
            for ((slot, item) in gear) {
                mob.setItemSlot(slot, ItemStack(item))
                // No economy impact: champions never drop their gear
                mob.setDropChance(slot, 0.0f)
            }
        }

        level.addFreshEntity(mob)
        return mob
    }

    private fun findSpawnPos(
        level: ServerLevel,
        center: Vec3,
    ): BlockPos? {
        repeat(12) {
            val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
            val dist = Random.nextDouble(SPAWN_RING_MIN, SPAWN_RING_MAX)
            val x = (center.x + dist * cos(angle)).toInt()
            val z = (center.z + dist * sin(angle)).toInt()

            val surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
            // Surface spawn is fine for open arenas...
            if (abs(surfaceY - center.y) <= 8.0) {
                val pos = BlockPos(x, surfaceY, z)
                if (isSpawnable(level, pos)) {
                    return pos
                }
                return@repeat
            }

            // ...underground arenas spawn near the center instead
            for (dy in -3..3) {
                val candidate = BlockPos(x, center.y.toInt() + dy, z)
                if (isSpawnable(level, candidate)) {
                    return candidate
                }
            }
        }
        return null
    }

    private fun isSpawnable(
        level: ServerLevel,
        pos: BlockPos,
    ): Boolean {
        val floor = level.getBlockState(pos.below())
        return floor.isFaceSturdy(level, pos.below(), Direction.UP) &&
            level.getBlockState(pos).isAir &&
            level.getBlockState(pos.above()).isAir
    }

    private fun onWaveCleared(server: MinecraftServer) {
        when (val cleared = game.onWaveCleared()) {
            is WaveTickResult.Victory -> Unit // handled via tick() victory branch
            is WaveTickResult.PhaseChanged -> {
                if (cleared.newPhase == WavePhase.INTERMISSION) {
                    server.playerList.broadcastSystemMessage(
                        Component
                            .translatable(
                                "exservermod.waves.wave_cleared",
                                game.currentWave,
                                WaveDefenseGame.INTERMISSION_SECONDS,
                            ).withStyle(ChatFormatting.GREEN),
                        false,
                    )
                    playSoundToDefenders(server, SoundEvents.PLAYER_LEVELUP, 0.9f, 1.2f)
                }
            }
            else -> {}
        }
    }

    private fun onVictory(server: MinecraftServer) {
        val level = arenaLevel
        val center = arenaCenter

        clearMobs()

        val defenders = game.defenders.mapNotNull { server.playerList.getPlayer(it) }
        for (defender in defenders) {
            val anim = ClientboundSetTitlesAnimationPacket(10, 70, 20)
            val title =
                ClientboundSetTitleTextPacket(
                    Component.translatable("exservermod.waves.victory").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                )
            val subtitle =
                ClientboundSetSubtitleTextPacket(
                    Component
                        .translatable("exservermod.waves.victory_subtitle", WaveDefenseGame.TOTAL_WAVES, game.kills)
                        .withStyle(ChatFormatting.YELLOW),
                )
            defender.connection.send(anim)
            defender.connection.send(title)
            defender.connection.send(subtitle)
        }

        server.playerList.broadcastSystemMessage(
            Component
                .translatable("exservermod.waves.victory_chat", game.kills)
                .withStyle(ChatFormatting.GOLD),
            false,
        )

        if (level != null && center != null) {
            val fireworkStack = ItemStack(Items.FIREWORK_ROCKET)
            val explosions =
                listOf(
                    FireworkExplosion(
                        FireworkExplosion.Shape.LARGE_BALL,
                        IntArrayList(intArrayOf(0xFFD700, 0x00FF88, 0x00E5FF)),
                        IntArrayList(intArrayOf(0xFFFFFF)),
                        true,
                        true,
                    ),
                )
            fireworkStack.set(DataComponents.FIREWORKS, Fireworks(1, explosions))
            val firework = FireworkRocketEntity(level, center.x, center.y + 1.5, center.z, fireworkStack)
            level.addFreshEntity(firework)

            ExperienceOrb.award(level, center.add(0.0, 1.0, 0.0), VICTORY_XP)
            level.playSound(null, center.x, center.y, center.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 1.0f)
        }

        playSoundToDefenders(server, SoundEvents.PLAYER_LEVELUP, 1.2f, 1.0f)
    }

    private fun announceWave(
        server: MinecraftServer,
        wave: Int,
        totalWaves: Int,
        isBoss: Boolean,
    ) {
        val key = if (isBoss) "exservermod.waves.wave_announce_boss" else "exservermod.waves.wave_announce"
        val style = if (isBoss) ChatFormatting.RED to ChatFormatting.BOLD else ChatFormatting.YELLOW to ChatFormatting.BOLD
        server.playerList.broadcastSystemMessage(
            Component.translatable(key, wave, totalWaves).withStyle(style.first, style.second),
            false,
        )
        if (isBoss) {
            playSoundToDefenders(server, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 1.0f)
        } else {
            playSoundToDefenders(server, SoundEvents.RAID_HORN, 1.5f, 1.0f)
        }
    }

    private fun renderHud(server: MinecraftServer) {
        val defenders = game.defenders.mapNotNull { uuid -> server.playerList.getPlayer(uuid) }
        if (defenders.isEmpty()) {
            return
        }

        val message =
            when (game.phase) {
                WavePhase.COUNTDOWN -> {
                    val secondsLeft = game.remainingTicks / WaveDefenseGame.TICKS_PER_SECOND
                    if (secondsLeft in 1..5) {
                        playSoundToDefenders(server, SoundEvents.NOTE_BLOCK_PLING, 0.8f, 1.1f)
                    }
                    Component
                        .translatable("exservermod.waves.actionbar.countdown", secondsLeft)
                        .withStyle(ChatFormatting.YELLOW)
                }
                WavePhase.FIGHT ->
                    Component
                        .translatable(
                            "exservermod.waves.actionbar.fight",
                            game.currentWave,
                            WaveDefenseGame.TOTAL_WAVES,
                            trackedMobs.size,
                            game.kills,
                        ).withStyle(ChatFormatting.RED)
                WavePhase.INTERMISSION -> {
                    val secondsLeft = game.remainingTicks / WaveDefenseGame.TICKS_PER_SECOND
                    Component
                        .translatable("exservermod.waves.actionbar.intermission", game.currentWave + 1, secondsLeft)
                        .withStyle(ChatFormatting.GREEN)
                }
                else -> return
            }

        for (defender in defenders) {
            defender.sendSystemMessage(message, true)
        }
    }

    private fun playSoundToDefenders(
        server: MinecraftServer,
        sound: Holder<SoundEvent>,
        volume: Float,
        pitch: Float,
    ) {
        for (uuid in game.defenders) {
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

    private fun playSoundToDefenders(
        server: MinecraftServer,
        sound: SoundEvent,
        volume: Float,
        pitch: Float,
    ) {
        playSoundToDefenders(server, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), volume, pitch)
    }

    /**
     * Debug/diagnostic helper: number of currently tracked wave mobs.
     */
    fun trackedMobCount(): Int = trackedMobs.size
}
