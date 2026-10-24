package com.fome.exservermod.disguise

import com.fome.exservermod.client.ClientDisguiseHolder
import com.fome.exservermod.game.GameEventHandler
import com.fome.exservermod.game.HideAndSeekGame
import net.minecraft.SharedConstants
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.server.Bootstrap
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID
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
    fun `test expanded mob pool contains all living entities`() {
        assertTrue(MobPool.POOL.size >= 70, "Mob pool must contain at least 70 entities, found ${MobPool.POOL.size}")
        assertEquals(MobPool.POOL.size, MobPool.POOL.distinct().size, "All entities in pool must be distinct")

        // Tiny mobs
        assertTrue(MobPool.POOL.contains(EntityTypes.BEE))
        assertTrue(MobPool.POOL.contains(EntityTypes.SILVERFISH))
        assertTrue(MobPool.POOL.contains(EntityTypes.FROG))
        assertTrue(MobPool.POOL.contains(EntityTypes.CHICKEN))

        // Bosses & Colossal mobs
        assertTrue(MobPool.POOL.contains(EntityTypes.WARDEN))
        assertTrue(MobPool.POOL.contains(EntityTypes.IRON_GOLEM))
        assertTrue(MobPool.POOL.contains(EntityTypes.ENDER_DRAGON))
        assertTrue(MobPool.POOL.contains(EntityTypes.WITHER))

        // Flying mobs
        assertTrue(MobPool.POOL.contains(EntityTypes.BAT))
        assertTrue(MobPool.POOL.contains(EntityTypes.PHANTOM))
        assertTrue(MobPool.POOL.contains(EntityTypes.GHAST))
        assertTrue(MobPool.POOL.contains(EntityTypes.BLAZE))

        // Aquatic mobs
        assertTrue(MobPool.POOL.contains(EntityTypes.DOLPHIN))
        assertTrue(MobPool.POOL.contains(EntityTypes.AXOLOTL))
        assertTrue(MobPool.POOL.contains(EntityTypes.DROWNED))
    }

    @Test
    fun `test trait categorization size classes`() {
        assertEquals(SizeClass.TINY, MobPool.getSizeClass(EntityTypes.BEE))
        assertEquals(SizeClass.TINY, MobPool.getSizeClass(EntityTypes.SILVERFISH))
        assertEquals(SizeClass.TINY, MobPool.getSizeClass(EntityTypes.ENDERMITE))
        assertEquals(SizeClass.TINY, MobPool.getSizeClass(EntityTypes.CHICKEN))

        assertEquals(SizeClass.MEDIUM, MobPool.getSizeClass(EntityTypes.ZOMBIE))
        assertEquals(SizeClass.MEDIUM, MobPool.getSizeClass(EntityTypes.SKELETON))
        assertEquals(SizeClass.MEDIUM, MobPool.getSizeClass(EntityTypes.CREEPER))
        assertEquals(SizeClass.MEDIUM, MobPool.getSizeClass(EntityTypes.COW))

        assertEquals(SizeClass.COLOSSAL, MobPool.getSizeClass(EntityTypes.WARDEN))
        assertEquals(SizeClass.COLOSSAL, MobPool.getSizeClass(EntityTypes.ENDERMAN))
        assertEquals(SizeClass.COLOSSAL, MobPool.getSizeClass(EntityTypes.IRON_GOLEM))
        assertEquals(SizeClass.COLOSSAL, MobPool.getSizeClass(EntityTypes.GHAST))
        assertEquals(SizeClass.COLOSSAL, MobPool.getSizeClass(EntityTypes.ENDER_DRAGON))
    }

    @Test
    fun `test trait categorization origins`() {
        assertEquals(Origin.NETHER, MobPool.getOrigin(EntityTypes.GHAST))
        assertEquals(Origin.NETHER, MobPool.getOrigin(EntityTypes.BLAZE))
        assertEquals(Origin.NETHER, MobPool.getOrigin(EntityTypes.PIGLIN))
        assertEquals(Origin.NETHER, MobPool.getOrigin(EntityTypes.WITHER))

        assertEquals(Origin.THE_END, MobPool.getOrigin(EntityTypes.ENDERMAN))
        assertEquals(Origin.THE_END, MobPool.getOrigin(EntityTypes.SHULKER))
        assertEquals(Origin.THE_END, MobPool.getOrigin(EntityTypes.ENDER_DRAGON))

        assertEquals(Origin.AQUATIC, MobPool.getOrigin(EntityTypes.DOLPHIN))
        assertEquals(Origin.AQUATIC, MobPool.getOrigin(EntityTypes.AXOLOTL))
        assertEquals(Origin.AQUATIC, MobPool.getOrigin(EntityTypes.DROWNED))

        assertEquals(Origin.OVERWORLD, MobPool.getOrigin(EntityTypes.COW))
        assertEquals(Origin.OVERWORLD, MobPool.getOrigin(EntityTypes.CREEPER))
        assertEquals(Origin.OVERWORLD, MobPool.getOrigin(EntityTypes.VILLAGER))
    }

    @Test
    fun `test trait categorization locomotion`() {
        assertEquals(Locomotion.FLYING, MobPool.getLocomotion(EntityTypes.BAT))
        assertEquals(Locomotion.FLYING, MobPool.getLocomotion(EntityTypes.BEE))
        assertEquals(Locomotion.FLYING, MobPool.getLocomotion(EntityTypes.PHANTOM))
        assertEquals(Locomotion.FLYING, MobPool.getLocomotion(EntityTypes.GHAST))
        assertEquals(Locomotion.FLYING, MobPool.getLocomotion(EntityTypes.ENDER_DRAGON))

        assertEquals(Locomotion.TERRESTRIAL, MobPool.getLocomotion(EntityTypes.ZOMBIE))
        assertEquals(Locomotion.TERRESTRIAL, MobPool.getLocomotion(EntityTypes.COW))
        assertEquals(Locomotion.TERRESTRIAL, MobPool.getLocomotion(EntityTypes.WARDEN))
    }

    @Test
    fun `test trait categorization disposition`() {
        assertEquals(Disposition.PASSIVE, MobPool.getDisposition(EntityTypes.COW))
        assertEquals(Disposition.PASSIVE, MobPool.getDisposition(EntityTypes.SHEEP))
        assertEquals(Disposition.PASSIVE, MobPool.getDisposition(EntityTypes.ALLAY))
        assertEquals(Disposition.PASSIVE, MobPool.getDisposition(EntityTypes.VILLAGER))

        assertEquals(Disposition.HOSTILE, MobPool.getDisposition(EntityTypes.ZOMBIE))
        assertEquals(Disposition.HOSTILE, MobPool.getDisposition(EntityTypes.CREEPER))
        assertEquals(Disposition.HOSTILE, MobPool.getDisposition(EntityTypes.WARDEN))
        assertEquals(Disposition.HOSTILE, MobPool.getDisposition(EntityTypes.WITHER))
    }

    @Test
    fun `test hint generation translatable keys`() {
        val sizeHint = MobPool.getHint(EntityTypes.BEE, HintCategory.SIZE)
        val sizeContents = sizeHint.contents as TranslatableContents
        assertEquals("exservermod.disguise.hint.size.tiny", sizeContents.key)

        val originHint = MobPool.getHint(EntityTypes.GHAST, HintCategory.ORIGIN)
        val originContents = originHint.contents as TranslatableContents
        assertEquals("exservermod.disguise.hint.origin.nether", originContents.key)

        val locomotionHint = MobPool.getHint(EntityTypes.BAT, HintCategory.LOCOMOTION)
        val locomotionContents = locomotionHint.contents as TranslatableContents
        assertEquals("exservermod.disguise.hint.locomotion.flying", locomotionContents.key)

        val dispositionHint = MobPool.getHint(EntityTypes.ZOMBIE, HintCategory.DISPOSITION)
        val dispositionContents = dispositionHint.contents as TranslatableContents
        assertEquals("exservermod.disguise.hint.disposition.hostile", dispositionContents.key)
    }

    @Test
    fun `test ambient sound retrieval`() {
        assertEquals(SoundEvents.ZOMBIE_AMBIENT, MobPool.getAmbientSound(EntityTypes.ZOMBIE))
        assertEquals(SoundEvents.CREEPER_PRIMED, MobPool.getAmbientSound(EntityTypes.CREEPER))
        assertEquals(SoundEvents.WARDEN_AMBIENT, MobPool.getAmbientSound(EntityTypes.WARDEN))
        assertEquals(SoundEvents.GHAST_AMBIENT, MobPool.getAmbientSound(EntityTypes.GHAST))

        // Fallback for unmapped or generic mob
        assertNotNull(MobPool.getAmbientSound(EntityTypes.GIANT))
    }

    @Test
    fun `test seeking phase hint interval timers`() {
        assertEquals(900, GameEventHandler.ACOUSTIC_TAUNT_INTERVAL_TICKS)
        assertEquals(45, GameEventHandler.ACOUSTIC_TAUNT_INTERVAL_TICKS / HideAndSeekGame.TICKS_PER_SECOND)

        assertEquals(1800, GameEventHandler.INTELLECTUAL_HINT_INTERVAL_TICKS)
        assertEquals(90, GameEventHandler.INTELLECTUAL_HINT_INTERVAL_TICKS / HideAndSeekGame.TICKS_PER_SECOND)

        assertEquals(900, GameEventHandler.HINT_INTERVAL_TICKS)
        assertEquals(45, GameEventHandler.HINT_INTERVAL_TICKS / HideAndSeekGame.TICKS_PER_SECOND)
    }

    @Test
    fun `test calculateScale formula`() {
        val playerScale = MobPool.calculateScale(0.6f, 1.8f)
        assertEquals(1.0, playerScale, 1e-4)

        val ironGolemScale = MobPool.calculateScale(1.4f, 2.7f)
        assertEquals(1.0, ironGolemScale, 1e-4)

        val endermanScale = MobPool.calculateScale(0.6f, 2.9f)
        assertEquals(1.0, endermanScale, 1e-4)

        val chickenScale = MobPool.calculateScale(0.4f, 0.7f)
        assertEquals(1.0, chickenScale, 1e-4)
    }

    @Test
    fun `test getRandomMob returns elements from pool`() {
        val random = Random(42)
        val selected = mutableSetOf<Any>()
        repeat(200) {
            val mob = MobPool.getRandomMob(random)
            assertTrue(MobPool.POOL.contains(mob))
            selected.add(mob)
        }
        assertTrue(selected.size > 20, "Random selection should sample a wide variety of mobs")
    }

    @Test
    fun `test disguise manager state tracking`() {
        val manager = DisguiseManager()
        val player1 = UUID.randomUUID()
        val player2 = UUID.randomUUID()

        assertFalse(manager.isDisguised(player1))
        assertEquals(null, manager.getDisguise(player1))

        manager.recordDisguise(player1, EntityTypes.ZOMBIE)
        manager.recordDisguise(player2, EntityTypes.CREEPER)

        assertTrue(manager.isDisguised(player1))
        assertEquals(EntityTypes.ZOMBIE, manager.getDisguise(player1))
        assertTrue(manager.isDisguised(player2))
        assertEquals(EntityTypes.CREEPER, manager.getDisguise(player2))

        val removed = manager.removeDisguise(player1)
        assertEquals(EntityTypes.ZOMBIE, removed)
        assertFalse(manager.isDisguised(player1))
        assertTrue(manager.isDisguised(player2))

        manager.clearAllDisguises()
        assertFalse(manager.isDisguised(player2))
        assertTrue(manager.disguises.isEmpty())
    }

    @Test
    fun `test getNextHint avoids consecutive identical categories`() {
        val history = mutableListOf<HintCategory>()
        for (i in 1..100) {
            val (hint, category) = MobPool.getNextHint(EntityTypes.ZOMBIE, history)
            assertNotNull(hint)
            assertTrue(category != history.lastOrNull(), "Consecutive hints must have different categories")
            history.add(category)
        }
    }

    @Test
    fun `test getNextHint does not repeat until all categories exhausted`() {
        // Fresh categories must be exhausted before any category shows up twice
        val history = mutableListOf<HintCategory>()
        repeat(HintCategory.entries.size) {
            val (_, category) = MobPool.getNextHint(EntityTypes.SHEEP, history)
            assertFalse(category in history, "Category $category must not repeat while fresh ones remain")
            history.add(category)
        }
        // Rotation restarts after exhaustion but still avoids immediate repeats
        val (_, nextAfterExhaustion) = MobPool.getNextHint(EntityTypes.SHEEP, history)
        assertTrue(nextAfterExhaustion != history.last(), "After exhaustion the last category must be avoided")
    }

    @Test
    fun `test client disguise holder tracking`() {
        val player = UUID.randomUUID()
        ClientDisguiseHolder.clear()

        assertFalse(ClientDisguiseHolder.isDisguised(player))
        assertEquals(null, ClientDisguiseHolder.getDisguiseMobId(player))

        ClientDisguiseHolder.setDisguise(player, "minecraft:creeper", true)
        assertTrue(ClientDisguiseHolder.isDisguised(player))
        assertEquals("minecraft:creeper", ClientDisguiseHolder.getDisguiseMobId(player))

        ClientDisguiseHolder.setDisguise(player, "minecraft:skeleton", true)
        assertTrue(ClientDisguiseHolder.isDisguised(player))
        assertEquals("minecraft:skeleton", ClientDisguiseHolder.getDisguiseMobId(player))

        ClientDisguiseHolder.setDisguise(player, "", false)
        assertFalse(ClientDisguiseHolder.isDisguised(player))
        assertEquals(null, ClientDisguiseHolder.getDisguiseMobId(player))

        ClientDisguiseHolder.setDisguise(player, "minecraft:zombie", true)
        ClientDisguiseHolder.clear()
        assertFalse(ClientDisguiseHolder.isDisguised(player))
    }

    @Test
    fun `test unstuck counter resets and constants`() {
        assertEquals(20_000L, DisguiseManager.UNSTUCK_COOLDOWN_MS)
        assertEquals(3, DisguiseManager.MAX_UNSTUCK_PER_ROUND)

        val manager = DisguiseManager()
        manager.resetUnstuckCounters()
    }
}
