package com.fome.exservermod.disguise

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attributes
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DisguiseManager {
    val disguises: ConcurrentHashMap<UUID, EntityType<*>> = ConcurrentHashMap()

    fun getDisguise(uuid: UUID): EntityType<*>? = disguises[uuid]

    fun isDisguised(uuid: UUID): Boolean = disguises.containsKey(uuid)

    fun recordDisguise(
        uuid: UUID,
        mobType: EntityType<*>,
    ) {
        disguises[uuid] = mobType
    }

    fun removeDisguise(uuid: UUID): EntityType<*>? = disguises.remove(uuid)

    fun clearAllDisguises() {
        disguises.clear()
    }

    fun applyDisguise(
        player: ServerPlayer,
        mobType: EntityType<*>,
    ) {
        recordDisguise(player.uuid, mobType)

        val scale = MobPool.calculateScale(mobType)
        player.getAttribute(Attributes.SCALE)?.baseValue = scale

        player.addEffect(MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false, false))

        if (MobPool.getOrigin(mobType) == Origin.AQUATIC) {
            player.addEffect(MobEffectInstance(MobEffects.WATER_BREATHING, -1, 0, false, false, false))
            player.airSupply = player.maxAirSupply
        }

        player.sendSystemMessage(
            Component
                .translatable("exservermod.disguise.you_are_disguised", mobType.description)
                .withStyle(ChatFormatting.GREEN),
        )
    }

    fun clearDisguise(player: ServerPlayer) {
        removeDisguise(player.uuid)
        player.getAttribute(Attributes.SCALE)?.baseValue = 1.0
        player.removeEffect(MobEffects.INVISIBILITY)
        player.removeEffect(MobEffects.WATER_BREATHING)
        player.airSupply = player.maxAirSupply
    }

    fun clearAll(server: MinecraftServer) {
        for (uuid in disguises.keys) {
            val player = server.playerList.getPlayer(uuid)
            if (player != null) {
                player.getAttribute(Attributes.SCALE)?.baseValue = 1.0
                player.removeEffect(MobEffects.INVISIBILITY)
                player.removeEffect(MobEffects.WATER_BREATHING)
                player.airSupply = player.maxAirSupply
            }
        }
        clearAllDisguises()
    }

    fun checkAntiStuck(player: ServerPlayer): Boolean {
        if (!player.isInWall) return false

        val level = player.level()
        val pos = player.blockPosition()

        for (dy in 0..5) {
            for (dx in -1..1) {
                for (dz in -1..1) {
                    val candidate = pos.offset(dx, dy, dz)
                    val feet = level.getBlockState(candidate)
                    val head = level.getBlockState(candidate.above())
                    val ground = level.getBlockState(candidate.below())

                    if (feet.isAir && head.isAir && !ground.isAir) {
                        player.teleportTo(candidate.x + 0.5, candidate.y.toDouble(), candidate.z + 0.5)
                        player.sendSystemMessage(
                            Component
                                .translatable("exservermod.disguise.anti_stuck_relocated")
                                .withStyle(ChatFormatting.GOLD),
                            true,
                        )
                        return true
                    }
                }
            }
        }
        return false
    }
}
