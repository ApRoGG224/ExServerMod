package com.fome.exservermod.halloween

import com.fome.exservermod.halloween.entity.CandyEntity
import com.fome.exservermod.halloween.entity.GhostPetEntity
import com.fome.exservermod.halloween.entity.HalloweenMerchantEntity
import com.fome.exservermod.halloween.entity.PumpkinProjectileEntity
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.animal.allay.Allay

object HalloweenEntities {
    val CANDY_ID: Identifier = Identifier.parse("exservermod:candy")
    val PUMPKIN_PROJECTILE_ID: Identifier = Identifier.parse("exservermod:pumpkin_projectile")
    val HALLOWEEN_MERCHANT_ID: Identifier = Identifier.parse("exservermod:halloween_merchant")
    val GHOST_PET_ID: Identifier = Identifier.parse("exservermod:ghost_pet")

    val CANDY: EntityType<CandyEntity> =
        EntityType.Builder
            .of(::CandyEntity, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, CANDY_ID))

    val PUMPKIN_PROJECTILE: EntityType<PumpkinProjectileEntity> =
        EntityType.Builder
            .of(::PumpkinProjectileEntity, MobCategory.MISC)
            .sized(0.5f, 0.5f)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, PUMPKIN_PROJECTILE_ID))

    val HALLOWEEN_MERCHANT: EntityType<HalloweenMerchantEntity> =
        EntityType.Builder
            .of(::HalloweenMerchantEntity, MobCategory.CREATURE)
            .sized(0.6f, 1.95f)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, HALLOWEEN_MERCHANT_ID))

    val GHOST_PET: EntityType<GhostPetEntity> =
        EntityType.Builder
            .of(::GhostPetEntity, MobCategory.CREATURE)
            .sized(0.6f, 0.8f)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, GHOST_PET_ID))

    fun register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, CANDY_ID, CANDY)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, PUMPKIN_PROJECTILE_ID, PUMPKIN_PROJECTILE)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, HALLOWEEN_MERCHANT_ID, HALLOWEEN_MERCHANT)
        Registry.register(BuiltInRegistries.ENTITY_TYPE, GHOST_PET_ID, GHOST_PET)

        FabricDefaultAttributeRegistry.register(HALLOWEEN_MERCHANT, Mob.createMobAttributes())
        FabricDefaultAttributeRegistry.register(GHOST_PET, Allay.createAttributes())
    }
}
