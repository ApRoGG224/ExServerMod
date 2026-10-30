package com.fome.exservermod.halloween.entity

import com.fome.exservermod.halloween.HalloweenEvent
import com.fome.exservermod.halloween.HalloweenPhase
import net.minecraft.ChatFormatting
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.Level
import kotlin.random.Random

class BloodZombieEntity(
    type: EntityType<out BloodZombieEntity>,
    level: Level,
) : Zombie(type, level) {
    override fun isSunSensitive(): Boolean = false

    override fun tick() {
        super.tick()
        val sLevel = level() as? ServerLevel ?: return
        if (tickCount % 8 == 0) {
            sLevel.sendParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                x,
                y + 1.1,
                z,
                1,
                0.15,
                0.3,
                0.15,
                0.01,
            )
        }
    }

    override fun doHurtTarget(
        serverLevel: ServerLevel,
        target: Entity,
    ): Boolean {
        val hit = super.doHurtTarget(serverLevel, target)
        if (hit && target is ServerPlayer) {
            val stealAmount = Random.nextInt(5, 16)
            val stolen = HalloweenEvent.balanceManager?.removeCandies(target, stealAmount) ?: 0
            if (stolen > 0) {
                target.sendSystemMessage(
                    Component.translatable("exservermod.halloween.candy_stolen", stolen).withStyle(ChatFormatting.RED),
                    true,
                )
                serverLevel.playSound(
                    null,
                    target.x,
                    target.y,
                    target.z,
                    SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                    SoundSource.HOSTILE,
                    1.0f,
                    1.4f,
                )
            }
        }
        return hit
    }

    override fun die(damageSource: DamageSource) {
        super.die(damageSource)
        val sLevel = level() as? ServerLevel ?: return
        val killer = damageSource.entity as? ServerPlayer
        val amount = Random.nextInt(2, 6)
        if (killer != null) {
            val newBal = HalloweenEvent.balanceManager?.addCandies(killer, amount, sendActionBar = true) ?: 0
            val timeLeft =
                if (HalloweenEvent.instance?.phase == HalloweenPhase.ACTIVE_GATHERING) {
                    " | ⏱ ${HalloweenEvent.instance?.formattedTimeLeft()}"
                } else {
                    ""
                }
            killer.sendSystemMessage(
                Component
                    .translatable("exservermod.halloween.candy_mob_kill", amount, newBal, timeLeft)
                    .withStyle(ChatFormatting.GOLD),
                true,
            )
        }
        sLevel.sendParticles(
            ParticleTypes.DAMAGE_INDICATOR,
            x,
            y + 1.0,
            z,
            12,
            0.3,
            0.5,
            0.3,
            0.1,
        )
    }
}
