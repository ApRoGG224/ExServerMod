package com.fome.exservermod.disguise

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import kotlin.math.max
import kotlin.random.Random

object MobPool {
    const val EXPECTED_POOL_SIZE = 34

    val POOL: List<EntityType<*>> =
        listOf(
            EntityTypes.ZOMBIE,
            EntityTypes.SKELETON,
            EntityTypes.CREEPER,
            EntityTypes.SPIDER,
            EntityTypes.CAVE_SPIDER,
            EntityTypes.ENDERMAN,
            EntityTypes.WITCH,
            EntityTypes.ZOMBIE_VILLAGER,
            EntityTypes.DROWNED,
            EntityTypes.HUSK,
            EntityTypes.STRAY,
            EntityTypes.PIGLIN,
            EntityTypes.ZOMBIFIED_PIGLIN,
            EntityTypes.IRON_GOLEM,
            EntityTypes.WARDEN,
            EntityTypes.SNIFFER,
            EntityTypes.COW,
            EntityTypes.SHEEP,
            EntityTypes.PIG,
            EntityTypes.CHICKEN,
            EntityTypes.VILLAGER,
            EntityTypes.HORSE,
            EntityTypes.DONKEY,
            EntityTypes.MULE,
            EntityTypes.LLAMA,
            EntityTypes.WOLF,
            EntityTypes.CAT,
            EntityTypes.OCELOT,
            EntityTypes.FOX,
            EntityTypes.PANDA,
            EntityTypes.POLAR_BEAR,
            EntityTypes.GOAT,
            EntityTypes.CAMEL,
            EntityTypes.ARMADILLO,
        )

    fun getRandomMob(random: Random = Random): EntityType<*> = POOL[random.nextInt(POOL.size)]

    fun calculateScale(
        mobWidth: Float,
        mobHeight: Float,
    ): Double = max(mobWidth / 0.6f, mobHeight / 1.8f).toDouble()

    fun calculateScale(entityType: EntityType<*>): Double = calculateScale(entityType.width, entityType.height)
}
