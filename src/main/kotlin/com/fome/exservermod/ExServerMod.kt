package com.fome.exservermod

import com.fome.exservermod.game.GameCommands
import com.fome.exservermod.game.GameEventHandler
import com.fome.exservermod.game.HideAndSeekGame
import com.fome.exservermod.landmarks.LandmarkCommands
import com.fome.exservermod.landmarks.LandmarkManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

object ExServerMod : ModInitializer {
    const val MOD_ID = "exservermod"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    val game = HideAndSeekGame()
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

        GameEventHandler(game, landmarkManager).register()
        GameCommands(game).register()
        LandmarkCommands(landmarkManager).register()

        logger.info("[ExServerMod] Core game and landmarks initialized.")
    }
}
