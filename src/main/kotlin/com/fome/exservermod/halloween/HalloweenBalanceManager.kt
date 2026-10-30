package com.fome.exservermod.halloween

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class HalloweenBalanceManager(
    private val storageFile: File,
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val balances: ConcurrentHashMap<UUID, Int> = ConcurrentHashMap()

    init {
        load()
    }

    fun getBalance(uuid: UUID): Int = balances.getOrDefault(uuid, 0)

    fun addCandies(
        player: ServerPlayer,
        amount: Int,
        sendActionBar: Boolean = true,
    ): Int {
        if (amount <= 0) return getBalance(player.uuid)
        val newBalance = balances.compute(player.uuid) { _, current -> (current ?: 0) + amount } ?: amount
        save()
        if (sendActionBar) {
            sendBalanceActionBar(player, newBalance)
        }
        return newBalance
    }

    fun addCandies(
        uuid: UUID,
        amount: Int,
    ): Int {
        if (amount <= 0) return getBalance(uuid)
        val newBalance = balances.compute(uuid) { _, current -> (current ?: 0) + amount } ?: amount
        save()
        return newBalance
    }

    fun spendCandies(
        player: ServerPlayer,
        cost: Int,
    ): Boolean {
        if (cost <= 0) return true
        var success = false
        balances.compute(player.uuid) { _, current ->
            val bal = current ?: 0
            if (bal >= cost) {
                success = true
                bal - cost
            } else {
                success = false
                bal
            }
        }
        if (success) {
            save()
            sendBalanceActionBar(player, getBalance(player.uuid))
        }
        return success
    }

    fun setCandies(
        uuid: UUID,
        amount: Int,
    ) {
        val sanitized = amount.coerceAtLeast(0)
        balances[uuid] = sanitized
        save()
    }

    fun sendBalanceActionBar(
        player: ServerPlayer,
        balance: Int = getBalance(player.uuid),
        timeFormatted: String? = null,
    ) {
        val message =
            if (timeFormatted != null) {
                Component.translatable("exservermod.halloween.candy_counter_time", balance, timeFormatted)
            } else {
                Component.translatable("exservermod.halloween.candy_counter", balance)
            }
        player.sendSystemMessage(
            message,
            true,
        )
    }

    fun load() {
        if (!storageFile.exists()) return
        try {
            val json = storageFile.readText(Charsets.UTF_8)
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val loaded: Map<String, Int>? = gson.fromJson(json, type)
            balances.clear()
            loaded?.forEach { (k, v) ->
                try {
                    balances[UUID.fromString(k)] = v
                } catch (_: IllegalArgumentException) {
                    // Ignore malformed UUID keys
                }
            }
        } catch (_: Exception) {
            // Keep in-memory balances on read failure
        }
    }

    fun save() {
        try {
            storageFile.parentFile?.mkdirs()
            val stringKeyMap = balances.mapKeys { it.key.toString() }
            storageFile.writeText(gson.toJson(stringKeyMap), Charsets.UTF_8)
        } catch (_: Exception) {
            // Non-fatal configuration write failure
        }
    }
}
