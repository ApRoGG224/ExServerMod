package com.fome.exservermod.halloween.entity

import com.fome.exservermod.halloween.HalloweenEvent
import com.fome.exservermod.halloween.HalloweenPhase
import net.minecraft.ChatFormatting
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class DemonEyeEntity(
    type: EntityType<out DemonEyeEntity>,
    level: Level,
) : Monster(type, level) {
    companion object {
        val DATA_VARIANT: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(DemonEyeEntity::class.java, EntityDataSerializers.INT)

        fun createAttributes(): AttributeSupplier.Builder =
            Monster
                .createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 5.0)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32.0)
    }

    private var diveCooldownTicks: Int = 40
    private var isDiving: Boolean = false
    private var diveTicksLeft: Int = 0
    private var targetDivePos: Vec3? = null

    init {
        isNoGravity = true
        noPhysics = false
    }

    fun getVariant(): Int = entityData.get(DATA_VARIANT)

    fun setVariant(variant: Int) {
        entityData.set(DATA_VARIANT, variant.coerceIn(0, 3))
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_VARIANT, Random.nextInt(4))
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        super.readAdditionalSaveData(input)
        setVariant(input.getIntOr("Variant", 0))
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        super.addAdditionalSaveData(output)
        output.putInt("Variant", getVariant())
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.PHANTOM_AMBIENT

    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.PHANTOM_HURT

    override fun getDeathSound(): SoundEvent = SoundEvents.PHANTOM_DEATH

    override fun tick() {
        super.tick()
        isNoGravity = true

        val sLevel = level() as? ServerLevel ?: return

        // Blood trail in flight
        if (tickCount % 2 == 0) {
            val mov = deltaMovement
            val trailOffset = if (mov.lengthSqr() > 0.001) mov.normalize().scale(-0.4) else Vec3.ZERO
            sLevel.sendParticles(
                ParticleTypes.CRIMSON_SPORE,
                x + trailOffset.x,
                y + 0.35 + trailOffset.y,
                z + trailOffset.z,
                2,
                0.06,
                0.06,
                0.06,
                0.01,
            )
            sLevel.sendParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                x + trailOffset.x,
                y + 0.35 + trailOffset.y,
                z + trailOffset.z,
                1,
                0.03,
                0.03,
                0.03,
                0.0,
            )
        }

        // Target nearest player within 32 blocks
        var currentTarget = target
        if (currentTarget == null || !currentTarget.isAlive || currentTarget.distanceToSqr(this) > 1024.0) {
            currentTarget = sLevel.getNearestPlayer(this, 32.0)
            target = currentTarget
        }

        if (currentTarget != null && currentTarget.isAlive) {
            val distToTarget = position().distanceTo(currentTarget.position())

            if (isDiving) {
                diveTicksLeft--
                val targetPos = targetDivePos ?: currentTarget.eyePosition
                val diff = targetPos.subtract(position())
                val len = diff.length()

                if (len < 1.3) {
                    // Impact hit! 1 HP pure damage bypasses armor
                    if (currentTarget is ServerPlayer) {
                        currentTarget.hurtServer(sLevel, damageSources().magic(), 1.0f)
                    } else {
                        currentTarget.hurtServer(sLevel, damageSources().magic(), 1.0f)
                    }

                    sLevel.playSound(
                        null,
                        x,
                        y,
                        z,
                        SoundEvents.FOX_BITE,
                        SoundSource.HOSTILE,
                        1.2f,
                        1.0f,
                    )
                    sLevel.sendParticles(
                        ParticleTypes.DAMAGE_INDICATOR,
                        x,
                        y + 0.3,
                        z,
                        10,
                        0.25,
                        0.25,
                        0.25,
                        0.1,
                    )

                    // Bounce back & up
                    val bounce = diff.normalize().scale(-0.45).add(0.0, 0.35, 0.0)
                    setDeltaMovement(bounce)
                    isDiving = false
                    diveCooldownTicks = Random.nextInt(60, 100)
                } else if (diveTicksLeft <= 0 || len > 25.0) {
                    isDiving = false
                    diveCooldownTicks = Random.nextInt(40, 80)
                } else {
                    // Accelerate along dive
                    val diveVelocity = diff.normalize().scale(0.55)
                    setDeltaMovement(diveVelocity)
                    move(MoverType.SELF, diveVelocity)
                }
            } else {
                diveCooldownTicks--
                if (diveCooldownTicks <= 0 && distToTarget < 20.0) {
                    // Start swooping dive!
                    isDiving = true
                    diveTicksLeft = 50
                    targetDivePos = currentTarget.eyePosition

                    sLevel.playSound(
                        null,
                        x,
                        y,
                        z,
                        SoundEvents.PHANTOM_SWOOP,
                        SoundSource.HOSTILE,
                        1.1f,
                        1.2f,
                    )
                } else {
                    // Hover/orbit around target
                    val orbitAngle = (tickCount * 0.05) % (2 * Math.PI)
                    val desiredDist = 7.0
                    val orbitX = currentTarget.x + cos(orbitAngle) * desiredDist
                    val orbitY = currentTarget.y + 3.0 + sin(tickCount * 0.08) * 0.8
                    val orbitZ = currentTarget.z + sin(orbitAngle) * desiredDist
                    val orbitTarget = Vec3(orbitX, orbitY, orbitZ)

                    val diff = orbitTarget.subtract(position())
                    val speed = 0.18
                    val vel = diff.scale(speed)
                    setDeltaMovement(vel)
                    move(MoverType.SELF, vel)
                }
            }

            // Face target / direction
            val dx = currentTarget.x - x
            val dz = currentTarget.z - z
            val dy = currentTarget.eyeY - y
            val horizontalDist = sqrt(dx * dx + dz * dz)
            yRot = (-Math.toDegrees(atan2(dx, dz))).toFloat()
            xRot = (-Math.toDegrees(atan2(dy, horizontalDist))).toFloat()
            yHeadRot = yRot
        } else {
            // Idle hover above ground
            val groundY = sLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ)
            val hoverY = groundY + 4.0 + sin(tickCount * 0.06) * 0.5
            val diffY = hoverY - y
            val idleVel = Vec3(0.0, diffY * 0.1, 0.0)
            setDeltaMovement(idleVel)
            move(MoverType.SELF, idleVel)
        }
    }

    override fun die(damageSource: DamageSource) {
        super.die(damageSource)
        val sLevel = level() as? ServerLevel ?: return
        val killer = damageSource.entity as? ServerPlayer
        val amount = Random.nextInt(1, 4)
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
            y + 0.3,
            z,
            10,
            0.2,
            0.2,
            0.2,
            0.1,
        )
    }
}
