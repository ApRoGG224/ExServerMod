package com.fome.exservermod.halloween.entity

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.animal.allay.Allay
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class GhostPetEntity(
    type: EntityType<out Allay>,
    level: Level,
) : Allay(type, level) {
    var ownerUuid: UUID? = null

    init {
        noPhysics = true
        isInvulnerable = true
        isCustomNameVisible = true
    }

    override fun hurtServer(
        serverLevel: ServerLevel,
        damageSource: DamageSource,
        f: Float,
    ): Boolean = false

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        val uuidStr = input.getStringOr("OwnerUuid", "")
        if (uuidStr.isNotEmpty()) {
            try {
                ownerUuid = UUID.fromString(uuidStr)
            } catch (_: IllegalArgumentException) {
                // Ignore malformed UUID
            }
        }
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putString("OwnerUuid", ownerUuid?.toString() ?: "")
    }

    override fun tick() {
        super.tick()
        noPhysics = true

        val sLevel = level() as? ServerLevel ?: return
        val currentOwnerUuid = ownerUuid ?: return

        // Ambient soul flame particle
        if (tickCount % 8 == 0) {
            sLevel.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                x,
                y + 0.35,
                z,
                1,
                0.08,
                0.08,
                0.08,
                0.01,
            )
        }

        // Locate owner
        var owner = sLevel.server.playerList.getPlayer(currentOwnerUuid)
        if (owner == null) {
            for (otherLevel in sLevel.server.allLevels) {
                val p = otherLevel.getPlayerByUUID(currentOwnerUuid)
                if (p != null) {
                    teleportTo(otherLevel, p.x, p.y + 1.2, p.z, emptySet(), 0.0f, 0.0f, false)
                    return
                }
            }
            return
        }

        if (owner.level() != sLevel) {
            val targetLevel = owner.level() as? ServerLevel
            if (targetLevel != null) {
                teleportTo(targetLevel, owner.x, owner.y + 1.2, owner.z, emptySet(), 0.0f, 0.0f, false)
                return
            }
        }

        val dist = position().distanceTo(owner.position())
        if (dist > 12.0) {
            teleportTo(owner.x + 0.8, owner.y + 1.2, owner.z + 0.8)
            setDeltaMovement(Vec3.ZERO)
            return
        }

        // Smooth orbital movement within 3 blocks
        val angle = (tickCount * 0.04) % (2 * Math.PI)
        val orbitRadius = 1.6
        val targetX = owner.x + cos(angle) * orbitRadius
        val targetY = owner.y + 1.1 + sin(tickCount * 0.08) * 0.25
        val targetZ = owner.z + sin(angle) * orbitRadius
        val target = Vec3(targetX, targetY, targetZ)

        val direction = target.subtract(position())
        val velocity = direction.scale(0.22)
        setDeltaMovement(velocity)
        move(MoverType.SELF, velocity)

        val dx = owner.x - x
        val dz = owner.z - z
        yRot = (-Math.toDegrees(kotlin.math.atan2(dx, dz))).toFloat()
    }
}
