package com.fome.exservermod.disguise

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap
import net.minecraft.world.entity.EntityTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DisguiseTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setupMinecraft() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    @Test
    fun `test mob pool contains exactly 34 curated mobs`() {
        assertEquals(34, MobPool.POOL.size)
        assertEquals(34, MobPool.POOL.distinct().size)
    }

    @Test
    fun `test mob pool excludes bosses and flying entities`() {
        val forbidden =
            listOf(
                EntityTypes.ENDER_DRAGON,
                EntityTypes.WITHER,
                EntityTypes.ELDER_GUARDIAN,
                EntityTypes.PHANTOM,
                EntityTypes.BAT,
                EntityTypes.GHAST,
                EntityTypes.ALLAY,
                EntityTypes.BEE,
                EntityTypes.BLAZE,
                EntityTypes.PARROT,
                EntityTypes.VEX,
            )

        for (entityType in forbidden) {
            assertFalse(
                MobPool.POOL.contains(entityType),
                "MobPool must not contain forbidden entity: $entityType",
            )
        }
    }

    @Test
    fun `test calculateScale formula`() {
        // Standard player dimensions: 0.6 width, 1.8 height -> scale 1.0
        val playerScale = MobPool.calculateScale(0.6f, 1.8f)
        assertEquals(1.0, playerScale, 1e-4)

        // Wider entity: width 1.4f, height 2.7f -> max(1.4/0.6, 2.7/1.8) = max(2.3333, 1.5) = 2.3333
        val ironGolemScale = MobPool.calculateScale(1.4f, 2.7f)
        assertEquals(1.4 / 0.6, ironGolemScale, 1e-4)

        // Taller entity: width 0.6f, height 2.9f -> max(0.6/0.6, 2.9/1.8) = max(1.0, 1.6111) = 1.6111
        val endermanScale = MobPool.calculateScale(0.6f, 2.9f)
        assertEquals(2.9 / 1.8, endermanScale, 1e-4)

        // Smaller entity: width 0.4f, height 0.7f -> max(0.4/0.6, 0.7/1.8) = max(0.6666, 0.3888) = 0.6666
        val chickenScale = MobPool.calculateScale(0.4f, 0.7f)
        assertEquals(0.4 / 0.6, chickenScale, 1e-4)
    }

    @Test
    fun `test getRandomMob returns elements from pool`() {
        val random = Random(42)
        val selected = mutableSetOf<Any>()
        repeat(100) {
            val mob = MobPool.getRandomMob(random)
            assertTrue(MobPool.POOL.contains(mob))
            selected.add(mob)
        }
        assertTrue(selected.size > 1, "Random selection should sample different mobs")
    }
}
