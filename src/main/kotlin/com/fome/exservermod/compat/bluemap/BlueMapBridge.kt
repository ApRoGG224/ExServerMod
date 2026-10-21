package com.fome.exservermod.compat.bluemap

import java.lang.reflect.Method
import java.util.Optional
import java.util.UUID

internal object BlueMapBridge {
    fun setPlayerVisibility(
        playerUuid: UUID,
        visible: Boolean,
    ) {
        val blueMapApiClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI")
        val getInstanceMethod: Method = blueMapApiClass.getMethod("getInstance")
        val optionalApi = getInstanceMethod.invoke(null) as? Optional<*> ?: return
        if (!optionalApi.isPresent) return
        val api = optionalApi.get()

        // 1. Try webApp.setPlayerVisibility(UUID, boolean)
        try {
            val getWebAppMethod = api.javaClass.getMethod("getWebApp")
            val webApp = getWebAppMethod.invoke(api)
            if (webApp != null) {
                val setVisibilityMethod =
                    webApp.javaClass.getMethod(
                        "setPlayerVisibility",
                        UUID::class.java,
                        java.lang.Boolean.TYPE,
                    )
                setVisibilityMethod.invoke(webApp, playerUuid, visible)
                return
            }
        } catch (_: NoSuchMethodException) {
        }

        // 2. Try plugin.setPlayerVisibility(UUID, boolean)
        try {
            val getPluginMethod = api.javaClass.getMethod("getPlugin")
            val plugin = getPluginMethod.invoke(api)
            if (plugin != null) {
                val setVisibilityMethod =
                    plugin.javaClass.getMethod(
                        "setPlayerVisibility",
                        UUID::class.java,
                        java.lang.Boolean.TYPE,
                    )
                setVisibilityMethod.invoke(plugin, playerUuid, visible)
                return
            }
        } catch (_: NoSuchMethodException) {
        }

        // 3. Fallback: direct api.setPlayerVisibility(UUID, boolean)
        try {
            val setVisibilityMethod =
                api.javaClass.getMethod(
                    "setPlayerVisibility",
                    UUID::class.java,
                    java.lang.Boolean.TYPE,
                )
            setVisibilityMethod.invoke(api, playerUuid, visible)
        } catch (_: NoSuchMethodException) {
        }
    }
}
