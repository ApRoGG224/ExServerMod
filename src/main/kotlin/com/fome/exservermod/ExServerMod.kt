package com.fome.exservermod

import com.fome.exservermod.disguise.DisguiseManager
import com.fome.exservermod.game.GameCommands
import com.fome.exservermod.game.GameEventHandler
import com.fome.exservermod.game.HideAndSeekGame
import com.fome.exservermod.landmarks.LandmarkCommands
import com.fome.exservermod.landmarks.LandmarkManager
import com.fome.exservermod.network.DisguiseSyncPayload
import com.fome.exservermod.network.OpenRoulettePayload
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

object ExServerMod : ModInitializer {
    const val MOD_ID = "exservermod"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    val game = HideAndSeekGame()
    val disguiseManager = DisguiseManager()
    val landmarkManager by lazy {
        val configFile =
            FabricLoader
                .getInstance()
                .configDir
                .resolve("exservermod/landmarks.json")
                .toFile()
        LandmarkManager(configFile)
    }

    override fun onInitialize() {
        logger.info("[ExServerMod] Initializing server mod.")

        PayloadTypeRegistry.clientboundPlay().register(OpenRoulettePayload.TYPE, OpenRoulettePayload.STREAM_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(DisguiseSyncPayload.TYPE, DisguiseSyncPayload.STREAM_CODEC)

        GameEventHandler(game, landmarkManager, disguiseManager).register()
        GameCommands(game, disguiseManager, landmarkManager).register()
        LandmarkCommands(landmarkManager).register()

        logger.info("[ExServerMod] Core game, disguise engine, and landmarks initialized.")
    }
}
