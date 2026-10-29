package com.fome.exservermod.landmarks

import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

class LandmarkCommands(
    private val manager: LandmarkManager,
) {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands
                    .literal("location-add")
                    .then(
                        Commands
                            .argument("radius", DoubleArgumentType.doubleArg(1.0))
                            .then(
                                Commands
                                    .argument("name", StringArgumentType.greedyString())
                                    .executes { ctx ->
                                        val player = ctx.source.player
                                        if (player != null) {
                                            val radius = DoubleArgumentType.getDouble(ctx, "radius")
                                            val name = StringArgumentType.getString(ctx, "name").trim()
                                            handleAdd(player, name, radius)
                                        }
                                        1
                                    },
                            ),
                    ).then(
                        Commands
                            .argument("name", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    val name = StringArgumentType.getString(ctx, "name").trim()
                                    handleAdd(player, name, Landmark.DEFAULT_RADIUS)
                                }
                                1
                            },
                    ),
            )

            dispatcher.register(
                Commands
                    .literal("location-remove")
                    .then(
                        Commands
                            .argument("name", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val player = ctx.source.player
                                if (player != null) {
                                    val name = StringArgumentType.getString(ctx, "name").trim()
                                    handleRemove(player, name)
                                }
                                1
                            },
                    ),
            )

            dispatcher.register(
                Commands
                    .literal("location-list")
                    .executes { ctx ->
                        val player = ctx.source.player
                        if (player != null) {
                            handleList(player)
                        }
                        1
                    },
            )
        }
    }

    private fun handleAdd(
        player: ServerPlayer,
        name: String,
        radius: Double,
    ) {
        if (radius <= 0.0) {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.landmarks.invalid_radius")
                    .withStyle(ChatFormatting.RED),
            )
            return
        }

        val dim =
            player
                .level()
                .dimension()
                .identifier()
                .toString()
        val landmark =
            Landmark(
                name = name,
                x = player.x,
                z = player.z,
                radius = radius,
                dimension = dim,
            )

        val added = manager.addLandmark(landmark)
        if (added) {
            player.sendSystemMessage(
                Component
                    .translatable(
                        "exservermod.landmarks.added",
                        name,
                        player.x,
                        player.z,
                        radius,
                        DimensionFormatter.format(dim),
                    ).withStyle(ChatFormatting.GREEN),
            )
        } else {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.landmarks.already_exists", name)
                    .withStyle(ChatFormatting.RED),
            )
        }
    }

    private fun handleRemove(
        player: ServerPlayer,
        name: String,
    ) {
        val removed = manager.removeLandmark(name)
        if (removed) {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.landmarks.removed", name)
                    .withStyle(ChatFormatting.GREEN),
            )
        } else {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.landmarks.not_found", name)
                    .withStyle(ChatFormatting.RED),
            )
        }
    }

    private fun handleList(player: ServerPlayer) {
        val all = manager.getAllLandmarks()
        if (all.isEmpty()) {
            player.sendSystemMessage(
                Component
                    .translatable("exservermod.landmarks.list_empty")
                    .withStyle(ChatFormatting.GRAY),
            )
            return
        }

        player.sendSystemMessage(
            Component
                .translatable("exservermod.landmarks.list_header", all.size)
                .withStyle(ChatFormatting.GOLD),
        )

        all.sortedBy { it.name }.forEach { landmark ->
            player.sendSystemMessage(
                Component
                    .translatable(
                        "exservermod.landmarks.list_entry",
                        landmark.name,
                        landmark.x,
                        landmark.z,
                        landmark.radius,
                        DimensionFormatter.format(landmark.dimension),
                    ).withStyle(ChatFormatting.YELLOW),
            )
        }
    }
}
