package com.fome.exservermod.compat.bluemap

import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.util.UUID

object BlueMapCompat {
    private val logger = LoggerFactory.getLogger(BlueMapCompat::class.java)

    val isModLoaded: Boolean by lazy {
        FabricLoader.getInstance().isModLoaded("bluemap")
    }

    fun setPlayerVisibility(
        playerUuid: UUID,
        visible: Boolean,
    ) {
        if (!isModLoaded) return
        try {
            BlueMapBridge.setPlayerVisibility(playerUuid, visible)
        } catch (e: Throwable) {
            logger.warn("[ExServerMod] Failed to update BlueMap player visibility for $playerUuid: ${e.message}")
        }
    }

    fun setPlayersVisibility(
        playerUuids: Collection<UUID>,
        visible: Boolean,
    ) {
        if (!isModLoaded) return
        playerUuids.forEach { setPlayerVisibility(it, visible) }
    }
}
