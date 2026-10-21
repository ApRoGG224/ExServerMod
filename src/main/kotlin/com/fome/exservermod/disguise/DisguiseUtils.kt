package com.fome.exservermod.disguise

import com.fome.exservermod.ExServerMod
import com.fome.exservermod.client.ClientDisguiseHolder
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player

object DisguiseUtils {
    @JvmStatic
    fun getDisguiseType(player: Player): EntityType<*>? {
        try {
            val serverDisguise = ExServerMod.disguiseManager.getDisguise(player.uuid)
            if (serverDisguise != null) {
                return serverDisguise
            }
        } catch (_: Throwable) {
        }

        try {
            if (FabricLoader.getInstance().environmentType == EnvType.CLIENT) {
                val mobId = ClientDisguiseHolder.getDisguiseMobId(player.uuid) ?: return null
                val identifier = Identifier.tryParse(mobId) ?: return null
                val entityType: EntityType<*>? = BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null)
                return entityType
            }
        } catch (_: Throwable) {
        }

        return null
    }
}
