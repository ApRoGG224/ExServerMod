package com.fome.exservermod.landmarks

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class LandmarkManager(
    private val configFile: File,
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val landmarks: MutableMap<String, Landmark> = mutableMapOf()

    init {
        load()
    }

    fun addLandmark(landmark: Landmark): Boolean {
        val key = landmark.name.lowercase()
        if (landmarks.containsKey(key)) {
            return false
        }
        landmarks[key] = landmark
        save()
        return true
    }

    fun removeLandmark(name: String): Boolean {
        val removed = landmarks.remove(name.lowercase()) != null
        if (removed) {
            save()
        }
        return removed
    }

    fun getLandmark(name: String): Landmark? = landmarks[name.lowercase()]

    fun getAllLandmarks(): List<Landmark> = landmarks.values.toList()

    fun findClosest(
        x: Double,
        z: Double,
        dimension: String,
    ): Landmark? =
        landmarks.values
            .filter { it.dimension == dimension }
            .minByOrNull { it.distanceToPerimeter(x, z) }

    fun load() {
        if (!configFile.exists()) {
            return
        }
        try {
            val json = configFile.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<Landmark>>() {}.type
            val loaded: List<Landmark>? = gson.fromJson(json, type)
            landmarks.clear()
            loaded?.forEach { landmark ->
                landmarks[landmark.name.lowercase()] = landmark
            }
        } catch (_: Exception) {
            // Keep existing in-memory landmarks if file is unreadable
        }
    }

    fun save() {
        try {
            configFile.parentFile?.mkdirs()
            val json = gson.toJson(landmarks.values.toList())
            configFile.writeText(json, Charsets.UTF_8)
        } catch (_: Exception) {
            // Non-fatal configuration write failure
        }
    }
}
