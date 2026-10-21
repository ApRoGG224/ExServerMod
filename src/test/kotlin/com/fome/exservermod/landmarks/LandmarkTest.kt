package com.fome.exservermod.landmarks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LandmarkTest {
    @Test
    fun testDistanceToPerimeterInsideOrOnRadius() {
        val landmark = Landmark(name = "Castle", x = 100.0, z = 100.0, radius = 50.0)

        // Exact center
        assertEquals(0.0, landmark.distanceToPerimeter(100.0, 100.0), 0.001)

        // Inside perimeter (distance 30 < 50)
        assertEquals(0.0, landmark.distanceToPerimeter(130.0, 100.0), 0.001)

        // On perimeter (distance 50 == 50)
        assertEquals(0.0, landmark.distanceToPerimeter(100.0, 150.0), 0.001)
    }

    @Test
    fun testDistanceToPerimeterOutsideRadius() {
        val landmark = Landmark(name = "Tower", x = 0.0, z = 0.0, radius = 50.0)

        // Target at (0, 150) -> center distance = 150 -> perimeter distance = 150 - 50 = 100
        assertEquals(100.0, landmark.distanceToPerimeter(0.0, 150.0), 0.001)

        // Target at (60, 80) -> center distance = hypot(60, 80) = 100 -> perimeter distance = 50
        assertEquals(50.0, landmark.distanceToPerimeter(60.0, 80.0), 0.001)
    }

    @Test
    fun testFindClosestLandmark(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "landmarks.json")
        val manager = LandmarkManager(file)

        // Landmark A: (0, 0), radius 20
        manager.addLandmark(Landmark("Spawn", 0.0, 0.0, 20.0, "minecraft:overworld"))
        // Landmark B: (100, 0), radius 50
        manager.addLandmark(Landmark("Fortress", 100.0, 0.0, 50.0, "minecraft:overworld"))
        // Landmark C: in the nether
        manager.addLandmark(Landmark("NetherBase", 10.0, 0.0, 10.0, "minecraft:the_nether"))

        // Player at (60, 0):
        // Distance to Spawn perimeter: hypot(60, 0) - 20 = 40.0
        // Distance to Fortress perimeter: hypot(40, 0) - 50 = max(0, -10) = 0.0
        val closestOverworld = manager.findClosest(60.0, 0.0, "minecraft:overworld")
        assertNotNull(closestOverworld)
        assertEquals("Fortress", closestOverworld?.name)

        // Nether check: only NetherBase is returned
        val closestNether = manager.findClosest(100.0, 0.0, "minecraft:the_nether")
        assertNotNull(closestNether)
        assertEquals("NetherBase", closestNether?.name)

        // Dimension with no landmarks
        val closestEnd = manager.findClosest(0.0, 0.0, "minecraft:the_end")
        assertNull(closestEnd)
    }

    @Test
    fun testAddAndRemoveLandmark(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "landmarks.json")
        val manager = LandmarkManager(file)

        val l1 = Landmark("Arena", 50.0, 50.0, 30.0)
        assertTrue(manager.addLandmark(l1))

        // Duplicate name case-insensitively
        val duplicate = Landmark("arena", 10.0, 10.0, 20.0)
        assertFalse(manager.addLandmark(duplicate))
        assertEquals(1, manager.getAllLandmarks().size)

        // Remove
        assertTrue(manager.removeLandmark("ARENA"))
        assertEquals(0, manager.getAllLandmarks().size)
        assertFalse(manager.removeLandmark("ARENA"))
    }

    @Test
    fun testJsonPersistence(
        @TempDir tempDir: File,
    ) {
        val file = File(tempDir, "landmarks.json")
        val manager1 = LandmarkManager(file)

        manager1.addLandmark(Landmark("Harbor", 200.0, 300.0, 45.0, "minecraft:overworld"))
        manager1.addLandmark(Landmark("Farm", -100.0, 50.0, 25.0, "minecraft:overworld"))

        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        // New manager instance reads back same data
        val manager2 = LandmarkManager(file)
        val loaded = manager2.getAllLandmarks()
        assertEquals(2, loaded.size)

        val harbor = manager2.getLandmark("harbor")
        assertNotNull(harbor)
        assertEquals("Harbor", harbor?.name)
        assertEquals(200.0, harbor?.x)
        assertEquals(300.0, harbor?.z)
        assertEquals(45.0, harbor?.radius)
    }
}
