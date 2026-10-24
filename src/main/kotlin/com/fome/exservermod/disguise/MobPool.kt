package com.fome.exservermod.disguise

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import kotlin.random.Random

enum class SizeClass {
    TINY,
    MEDIUM,
    COLOSSAL,
}

enum class Origin {
    NETHER,
    THE_END,
    AQUATIC,
    OVERWORLD,
}

enum class Locomotion {
    FLYING,
    TERRESTRIAL,
}

enum class Disposition {
    PASSIVE,
    HOSTILE,
}

enum class HintCategory {
    SIZE,
    ORIGIN,
    LOCOMOTION,
    DISPOSITION,
}

object MobPool {
    val NETHER_MOBS: Set<EntityType<*>> =
        setOf(
            EntityTypes.BLAZE,
            EntityTypes.GHAST,
            EntityTypes.HOGLIN,
            EntityTypes.MAGMA_CUBE,
            EntityTypes.PIGLIN,
            EntityTypes.PIGLIN_BRUTE,
            EntityTypes.STRIDER,
            EntityTypes.WITHER,
            EntityTypes.WITHER_SKELETON,
            EntityTypes.ZOGLIN,
            EntityTypes.ZOMBIFIED_PIGLIN,
        )

    val END_MOBS: Set<EntityType<*>> =
        setOf(
            EntityTypes.ENDER_DRAGON,
            EntityTypes.ENDERMAN,
            EntityTypes.ENDERMITE,
            EntityTypes.SHULKER,
        )

    val AQUATIC_MOBS: Set<EntityType<*>> =
        setOf(
            EntityTypes.AXOLOTL,
            EntityTypes.COD,
            EntityTypes.DOLPHIN,
            EntityTypes.DROWNED,
            EntityTypes.ELDER_GUARDIAN,
            EntityTypes.GLOW_SQUID,
            EntityTypes.GUARDIAN,
            EntityTypes.PUFFERFISH,
            EntityTypes.SALMON,
            EntityTypes.SQUID,
            EntityTypes.TADPOLE,
            EntityTypes.TROPICAL_FISH,
            EntityTypes.TURTLE,
        )

    val FLYING_MOBS: Set<EntityType<*>> =
        setOf(
            EntityTypes.ALLAY,
            EntityTypes.BAT,
            EntityTypes.BEE,
            EntityTypes.BLAZE,
            EntityTypes.BREEZE,
            EntityTypes.ENDER_DRAGON,
            EntityTypes.GHAST,
            EntityTypes.PARROT,
            EntityTypes.PHANTOM,
            EntityTypes.VEX,
            EntityTypes.WITHER,
        )

    val PASSIVE_MOBS: Set<EntityType<*>> =
        setOf(
            EntityTypes.ALLAY,
            EntityTypes.ARMADILLO,
            EntityTypes.AXOLOTL,
            EntityTypes.BAT,
            EntityTypes.CAMEL,
            EntityTypes.CAT,
            EntityTypes.CHICKEN,
            EntityTypes.COD,
            EntityTypes.COW,
            EntityTypes.DONKEY,
            EntityTypes.FROG,
            EntityTypes.GLOW_SQUID,
            EntityTypes.GOAT,
            EntityTypes.HORSE,
            EntityTypes.IRON_GOLEM,
            EntityTypes.LLAMA,
            EntityTypes.MOOSHROOM,
            EntityTypes.MULE,
            EntityTypes.OCELOT,
            EntityTypes.PANDA,
            EntityTypes.PARROT,
            EntityTypes.PIG,
            EntityTypes.PUFFERFISH,
            EntityTypes.RABBIT,
            EntityTypes.SALMON,
            EntityTypes.SHEEP,
            EntityTypes.SNIFFER,
            EntityTypes.SNOW_GOLEM,
            EntityTypes.SQUID,
            EntityTypes.STRIDER,
            EntityTypes.TADPOLE,
            EntityTypes.TRADER_LLAMA,
            EntityTypes.TROPICAL_FISH,
            EntityTypes.TURTLE,
            EntityTypes.VILLAGER,
            EntityTypes.WANDERING_TRADER,
        )

    val AMBIENT_SOUNDS: Map<EntityType<*>, SoundEvent> by lazy {
        val cowSound =
            SoundEvents.COW_SOUNDS.values
                .first()
                .ambientSound()
                .value()
        val pigSound =
            SoundEvents.PIG_SOUNDS.values
                .first()
                .adultSounds()
                .ambientSound()
                .value()
        val chickenSound =
            SoundEvents.CHICKEN_SOUNDS.values
                .first()
                .adultSounds()
                .ambientSound()
                .value()
        val catSound =
            SoundEvents.CAT_SOUNDS.values
                .first()
                .adultSounds()
                .ambientSound()
                .value()
        val wolfSound =
            SoundEvents.WOLF_SOUNDS.values
                .first()
                .adultSounds()
                .ambientSound()
                .value()

        mapOf(
            EntityTypes.ALLAY to SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM,
            EntityTypes.ARMADILLO to SoundEvents.ARMADILLO_AMBIENT,
            EntityTypes.AXOLOTL to SoundEvents.AXOLOTL_IDLE_AIR,
            EntityTypes.BAT to SoundEvents.BAT_AMBIENT,
            EntityTypes.BEE to SoundEvents.BEE_LOOP,
            EntityTypes.BLAZE to SoundEvents.BLAZE_AMBIENT,
            EntityTypes.BOGGED to SoundEvents.BOGGED_AMBIENT,
            EntityTypes.BREEZE to SoundEvents.BREEZE_IDLE_GROUND,
            EntityTypes.CAMEL to SoundEvents.CAMEL_AMBIENT,
            EntityTypes.CAT to catSound,
            EntityTypes.CAVE_SPIDER to SoundEvents.SPIDER_AMBIENT,
            EntityTypes.CHICKEN to chickenSound,
            EntityTypes.COD to SoundEvents.COD_FLOP,
            EntityTypes.COW to cowSound,
            EntityTypes.CREAKING to SoundEvents.CREAKING_AMBIENT,
            EntityTypes.CREEPER to SoundEvents.CREEPER_PRIMED,
            EntityTypes.DOLPHIN to SoundEvents.DOLPHIN_AMBIENT,
            EntityTypes.DONKEY to SoundEvents.DONKEY_AMBIENT,
            EntityTypes.DROWNED to SoundEvents.DROWNED_AMBIENT,
            EntityTypes.ELDER_GUARDIAN to SoundEvents.ELDER_GUARDIAN_AMBIENT,
            EntityTypes.ENDER_DRAGON to SoundEvents.ENDER_DRAGON_AMBIENT,
            EntityTypes.ENDERMAN to SoundEvents.ENDERMAN_AMBIENT,
            EntityTypes.ENDERMITE to SoundEvents.ENDERMITE_AMBIENT,
            EntityTypes.EVOKER to SoundEvents.EVOKER_AMBIENT,
            EntityTypes.FOX to SoundEvents.FOX_AMBIENT,
            EntityTypes.FROG to SoundEvents.FROG_AMBIENT,
            EntityTypes.GHAST to SoundEvents.GHAST_AMBIENT,
            EntityTypes.GIANT to SoundEvents.ZOMBIE_AMBIENT,
            EntityTypes.GLOW_SQUID to SoundEvents.GLOW_SQUID_AMBIENT,
            EntityTypes.GOAT to SoundEvents.GOAT_AMBIENT,
            EntityTypes.GUARDIAN to SoundEvents.GUARDIAN_AMBIENT,
            EntityTypes.HOGLIN to SoundEvents.HOGLIN_AMBIENT,
            EntityTypes.HORSE to SoundEvents.HORSE_AMBIENT,
            EntityTypes.HUSK to SoundEvents.HUSK_AMBIENT,
            EntityTypes.ILLUSIONER to SoundEvents.ILLUSIONER_AMBIENT,
            EntityTypes.IRON_GOLEM to SoundEvents.IRON_GOLEM_REPAIR,
            EntityTypes.LLAMA to SoundEvents.LLAMA_AMBIENT,
            EntityTypes.MAGMA_CUBE to SoundEvents.MAGMA_CUBE_SQUISH,
            EntityTypes.MOOSHROOM to cowSound,
            EntityTypes.MULE to SoundEvents.MULE_AMBIENT,
            EntityTypes.OCELOT to SoundEvents.OCELOT_AMBIENT,
            EntityTypes.PANDA to SoundEvents.PANDA_AMBIENT,
            EntityTypes.PARROT to SoundEvents.PARROT_AMBIENT,
            EntityTypes.PHANTOM to SoundEvents.PHANTOM_AMBIENT,
            EntityTypes.PIG to pigSound,
            EntityTypes.PIGLIN to SoundEvents.PIGLIN_AMBIENT,
            EntityTypes.PIGLIN_BRUTE to SoundEvents.PIGLIN_BRUTE_AMBIENT,
            EntityTypes.PILLAGER to SoundEvents.PILLAGER_AMBIENT,
            EntityTypes.POLAR_BEAR to SoundEvents.POLAR_BEAR_AMBIENT,
            EntityTypes.PUFFERFISH to SoundEvents.PUFFER_FISH_BLOW_UP,
            EntityTypes.RABBIT to SoundEvents.RABBIT_AMBIENT,
            EntityTypes.RAVAGER to SoundEvents.RAVAGER_AMBIENT,
            EntityTypes.SALMON to SoundEvents.SALMON_FLOP,
            EntityTypes.SHEEP to SoundEvents.SHEEP_AMBIENT,
            EntityTypes.SHULKER to SoundEvents.SHULKER_AMBIENT,
            EntityTypes.SILVERFISH to SoundEvents.SILVERFISH_AMBIENT,
            EntityTypes.SKELETON to SoundEvents.SKELETON_AMBIENT,
            EntityTypes.SKELETON_HORSE to SoundEvents.SKELETON_HORSE_AMBIENT,
            EntityTypes.SLIME to SoundEvents.SLIME_SQUISH,
            EntityTypes.SNIFFER to SoundEvents.SNIFFER_IDLE,
            EntityTypes.SNOW_GOLEM to SoundEvents.SNOW_GOLEM_AMBIENT,
            EntityTypes.SPIDER to SoundEvents.SPIDER_AMBIENT,
            EntityTypes.SQUID to SoundEvents.SQUID_AMBIENT,
            EntityTypes.STRAY to SoundEvents.STRAY_AMBIENT,
            EntityTypes.STRIDER to SoundEvents.STRIDER_AMBIENT,
            EntityTypes.TADPOLE to SoundEvents.TADPOLE_FLOP,
            EntityTypes.TRADER_LLAMA to SoundEvents.LLAMA_AMBIENT,
            EntityTypes.TROPICAL_FISH to SoundEvents.TROPICAL_FISH_FLOP,
            EntityTypes.TURTLE to SoundEvents.TURTLE_AMBIENT_LAND,
            EntityTypes.VEX to SoundEvents.VEX_AMBIENT,
            EntityTypes.VILLAGER to SoundEvents.VILLAGER_AMBIENT,
            EntityTypes.VINDICATOR to SoundEvents.VINDICATOR_AMBIENT,
            EntityTypes.WANDERING_TRADER to SoundEvents.WANDERING_TRADER_AMBIENT,
            EntityTypes.WARDEN to SoundEvents.WARDEN_AMBIENT,
            EntityTypes.WITCH to SoundEvents.WITCH_AMBIENT,
            EntityTypes.WITHER to SoundEvents.WITHER_AMBIENT,
            EntityTypes.WITHER_SKELETON to SoundEvents.WITHER_SKELETON_AMBIENT,
            EntityTypes.WOLF to wolfSound,
            EntityTypes.ZOGLIN to SoundEvents.ZOGLIN_AMBIENT,
            EntityTypes.ZOMBIE to SoundEvents.ZOMBIE_AMBIENT,
            EntityTypes.ZOMBIE_HORSE to SoundEvents.ZOMBIE_HORSE_AMBIENT,
            EntityTypes.ZOMBIE_VILLAGER to SoundEvents.ZOMBIE_VILLAGER_AMBIENT,
            EntityTypes.ZOMBIFIED_PIGLIN to SoundEvents.ZOMBIFIED_PIGLIN_AMBIENT,
        )
    }

    val POOL: List<EntityType<*>> =
        listOf(
            EntityTypes.ALLAY,
            EntityTypes.ARMADILLO,
            EntityTypes.AXOLOTL,
            EntityTypes.BAT,
            EntityTypes.BEE,
            EntityTypes.BLAZE,
            EntityTypes.BOGGED,
            EntityTypes.BREEZE,
            EntityTypes.CAMEL,
            EntityTypes.CAT,
            EntityTypes.CAVE_SPIDER,
            EntityTypes.CHICKEN,
            EntityTypes.COD,
            EntityTypes.COW,
            EntityTypes.CREAKING,
            EntityTypes.CREEPER,
            EntityTypes.DOLPHIN,
            EntityTypes.DONKEY,
            EntityTypes.DROWNED,
            EntityTypes.ELDER_GUARDIAN,
            EntityTypes.ENDER_DRAGON,
            EntityTypes.ENDERMAN,
            EntityTypes.ENDERMITE,
            EntityTypes.EVOKER,
            EntityTypes.FOX,
            EntityTypes.FROG,
            EntityTypes.GHAST,
            EntityTypes.GIANT,
            EntityTypes.GLOW_SQUID,
            EntityTypes.GOAT,
            EntityTypes.GUARDIAN,
            EntityTypes.HOGLIN,
            EntityTypes.HORSE,
            EntityTypes.HUSK,
            EntityTypes.ILLUSIONER,
            EntityTypes.IRON_GOLEM,
            EntityTypes.LLAMA,
            EntityTypes.MAGMA_CUBE,
            EntityTypes.MOOSHROOM,
            EntityTypes.MULE,
            EntityTypes.OCELOT,
            EntityTypes.PANDA,
            EntityTypes.PARROT,
            EntityTypes.PHANTOM,
            EntityTypes.PIG,
            EntityTypes.PIGLIN,
            EntityTypes.PIGLIN_BRUTE,
            EntityTypes.PILLAGER,
            EntityTypes.POLAR_BEAR,
            EntityTypes.PUFFERFISH,
            EntityTypes.RABBIT,
            EntityTypes.RAVAGER,
            EntityTypes.SALMON,
            EntityTypes.SHEEP,
            EntityTypes.SHULKER,
            EntityTypes.SILVERFISH,
            EntityTypes.SKELETON,
            EntityTypes.SKELETON_HORSE,
            EntityTypes.SLIME,
            EntityTypes.SNIFFER,
            EntityTypes.SNOW_GOLEM,
            EntityTypes.SPIDER,
            EntityTypes.SQUID,
            EntityTypes.STRAY,
            EntityTypes.STRIDER,
            EntityTypes.TADPOLE,
            EntityTypes.TRADER_LLAMA,
            EntityTypes.TROPICAL_FISH,
            EntityTypes.TURTLE,
            EntityTypes.VEX,
            EntityTypes.VILLAGER,
            EntityTypes.VINDICATOR,
            EntityTypes.WANDERING_TRADER,
            EntityTypes.WARDEN,
            EntityTypes.WITCH,
            EntityTypes.WITHER,
            EntityTypes.WITHER_SKELETON,
            EntityTypes.WOLF,
            EntityTypes.ZOGLIN,
            EntityTypes.ZOMBIE,
            EntityTypes.ZOMBIE_HORSE,
            EntityTypes.ZOMBIE_VILLAGER,
            EntityTypes.ZOMBIFIED_PIGLIN,
        )

    fun getRandomMob(random: Random = Random): EntityType<*> = POOL[random.nextInt(POOL.size)]

    fun calculateScale(
        mobWidth: Float,
        mobHeight: Float,
    ): Double = 1.0

    fun calculateScale(entityType: EntityType<*>): Double = 1.0

    fun getSizeClass(entityType: EntityType<*>): SizeClass =
        when {
            entityType.height < 0.8f -> SizeClass.TINY
            entityType.height > 2.4f -> SizeClass.COLOSSAL
            else -> SizeClass.MEDIUM
        }

    fun getOrigin(entityType: EntityType<*>): Origin =
        when (entityType) {
            in NETHER_MOBS -> Origin.NETHER
            in END_MOBS -> Origin.THE_END
            in AQUATIC_MOBS -> Origin.AQUATIC
            else -> Origin.OVERWORLD
        }

    fun getLocomotion(entityType: EntityType<*>): Locomotion = if (entityType in FLYING_MOBS) Locomotion.FLYING else Locomotion.TERRESTRIAL

    fun getDisposition(entityType: EntityType<*>): Disposition =
        if (entityType in PASSIVE_MOBS) Disposition.PASSIVE else Disposition.HOSTILE

    fun getAmbientSound(entityType: EntityType<*>): SoundEvent = AMBIENT_SOUNDS[entityType] ?: SoundEvents.ZOMBIE_AMBIENT

    fun getHint(
        entityType: EntityType<*>,
        category: HintCategory,
    ): MutableComponent {
        val key =
            when (category) {
                HintCategory.SIZE ->
                    when (getSizeClass(entityType)) {
                        SizeClass.TINY -> "exservermod.disguise.hint.size.tiny"
                        SizeClass.MEDIUM -> "exservermod.disguise.hint.size.medium"
                        SizeClass.COLOSSAL -> "exservermod.disguise.hint.size.colossal"
                    }
                HintCategory.ORIGIN ->
                    when (getOrigin(entityType)) {
                        Origin.NETHER -> "exservermod.disguise.hint.origin.nether"
                        Origin.THE_END -> "exservermod.disguise.hint.origin.the_end"
                        Origin.AQUATIC -> "exservermod.disguise.hint.origin.aquatic"
                        Origin.OVERWORLD -> "exservermod.disguise.hint.origin.overworld"
                    }
                HintCategory.LOCOMOTION ->
                    when (getLocomotion(entityType)) {
                        Locomotion.FLYING -> "exservermod.disguise.hint.locomotion.flying"
                        Locomotion.TERRESTRIAL -> "exservermod.disguise.hint.locomotion.terrestrial"
                    }
                HintCategory.DISPOSITION ->
                    when (getDisposition(entityType)) {
                        Disposition.PASSIVE -> "exservermod.disguise.hint.disposition.passive"
                        Disposition.HOSTILE -> "exservermod.disguise.hint.disposition.hostile"
                    }
            }
        return Component.translatable(key)
    }

    fun getRandomHint(
        entityType: EntityType<*>,
        random: Random = Random,
    ): MutableComponent {
        val category = HintCategory.entries[random.nextInt(HintCategory.entries.size)]
        return getHint(entityType, category)
    }

    /**
     * Picks the next categorical hint avoiding repeats: categories already
     * mentioned in this round are skipped; once every category has been used,
     * the rotation restarts (but never repeats the immediately previous one).
     */
    fun getNextHint(
        entityType: EntityType<*>,
        usedCategories: Collection<HintCategory> = emptyList(),
        random: Random = Random,
    ): Pair<MutableComponent, HintCategory> {
        val fresh = HintCategory.entries.filter { it !in usedCategories }
        val candidates =
            if (fresh.isEmpty()) {
                // Full rotation exhausted: restart, avoiding the very last used category
                val last = usedCategories.lastOrNull()
                HintCategory.entries.filter { it != last }
            } else {
                fresh
            }
        val category = candidates[random.nextInt(candidates.size)]
        return getHint(entityType, category) to category
    }
}
