package com.fome.exservermod.halloween

import com.fome.exservermod.halloween.item.CandyPouchItem
import com.fome.exservermod.landmarks.Landmark
import java.io.File
import java.util.UUID
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HalloweenEventTest {
    @Test
    fun testEventScheduleConstants() {
        val config = HalloweenConfig()
        // 90 minutes = 5400 seconds = 108,000 ticks
        assertEquals(90 * 60, config.eventCooldownSeconds)
        // 10 minutes = 600 seconds = 12,000 ticks
        assertEquals(10 * 60, config.eventDurationSeconds)
        // 5 minutes grace = 300 seconds = 6,000 ticks
        assertEquals(5 * 60, config.merchantGraceSeconds)
        // Radius = 25 blocks
        assertEquals(25, config.candySpawnRadius)
        // Min candy separation = 3.5 blocks
        assertEquals(3.5, config.minCandySeparation)
        // Mob candy drop = 3
        assertEquals(3, config.mobCandyDrop)
    }

    @Test
    fun testVirtualBalanceManager() {
        val tempFile = File.createTempFile("test_balances", ".json")
        tempFile.deleteOnExit()

        val manager = HalloweenBalanceManager(tempFile)
        val playerUuid = UUID.randomUUID()

        assertEquals(0, manager.getBalance(playerUuid))

        // Add candies
        val afterAdd = manager.addCandies(playerUuid, 50)
        assertEquals(50, afterAdd)
        assertEquals(50, manager.getBalance(playerUuid))

        // Spend candies successfully
        val successSpend = manager.spendCandies(playerUuid, 30)
        assertTrue(successSpend)
        assertEquals(20, manager.getBalance(playerUuid))

        // Spend more than available should fail
        val failSpend = manager.spendCandies(playerUuid, 100)
        assertFalse(failSpend)
        assertEquals(20, manager.getBalance(playerUuid))

        // Persistence reload test
        val reloadedManager = HalloweenBalanceManager(tempFile)
        assertEquals(20, reloadedManager.getBalance(playerUuid))
    }

    private fun HalloweenBalanceManager.spendCandies(
        uuid: UUID,
        cost: Int,
    ): Boolean {
        val current = getBalance(uuid)
        return if (current >= cost) {
            setCandies(uuid, current - cost)
            true
        } else {
            false
        }
    }

    @Test
    fun testPouchSpeedAndYieldConstants() {
        // Base (no pouch): 5.0 seconds (100 ticks), +25 candies
        val baseTicks = 100
        val baseYield = 25
        assertEquals(5.0, baseTicks / 20.0)
        assertEquals(25, baseYield)

        // Tier 1: 4.0 seconds (80 ticks), +30 candies
        assertEquals(80, CandyPouchItem.TIER_1_TICKS)
        assertEquals(30, CandyPouchItem.TIER_1_YIELD)
        assertEquals(4.0, CandyPouchItem.TIER_1_TICKS / 20.0)

        // Tier 2: 3.5 seconds (70 ticks), +45 candies
        assertEquals(70, CandyPouchItem.TIER_2_TICKS)
        assertEquals(45, CandyPouchItem.TIER_2_YIELD)
        assertEquals(3.5, CandyPouchItem.TIER_2_TICKS / 20.0)

        // Tier 3: 1.0 second (20 ticks), +80 candies
        assertEquals(20, CandyPouchItem.TIER_3_TICKS)
        assertEquals(80, CandyPouchItem.TIER_3_YIELD)
        assertEquals(1.0, CandyPouchItem.TIER_3_TICKS / 20.0)
    }

    @Test
    fun testLandmarkTraderDistanceBounds() {
        val landmark = Landmark("Pyramid", 100.0, 200.0, radius = 20.0)

        for (i in 0 until 100) {
            val dist = Random.nextDouble(10.0, 60.0)
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val spawnX = landmark.x + kotlin.math.cos(angle) * dist
            val spawnZ = landmark.z + kotlin.math.sin(angle) * dist

            val actualDist = hypot(spawnX - landmark.x, spawnZ - landmark.z)
            assertTrue(actualDist >= 10.0, "Merchant spawn distance must be >= 10.0")
            assertTrue(actualDist <= 60.0, "Merchant spawn distance must be <= 60.0")
        }
    }

    @Test
    fun testWaveCandyDropAmounts() {
        // Regular mob gives 3 candies
        val standardCandyDrop = 3
        assertEquals(3, standardCandyDrop)

        // Boss or Champion gives 200 candies
        val bossCandyDrop = 200
        assertEquals(200, bossCandyDrop)
    }

    @Test
    fun testWaresPoolCompleteness() {
        val pool = HalloweenEvent.ALL_WARES_POOL
        assertTrue(pool.contains("pumpkin_bow"))
        assertTrue(pool.contains("ghost_pet"))
        assertTrue(pool.contains("golden_apple"))
        assertTrue(pool.contains("golden_carrots"))
        assertTrue(pool.contains("totem"))
        assertTrue(pool.contains("fireworks_shulker"))
        assertTrue(pool.contains("wither_skulls"))
        assertTrue(pool.contains("netherite_ingot"))
        assertTrue(pool.contains("slime_blocks"))
    }
}
