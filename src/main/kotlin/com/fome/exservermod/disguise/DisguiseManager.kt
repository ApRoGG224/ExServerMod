package com.fome.exservermod.disguise

import com.fome.exservermod.network.DisguiseSyncPayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.Vec3
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

        if (isPlayerStuck(player)) {
            unstuckPlayer(player, isAutomatic = true)
        }

        val unstuckBtn =
            Component.translatable("exservermod.disguise.button.unstuck").withStyle { style ->
                style
                    .withColor(ChatFormatting.AQUA)
                    .withClickEvent(ClickEvent.RunCommand("/games unstuck"))
                    .withHoverEvent(
                        HoverEvent.ShowText(Component.translatable("exservermod.disguise.button.unstuck.hover")),
                    )
            }

        player.sendSystemMessage(
            Component
                .translatable("exservermod.disguise.you_are_disguised", mobType.description)
                .withStyle(ChatFormatting.GREEN)
                .append(" ")
                .append(unstuckBtn),
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

    companion object {
        const val UNSTUCK_COOLDOWN_MS = 20_000L
        const val MAX_UNSTUCK_PER_ROUND = 3
    }

    private val unstuckCooldowns = ConcurrentHashMap<UUID, Long>()
    private val unstuckCounts = ConcurrentHashMap<UUID, Int>()

    fun resetUnstuckCounters() {
        unstuckCooldowns.clear()
        unstuckCounts.clear()
    }

    fun isPlayerStuck(player: ServerPlayer): Boolean {
        val level = player.level()
        return !level.noCollision(player, player.boundingBox) || player.isInWall
    }

    fun findSafePosition(player: ServerPlayer): Vec3? {
        val level = player.level()
        val dims = player.getDimensions(player.pose)
        val center = player.position()
        val basePos = player.blockPosition()
        val mobType = getDisguise(player.uuid)
        val isAquatic = mobType != null && MobPool.getOrigin(mobType) == Origin.AQUATIC

        var bestPos: Vec3? = null
        var bestDistSq = Double.MAX_VALUE

        for (r in 0..5) {
            for (dx in -r..r) {
                for (dz in -r..r) {
                    if (r > 0 && maxOf(Math.abs(dx), Math.abs(dz)) != r) continue

                    for (dy in -2..3) {
                        val candidateBlock = basePos.offset(dx, dy, dz)
                        val testX = candidateBlock.x + 0.5
                        val testY = candidateBlock.y.toDouble()
                        val testZ = candidateBlock.z + 0.5

                        val floorPos = candidateBlock.below()
                        val floorState = level.getBlockState(floorPos)
                        if (!floorState.isFaceSturdy(level, floorPos, Direction.UP)) continue
                        if (floorState.block == Blocks.LAVA || floorState.block == Blocks.MAGMA_BLOCK) continue

                        val candidateBox = dims.makeBoundingBox(testX, testY, testZ)
                        if (!level.noCollision(player, candidateBox)) continue

                        val feetFluid = level.getFluidState(candidateBlock)
                        if (feetFluid.isSourceOfType(Fluids.LAVA)) continue

                        val headPos = BlockPos.containing(testX, testY + dims.eyeHeight(), testZ)
                        val headFluid = level.getFluidState(headPos)
                        if (headFluid.isSourceOfType(Fluids.LAVA)) continue

                        if (!isAquatic && (!feetFluid.isEmpty || !headFluid.isEmpty)) continue

                        val distSq = center.distanceToSqr(testX, testY, testZ)
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq
                            bestPos = Vec3(testX, testY, testZ)
                        }
                    }
                }
            }
            if (bestPos != null) return bestPos
        }
        return bestPos
    }

    fun unstuckPlayer(
        player: ServerPlayer,
        isAutomatic: Boolean = false,
        isSeekingPhase: Boolean = false,
    ): Boolean {
        val now = System.currentTimeMillis()

        if (!isAutomatic) {
            if (isSeekingPhase && !isPlayerStuck(player)) {
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.disguise.not_stuck")
                        .withStyle(ChatFormatting.RED),
                )
                return false
            }

            val lastUsed = unstuckCooldowns[player.uuid] ?: 0L
            val elapsed = now - lastUsed
            if (elapsed < UNSTUCK_COOLDOWN_MS) {
                val remainingSec = ((UNSTUCK_COOLDOWN_MS - elapsed) / 1000).coerceAtLeast(1)
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.disguise.unstuck_cooldown", remainingSec)
                        .withStyle(ChatFormatting.RED),
                )
                return false
            }

            val count = unstuckCounts[player.uuid] ?: 0
            if (count >= MAX_UNSTUCK_PER_ROUND) {
                player.sendSystemMessage(
                    Component
                        .translatable("exservermod.disguise.unstuck_limit", MAX_UNSTUCK_PER_ROUND)
                        .withStyle(ChatFormatting.RED),
                )
                return false
            }
        }

        val safePos = findSafePosition(player)
        if (safePos == null) {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.disguise.unstuck_no_spot")
                    .withStyle(ChatFormatting.RED),
            )
            return false
        }

        val level = player.level()
        level.sendParticles(ParticleTypes.PORTAL, player.x, player.y + 0.8, player.z, 25, 0.3, 0.5, 0.3, 0.1)
        player.teleportTo(safePos.x, safePos.y, safePos.z)
        level.sendParticles(ParticleTypes.PORTAL, safePos.x, safePos.y + 0.8, safePos.z, 25, 0.3, 0.5, 0.3, 0.1)
        level.playSound(null, safePos.x, safePos.y, safePos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f)

        if (!isAutomatic) {
            unstuckCooldowns[player.uuid] = now
            unstuckCounts[player.uuid] = (unstuckCounts[player.uuid] ?: 0) + 1
        }

        player.sendSystemMessage(
            Component
                .translatable("exservermod.disguise.anti_stuck_relocated")
                .withStyle(ChatFormatting.YELLOW),
        )
        return true
    }
}
