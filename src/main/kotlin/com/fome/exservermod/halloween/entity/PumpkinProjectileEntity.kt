package com.fome.exservermod.halloween.entity

import com.fome.exservermod.halloween.HalloweenEntities
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult

class PumpkinProjectileEntity : ThrowableItemProjectile {
    constructor(type: EntityType<out PumpkinProjectileEntity>, level: Level) : super(type, level)

    constructor(level: Level, owner: LivingEntity) : super(
        HalloweenEntities.PUMPKIN_PROJECTILE,
        owner,
        level,
        ItemStack(Items.CARVED_PUMPKIN),
    )

    override fun getDefaultItem(): Item = Items.CARVED_PUMPKIN

    override fun onHit(result: HitResult) {
        super.onHit(result)

        val sLevel = level() as? ServerLevel
        if (sLevel != null) {
            val center = position()
            val radius = 3.5

            // Orange explosion smoke and sparks
            sLevel.sendParticles(
                ParticleTypes.FLAME,
                center.x,
                center.y + 0.2,
                center.z,
                25,
                0.6,
                0.6,
                0.6,
                0.08,
            )
            sLevel.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                center.x,
                center.y + 0.2,
                center.z,
                15,
                0.5,
                0.5,
                0.5,
                0.04,
            )

            sLevel.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS,
                1.5f,
                0.8f,
            )
            sLevel.playSound(
                null,
                center.x,
                center.y,
                center.z,
                SoundEvents.WOOD_BREAK,
                SoundSource.PLAYERS,
                1.2f,
                0.7f,
            )

            // AoE damage without block griefing
            val box = AABB.ofSize(center, radius * 2, radius * 2, radius * 2)
            val targets = sLevel.getEntitiesOfClass(LivingEntity::class.java, box)
            val projectileOwner = getOwner()
            val damageSource = sLevel.damageSources().thrown(this, projectileOwner)

            for (target in targets) {
                if (target == projectileOwner) continue
                val dist = target.position().distanceTo(center)
                if (dist <= radius) {
                    val falloff = (1.0 - (dist / radius)).coerceIn(0.2, 1.0)
                    val damage = (14.0 * falloff).toFloat()
                    target.hurtServer(sLevel, damageSource, damage)
                }
            }
            discard()
        }
    }
}
