package com.fome.exservermod.game

import com.fome.exservermod.disguise.DisguiseManager
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.item.component.Fireworks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object RouletteManager {
    private val pendingDisguises = ConcurrentHashMap<UUID, EntityType<*>>()
    private val pendingTimestamps = ConcurrentHashMap<UUID, Long>()

    private const val TIMEOUT_MS = 15000L

    fun queueDisguise(
        uuid: UUID,
        mobType: EntityType<*>,
    ) {
        pendingDisguises[uuid] = mobType
        pendingTimestamps[uuid] = System.currentTimeMillis()
    }

    fun hasPending(uuid: UUID): Boolean = pendingDisguises.containsKey(uuid)

    fun applyDisguise(
        player: ServerPlayer,
        disguiseManager: DisguiseManager,
    ): Boolean {
        val mobType = pendingDisguises.remove(player.uuid) ?: return false
        pendingTimestamps.remove(player.uuid)

        disguiseManager.applyDisguise(player, mobType)
        spawnCelebrationEffects(player)
        return true
    }

    fun checkTimeouts(
        server: MinecraftServer,
        disguiseManager: DisguiseManager,
    ) {
        val now = System.currentTimeMillis()
        for ((uuid, timestamp) in pendingTimestamps) {
            if (now - timestamp > TIMEOUT_MS) {
                val player = server.playerList.getPlayer(uuid)
                if (player != null) {
                    applyDisguise(player, disguiseManager)
                } else {
                    pendingDisguises.remove(uuid)
                    pendingTimestamps.remove(uuid)
                }
            }
        }
    }

    fun clear() {
        pendingDisguises.clear()
        pendingTimestamps.clear()
    }

    private fun spawnCelebrationEffects(player: ServerPlayer) {
        val level = player.level()
        val x = player.x
        val y = player.y
        val z = player.z

        // Visual and auditory celebration fireworks
        val fireworkStack = ItemStack(Items.FIREWORK_ROCKET)
        val explosions =
            listOf(
                FireworkExplosion(
                    FireworkExplosion.Shape.LARGE_BALL,
                    IntArrayList(intArrayOf(0xFFD700, 0xFFA500, 0x00FF88)),
                    IntArrayList(intArrayOf(0xFFFFFF)),
                    true,
                    true,
                ),
            )
        fireworkStack.set(DataComponents.FIREWORKS, Fireworks(1, explosions))
        val firework = FireworkRocketEntity(level, x, y + 1.2, z, fireworkStack)
        level.addFreshEntity(firework)

        level.sendParticles(ParticleTypes.FIREWORK, x, y + 1.0, z, 45, 0.5, 0.7, 0.5, 0.12)
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0, z, 35, 0.4, 0.6, 0.4, 0.2)

        level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.85f, 1.25f)
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0f, 1.0f)
        level.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0f, 1.2f)
    }
}
