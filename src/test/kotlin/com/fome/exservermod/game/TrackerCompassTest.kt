package com.fome.exservermod.game

import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.core.component.DataComponentMap
import net.minecraft.server.Bootstrap
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TrackerCompassTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setupMinecraft() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
            Items.COMPASS.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY)
            Items.APPLE.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY)
        }
    }

    @Test
    fun testTrackerCompassIdentification() {
        val targetPos = GlobalPos.of(Level.OVERWORLD, BlockPos(100, 64, 200))
        val tracker = TrackerCompassManager.createTrackerCompass(targetPos)
        val regular = ItemStack(Items.COMPASS)
        val empty = ItemStack.EMPTY
        val apple = ItemStack(Items.APPLE)

        assertTrue(TrackerCompassManager.isTrackerCompass(tracker))
        assertFalse(TrackerCompassManager.isTrackerCompass(regular))
        assertFalse(TrackerCompassManager.isTrackerCompass(empty))
        assertFalse(TrackerCompassManager.isTrackerCompass(apple))
    }

    @Test
    fun testPurgeContainer() {
        val targetPos = GlobalPos.of(Level.OVERWORLD, BlockPos(100, 64, 200))
        val tracker = TrackerCompassManager.createTrackerCompass(targetPos)
        val regular = ItemStack(Items.COMPASS)
        val apple = ItemStack(Items.APPLE)

        val container = SimpleContainer(5)
        container.setItem(0, apple)
        container.setItem(1, tracker)
        container.setItem(2, regular)

        val changed = TrackerCompassManager.purgeContainer(container)
        assertTrue(changed)
        assertFalse(container.getItem(0).isEmpty, "Apple must remain in container")
        assertTrue(container.getItem(1).isEmpty, "Tracker compass must be purged from container")
        assertFalse(container.getItem(2).isEmpty, "Regular compass must remain in container")
    }
}
