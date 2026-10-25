package com.fome.exservermod.game

import net.minecraft.ChatFormatting
import net.minecraft.core.GlobalPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.LodestoneTracker
import java.util.Optional

object TrackerCompassManager {
    const val TRACKER_TAG = "exservermod_tracker"

    @JvmStatic
    fun isTrackerCompass(stack: ItemStack): Boolean {
        if (stack.isEmpty || !stack.`is`(Items.COMPASS)) return false
        val customData = stack.get(DataComponents.CUSTOM_DATA) ?: return false
        return customData.copyTag().getBooleanOr(TRACKER_TAG, false)
    }

    @JvmStatic
    fun createTrackerCompass(targetPos: GlobalPos): ItemStack {
        val compass = ItemStack(Items.COMPASS)
        compass.set(DataComponents.LODESTONE_TRACKER, LodestoneTracker(Optional.of(targetPos), false))
        compass.set(
            DataComponents.CUSTOM_NAME,
            Component
                .translatable("exservermod.game.tracker_compass_name")
                .withStyle(ChatFormatting.GOLD),
        )
        val customData =
            CustomData.EMPTY.update { tag ->
                tag.putBoolean(TRACKER_TAG, true)
            }
        compass.set(DataComponents.CUSTOM_DATA, customData)
        return compass
    }

    @JvmStatic
    fun purgePlayer(player: ServerPlayer): Boolean {
        var changed = false
        for (i in 0 until player.inventory.containerSize) {
            val stack = player.inventory.getItem(i)
            if (isTrackerCompass(stack)) {
                player.inventory.setItem(i, ItemStack.EMPTY)
                changed = true
            }
        }
        val carried = player.containerMenu.carried
        if (isTrackerCompass(carried)) {
            player.containerMenu.carried = ItemStack.EMPTY
            changed = true
        }
        for (i in 0 until player.enderChestInventory.containerSize) {
            val stack = player.enderChestInventory.getItem(i)
            if (isTrackerCompass(stack)) {
                player.enderChestInventory.setItem(i, ItemStack.EMPTY)
                changed = true
            }
        }
        if (changed) {
            player.inventoryMenu.broadcastChanges()
            player.containerMenu.broadcastChanges()
        }
        return changed
    }

    @JvmStatic
    fun purgeContainer(container: Container): Boolean {
        var changed = false
        for (i in 0 until container.containerSize) {
            val stack = container.getItem(i)
            if (isTrackerCompass(stack)) {
                container.setItem(i, ItemStack.EMPTY)
                changed = true
            }
        }
        return changed
    }

    @JvmStatic
    fun purgeAll(server: MinecraftServer) {
        for (player in server.playerList.players) {
            purgePlayer(player)
        }

        for (level in server.allLevels) {
            for (entity in level.allEntities) {
                when (entity) {
                    is ItemEntity -> {
                        if (isTrackerCompass(entity.item)) {
                            entity.discard()
                        }
                    }
                    is ItemFrame -> {
                        if (isTrackerCompass(entity.item)) {
                            entity.setItem(ItemStack.EMPTY)
                        }
                    }
                    is Container -> {
                        purgeContainer(entity)
                    }
                }
            }
        }
    }
}
