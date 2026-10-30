package com.fome.exservermod.halloween

import com.fome.exservermod.halloween.entity.CandyEntity
import com.fome.exservermod.halloween.entity.GhostPetEntity
import com.fome.exservermod.halloween.entity.HalloweenMerchantEntity
import com.fome.exservermod.halloween.network.CandyChannelPayload
import com.fome.exservermod.halloween.network.HalloweenSkySyncPayload
import com.fome.exservermod.halloween.network.MerchantBuyPayload
import com.fome.exservermod.halloween.network.OpenGhostNamingPayload
import com.fome.exservermod.halloween.network.OpenMerchantGuiPayload
import com.fome.exservermod.halloween.network.SubmitGhostNamePayload
import com.fome.exservermod.landmarks.LandmarkManager
import it.unimi.dsi.fastutil.ints.IntArrayList
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.FireworkExplosion
import net.minecraft.world.item.component.Fireworks
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import java.io.File
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class HalloweenPhase {
    IDLE,
    ACTIVE_GATHERING,
    MERCHANT_ONLY,
}

class HalloweenEvent(
    private val landmarks: LandmarkManager,
    configFile: File,
    storageFile: File,
) {
    companion object {
        var instance: HalloweenEvent? = null
            private set
        var balanceManager: HalloweenBalanceManager? = null
        var config: HalloweenConfig = HalloweenConfig()
        var activeMerchantWares: List<String> = emptyList()

        val ALL_WARES_POOL =
            listOf(
                "pumpkin_bow",
                "ghost_pet",
                "golden_apple",
                "golden_carrots",
                "totem",
                "fireworks_shulker",
                "wither_skulls",
                "netherite_ingot",
                "slime_blocks",
            )
    }

    init {
        instance = this
        config = HalloweenConfig.loadOrCreate(configFile)
        balanceManager = HalloweenBalanceManager(storageFile)
    }

    var phase: HalloweenPhase = HalloweenPhase.IDLE
        private set

    var ticksUntilNextEvent: Int = config.eventCooldownSeconds * 20
        private set

    var activeGatheringTicksLeft: Int = 0
        private set

    var merchantGraceTicksLeft: Int = 0
        private set

    private var merchantSpawnTick: Int = 0
    private var spawnedMerchantUuid: UUID? = null
    private val spawnedCandies: MutableList<CandyEntity> = mutableListOf()
    private val playerPets: MutableMap<UUID, UUID> = mutableMapOf()

    fun register() {
        registerServerTick()
        registerPackets()
        registerPlayerJoin()
        registerServerLifecycle()
    }

    fun formattedTimeLeft(): String {
        val totalSeconds =
            when (phase) {
                HalloweenPhase.ACTIVE_GATHERING -> (activeGatheringTicksLeft / 20).coerceAtLeast(0)
                HalloweenPhase.MERCHANT_ONLY -> (merchantGraceTicksLeft / 20).coerceAtLeast(0)
                HalloweenPhase.IDLE -> (ticksUntilNextEvent / 20).coerceAtLeast(0)
            }
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun registerServerTick() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            when (phase) {
                HalloweenPhase.IDLE -> {
                    ticksUntilNextEvent--
                    if (ticksUntilNextEvent <= 0) {
                        startEvent(server)
                    }
                }

                HalloweenPhase.ACTIVE_GATHERING -> {
                    activeGatheringTicksLeft--

                    // Broadcast Action Bar HUD every second with time remaining
                    if (server.tickCount % 20 == 0) {
                        val timeLeft = formattedTimeLeft()
                        for (player in server.playerList.players) {
                            balanceManager?.sendBalanceActionBar(player, timeFormatted = timeLeft)
                        }
                    }

                    // Replenish candies near players and cleanup far candies every 100 ticks (5s)
                    if (server.tickCount % 100 == 0) {
                        spawnCandiesAroundPlayers(server)
                        cleanupFarCandies(server)
                    }

                    // Check for merchant spawn between minute 2 and 8
                    val elapsed = (config.eventDurationSeconds * 20) - activeGatheringTicksLeft
                    if (spawnedMerchantUuid == null && elapsed >= merchantSpawnTick) {
                        spawnMerchant(server)
                    }

                    if (activeGatheringTicksLeft <= 0) {
                        endGathering(server)
                    }
                }

                HalloweenPhase.MERCHANT_ONLY -> {
                    merchantGraceTicksLeft--

                    if (server.tickCount % 40 == 0) {
                        val timeLeft = formattedTimeLeft()
                        for (player in server.playerList.players) {
                            balanceManager?.sendBalanceActionBar(player, timeFormatted = timeLeft)
                        }
                    }

                    if (merchantGraceTicksLeft <= 0) {
                        endEvent(server)
                    }
                }
            }
        }
    }

    fun startEvent(server: MinecraftServer) {
        phase = HalloweenPhase.ACTIVE_GATHERING
        activeGatheringTicksLeft = config.eventDurationSeconds * 20
        merchantGraceTicksLeft = config.merchantGraceSeconds * 20
        ticksUntilNextEvent = config.eventCooldownSeconds * 20

        // Schedule merchant between 2400 (2m) and 9600 (8m)
        merchantSpawnTick = Random.nextInt(2400, 9600)
        spawnedMerchantUuid = null

        // Pick 6-8 randomized wares for this event
        activeMerchantWares = ALL_WARES_POOL.shuffled().take(Random.nextInt(6, 9))

        // Clean any lingering entities
        clearCandies()

        val overworld = server.overworld()

        // Sync blood sky to clients
        for (player in server.playerList.players) {
            ServerPlayNetworking.send(player, HalloweenSkySyncPayload(true))
        }

        // Broadcast announcements
        server.playerList.broadcastSystemMessage(
            Component.translatable("exservermod.halloween.event_started").withStyle(ChatFormatting.GOLD),
            false,
        )

        for (player in overworld.players()) {
            player.level().playSound(null, player.x, player.y, player.z, SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.8f, 0.5f)
        }

        // Spawn candies around players
        spawnCandiesAroundPlayers(server)
    }

    private fun spawnCandiesAroundPlayers(server: MinecraftServer) {
        val overworld = server.overworld()
        val players = overworld.players()
        if (players.isEmpty()) return

        spawnedCandies.removeAll { !it.isAlive }

        for (player in players) {
            val playerPos = player.position()
            val nearbyCount = spawnedCandies.count { it.isAlive && it.position().distanceTo(playerPos) <= 35.0 }
            val needed = 12 - nearbyCount
            if (needed <= 0) continue

            val toSpawn = minOf(needed, 8)
            for (i in 0 until toSpawn) {
                val angle = Random.nextDouble(0.0, 2 * Math.PI)
                val dist = Random.nextDouble(5.0, config.candySpawnRadius.toDouble())
                val spawnX = player.x + cos(angle) * dist
                val spawnZ = player.z + sin(angle) * dist

                val groundY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX.toInt(), spawnZ.toInt())
                val targetPos = BlockPos(spawnX.toInt(), groundY, spawnZ.toInt())

                // Strictly avoid caves / indoor blocks
                if (!overworld.canSeeSky(targetPos.above())) continue

                // Check distance separation from already spawned candies
                val candyVec = Vec3(spawnX, groundY + 1.0, spawnZ)
                if (spawnedCandies.any { it.isAlive && it.position().distanceTo(candyVec) < config.minCandySeparation }) continue

                val candy = CandyEntity(HalloweenEntities.CANDY, overworld)
                candy.setPos(spawnX, groundY + 1.0, spawnZ)
                overworld.addFreshEntity(candy)
                spawnedCandies.add(candy)
            }
        }
    }

    private fun cleanupFarCandies(server: MinecraftServer) {
        val overworld = server.overworld()
        val players = overworld.players()
        if (players.isEmpty()) return

        val iterator = spawnedCandies.iterator()
        while (iterator.hasNext()) {
            val candy = iterator.next()
            if (!candy.isAlive) {
                iterator.remove()
                continue
            }
            val pos = candy.position()
            val nearestPlayerDist = players.minOfOrNull { it.position().distanceTo(pos) } ?: Double.MAX_VALUE
            if (nearestPlayerDist > 80.0) {
                candy.removeLight()
                candy.discard()
                iterator.remove()
            }
        }
    }

    private fun isGlassOrLeaves(state: BlockState): Boolean {
        if (state.`is`(BlockTags.LEAVES) || state.`is`(BlockTags.IMPERMEABLE)) return true
        val desc = state.block.descriptionId.lowercase()
        return desc.contains("glass") || desc.contains("leaves")
    }

    private fun isClearOrSkyAbove(
        level: ServerLevel,
        pos: BlockPos,
    ): Boolean {
        val maxY = level.maxY
        for (y in (pos.y + 2)..maxY) {
            val checkPos = BlockPos(pos.x, y, pos.z)
            val state = level.getBlockState(checkPos)
            if (state.isAir) continue
            if (!isGlassOrLeaves(state)) {
                return false
            }
        }
        return true
    }

    private fun spawnMerchant(server: MinecraftServer) {
        val overworld = server.overworld()
        val allLandmarks = landmarks.getAllLandmarks().filter { it.dimension == "minecraft:overworld" }

        val (anchorX, anchorZ, landmarkName) =
            if (allLandmarks.isNotEmpty()) {
                val lm = allLandmarks.random()
                Triple(lm.x, lm.z, lm.name)
            } else {
                val p = overworld.players().firstOrNull()
                if (p != null) Triple(p.x, p.z, "Спавн") else Triple(0.0, 0.0, "Центр")
            }

        var chosenPos: BlockPos? = null
        var chosenDistance = 0.0

        for (attempt in 0 until 100) {
            val distance = Random.nextDouble(10.0, 60.0)
            val angle = Random.nextDouble(0.0, 2 * Math.PI)
            val targetX = (anchorX + cos(angle) * distance).toInt()
            val targetZ = (anchorZ + sin(angle) * distance).toInt()

            val groundY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ)
            val candidatePos = BlockPos(targetX, groundY, targetZ)
            val floor = overworld.getBlockState(candidatePos.below())
            val feet = overworld.getBlockState(candidatePos)
            val head = overworld.getBlockState(candidatePos.above())

            if (!feet.isAir || !head.isAir || floor.isAir || !overworld.getFluidState(candidatePos.below()).isEmpty) {
                continue
            }

            if (isClearOrSkyAbove(overworld, candidatePos)) {
                chosenPos = candidatePos
                chosenDistance = distance
                break
            }
        }

        if (chosenPos == null) {
            val targetX = anchorX.toInt() + 5
            val targetZ = anchorZ.toInt() + 5
            val groundY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, targetX, targetZ)
            chosenPos = BlockPos(targetX, groundY, targetZ)
            chosenDistance = 15.0
        }

        val standingBlock = overworld.getBlockState(chosenPos.below()).block
        val standingBlockName = standingBlock.name.string

        val merchant = HalloweenMerchantEntity(HalloweenEntities.HALLOWEEN_MERCHANT, overworld)
        merchant.setPos(chosenPos.x + 0.5, chosenPos.y.toDouble(), chosenPos.z + 0.5)
        overworld.addFreshEntity(merchant)
        spawnedMerchantUuid = merchant.uuid

        server.playerList.broadcastSystemMessage(
            Component
                .translatable(
                    "exservermod.halloween.merchant_spawned_full",
                    landmarkName,
                    chosenDistance.toInt(),
                    standingBlockName,
                    chosenPos.x,
                    chosenPos.y,
                    chosenPos.z,
                ).withStyle(ChatFormatting.LIGHT_PURPLE),
            false,
        )

        for (player in overworld.players()) {
            player.level().playSound(null, player.x, player.y, player.z, SoundEvents.WANDERING_TRADER_YES, SoundSource.NEUTRAL, 1.0f, 1.0f)
        }
    }

    private fun endGathering(server: MinecraftServer) {
        phase = HalloweenPhase.MERCHANT_ONLY
        clearCandies()

        // Turn off blood sky
        for (player in server.playerList.players) {
            ServerPlayNetworking.send(player, HalloweenSkySyncPayload(false))
        }

        server.playerList.broadcastSystemMessage(
            Component.translatable("exservermod.halloween.event_gathering_ended").withStyle(ChatFormatting.YELLOW),
            false,
        )
    }

    fun endEvent(server: MinecraftServer) {
        phase = HalloweenPhase.IDLE
        ticksUntilNextEvent = config.eventCooldownSeconds * 20
        clearCandies()

        // Despawn merchant
        val merchantUuid = spawnedMerchantUuid
        if (merchantUuid != null) {
            val overworld = server.overworld()
            val merchant = overworld.getEntity(merchantUuid)
            if (merchant != null) {
                overworld.sendParticles(
                    ParticleTypes.POOF,
                    merchant.x,
                    merchant.y + 1.0,
                    merchant.z,
                    20,
                    0.5,
                    0.5,
                    0.5,
                    0.05,
                )
                merchant.discard()
            }
            spawnedMerchantUuid = null
        }

        // Restore normal sky
        for (player in server.playerList.players) {
            ServerPlayNetworking.send(player, HalloweenSkySyncPayload(false))
        }

        server.playerList.broadcastSystemMessage(
            Component.translatable("exservermod.halloween.event_ended").withStyle(ChatFormatting.RED),
            false,
        )
    }

    private fun clearCandies() {
        for (candy in spawnedCandies) {
            if (candy.isAlive) {
                candy.removeLight()
                candy.discard()
            }
        }
        spawnedCandies.clear()
    }

    private fun registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(CandyChannelPayload.TYPE) { payload, ctx ->
            val player = ctx.player()
            val entity = player.level().getEntity(payload.candyEntityId) as? CandyEntity
            entity?.setChanneling(player.uuid, payload.isHolding)
        }

        ServerPlayNetworking.registerGlobalReceiver(MerchantBuyPayload.TYPE) { payload, ctx ->
            val player = ctx.player()
            handleMerchantPurchase(player, payload.itemKey)
        }

        ServerPlayNetworking.registerGlobalReceiver(SubmitGhostNamePayload.TYPE) { payload, ctx ->
            val player = ctx.player()
            handleGhostNaming(player, payload.petName)
        }
    }

    private fun handleMerchantPurchase(
        player: ServerPlayer,
        itemKey: String,
    ) {
        val cost = config.prices[itemKey] ?: return
        val mgr = balanceManager ?: return

        if (!mgr.spendCandies(player, cost)) {
            player.sendSystemMessage(
                Component.translatable("exservermod.halloween.not_enough_candies").withStyle(ChatFormatting.RED),
                true,
            )
            player.level().playSound(null, player.x, player.y, player.z, SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0f, 1.0f)
            return
        }

        player.level().playSound(null, player.x, player.y, player.z, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0f, 1.0f)

        when (itemKey) {
            "ghost_pet" -> {
                ServerPlayNetworking.send(player, OpenGhostNamingPayload.INSTANCE)
            }

            "pumpkin_bow" -> {
                player.addItem(ItemStack(HalloweenItems.PUMPKIN_BOW))
            }

            "golden_apple" -> {
                player.addItem(ItemStack(Items.ENCHANTED_GOLDEN_APPLE))
            }

            "golden_carrots" -> {
                player.addItem(ItemStack(Items.GOLDEN_CARROT, 64))
            }

            "totem" -> {
                player.addItem(ItemStack(Items.TOTEM_OF_UNDYING))
            }

            "fireworks_shulker" -> {
                player.addItem(createFireworksShulker())
            }

            "wither_skulls" -> {
                player.addItem(ItemStack(Items.WITHER_SKELETON_SKULL, 3))
            }

            "netherite_ingot" -> {
                player.addItem(ItemStack(Items.NETHERITE_INGOT))
            }

            "slime_blocks" -> {
                player.addItem(ItemStack(Items.SLIME_BLOCK, 16))
            }
        }

        // Refresh merchant GUI
        val newBalance = mgr.getBalance(player.uuid)
        val prices = activeMerchantWares.map { config.prices[it] ?: 100 }
        ServerPlayNetworking.send(player, OpenMerchantGuiPayload(newBalance, activeMerchantWares, prices))
    }

    private fun handleGhostNaming(
        player: ServerPlayer,
        petName: String,
    ) {
        val sLevel = player.level()

        // Despawn old pet if any
        val oldPetUuid = playerPets[player.uuid]
        if (oldPetUuid != null) {
            for (lvl in sLevel.server.allLevels.toList()) {
                val old = lvl.getEntity(oldPetUuid)
                old?.discard()
            }
        }

        val cleanName = petName.trim().ifEmpty { "Призрак" }
        val pet = GhostPetEntity(HalloweenEntities.GHOST_PET, sLevel)
        pet.ownerUuid = player.uuid
        pet.customName = Component.literal(cleanName)
        pet.setPos(player.x, player.y + 1.2, player.z)
        sLevel.addFreshEntity(pet)

        playerPets[player.uuid] = pet.uuid

        player.level().playSound(
            null,
            player.x,
            player.y,
            player.z,
            SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM,
            SoundSource.NEUTRAL,
            1.5f,
            1.2f,
        )
        player.sendSystemMessage(
            Component.translatable("exservermod.halloween.ghost_summoned", cleanName).withStyle(ChatFormatting.AQUA),
            false,
        )
    }

    private fun createFireworksShulker(): ItemStack {
        val shulker = ItemStack(Items.SHULKER_BOX)
        val rocket = ItemStack(Items.FIREWORK_ROCKET, 64)
        val explosion = FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL, IntArrayList(listOf(0xFFA500)), IntArrayList(), true, true)
        rocket.set(DataComponents.FIREWORKS, Fireworks(3, listOf(explosion)))

        val stacks = List(27) { rocket.copy() }
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks))
        return shulker
    }

    private fun registerPlayerJoin() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            if (phase == HalloweenPhase.ACTIVE_GATHERING) {
                ServerPlayNetworking.send(player, HalloweenSkySyncPayload(true))
            }
            // Check if player has pet in world
            val petUuid = playerPets[player.uuid]
            if (petUuid != null) {
                val server = player.level().server
                var found = false
                for (lvl in server.allLevels.toList()) {
                    if (lvl.getEntity(petUuid) != null) {
                        found = true
                        break
                    }
                }
                if (!found) {
                    val pet = GhostPetEntity(HalloweenEntities.GHOST_PET, player.level())
                    pet.ownerUuid = player.uuid
                    pet.customName = Component.literal("Призрак")
                    pet.setPos(player.x, player.y + 1.2, player.z)
                    player.level().addFreshEntity(pet)
                    playerPets[player.uuid] = pet.uuid
                }
            }
        }
    }

    private fun registerServerLifecycle() {
        ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
            clearCandies()
            balanceManager?.save()
        }
    }
}
