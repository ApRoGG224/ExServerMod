package com.fome.exservermod.disguise

import com.fome.exservermod.network.DisguiseSyncPayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
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

    fun broadcastDisguiseToTrackers(
        player: ServerPlayer,
        mobType: EntityType<*>,
    ) {
        val server = player.level().server
        for (other in server.playerList.players) {
            if (other != player && other.level() == player.level()) {
                other.connection.send(ClientboundRemoveEntitiesPacket(player.id))

                other.connection.send(
                    ClientboundAddEntityPacket(
                        player.id,
                        player.uuid,
                        player.x,
                        player.y,
                        player.z,
                        player.xRot,
                        player.yRot,
                        mobType,
                        0,
                        player.deltaMovement,
                        player.yHeadRot.toDouble(),
                    ),
                )

                other.connection.send(
                    ClientboundRotateHeadPacket(
                        player,
                        (player.yHeadRot * 256.0f / 360.0f).toInt().toByte(),
                    ),
                )

                val values = player.entityData.nonDefaultValues
                if (values != null) {
                    val filtered = values.filter { it.id() <= 13 }
                    if (filtered.isNotEmpty()) {
                        other.connection.send(ClientboundSetEntityDataPacket(player.id, filtered))
                    }
                }
            }
        }
    }

    fun broadcastRestoreToTrackers(player: ServerPlayer) {
        val server = player.level().server
        for (other in server.playerList.players) {
            if (other != player && other.level() == player.level()) {
                other.connection.send(ClientboundRemoveEntitiesPacket(player.id))

                other.connection.send(
                    ClientboundAddEntityPacket(
                        player.id,
                        player.uuid,
                        player.x,
                        player.y,
                        player.z,
                        player.xRot,
                        player.yRot,
                        player.type,
                        0,
                        player.deltaMovement,
                        player.yHeadRot.toDouble(),
                    ),
                )

                other.connection.send(
                    ClientboundRotateHeadPacket(
                        player,
                        (player.yHeadRot * 256.0f / 360.0f).toInt().toByte(),
                    ),
                )

                val values = player.entityData.nonDefaultValues
                if (values != null) {
                    other.connection.send(ClientboundSetEntityDataPacket(player.id, values))
                }
            }
        }
    }

    fun applyDisguise(
        player: ServerPlayer,
        mobType: EntityType<*>,
    ) {
        recordDisguise(player.uuid, mobType)

        player.getAttribute(Attributes.SCALE)?.baseValue = 1.0
        player.refreshDimensions()

        if (MobPool.getOrigin(mobType) == Origin.AQUATIC) {
            player.addEffect(MobEffectInstance(MobEffects.WATER_BREATHING, -1, 0, false, false, false))
            player.airSupply = player.maxAirSupply
        }

        broadcastDisguiseToTrackers(player, mobType)

        val mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mobType).toString()
        val payload = DisguiseSyncPayload(player.uuid, mobId, true)
        val server = player.level().server
        for (p in server.playerList.players) {
            ServerPlayNetworking.send(p, payload)
        }

        player.sendSystemMessage(
            Component
                .translatable("exservermod.disguise.you_are_disguised", mobType.description)
                .withStyle(ChatFormatting.GREEN),
        )
    }

    fun clearDisguise(player: ServerPlayer) {
        if (!isDisguised(player.uuid)) return
        removeDisguise(player.uuid)
        player.getAttribute(Attributes.SCALE)?.baseValue = 1.0
        player.refreshDimensions()
        player.removeEffect(MobEffects.WATER_BREATHING)
        player.airSupply = player.maxAirSupply

        broadcastRestoreToTrackers(player)

        val payload = DisguiseSyncPayload(player.uuid, "", false)
        val server = player.level().server
        for (p in server.playerList.players) {
            ServerPlayNetworking.send(p, payload)
        }
    }

    fun clearAll(server: MinecraftServer) {
        for (uuid in disguises.keys) {
            val player = server.playerList.getPlayer(uuid)
            if (player != null) {
                player.getAttribute(Attributes.SCALE)?.baseValue = 1.0
                player.refreshDimensions()
                player.removeEffect(MobEffects.WATER_BREATHING)
                player.airSupply = player.maxAirSupply
                broadcastRestoreToTrackers(player)
            }
            val payload = DisguiseSyncPayload(uuid, "", false)
            for (p in server.playerList.players) {
                ServerPlayNetworking.send(p, payload)
            }
        }
        clearAllDisguises()
    }

    fun checkAntiStuck(player: ServerPlayer): Boolean {
        if (!player.isInWall) return false

        val currentPos = player.blockPosition()
        val safePos = currentPos.above(2)
        player.teleportTo(
            safePos.x + 0.5,
            safePos.y.toDouble(),
            safePos.z + 0.5,
        )
        player.sendSystemMessage(
            Component
                .translatable("exservermod.disguise.anti_stuck_relocated")
                .withStyle(ChatFormatting.YELLOW),
        )
        return true
    }
}
