package com.fome.exservermod.client

import com.fome.exservermod.network.OpenRoulettePayload
import com.fome.exservermod.network.RouletteCanceledPayload
import com.fome.exservermod.network.RouletteFinishedPayload
import com.fome.exservermod.sound.ModSounds
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class RouletteScreen(
    private val payload: OpenRoulettePayload,
) : Screen(Component.literal("MOB RANDOMIZER")) {
    private val entityCache = mutableMapOf<String, LivingEntity>()

    // Finalist reel: exactly 10 finalists ending on targetMobId
    private val finalists: List<String>
    private val targetReelIndex: Int
    private val carouselItems: List<String>

    // Candidate grid for responsive showcase & evaporation
    private val gridMobIds: List<String>
    private val finalistIndicesInGrid: Set<Int>

    // Timing constants (20 ticks = 1 sec)
    companion object {
        /**
         * Server-initiated force close has no direct accessor for the current
         * screen in 26.2, so the open wheel registers itself here.
         */
        var currentScreen: RouletteScreen? = null

        const val TICK_SHOWCASE_END = 30 // 1.5s
        const val TICK_EVAPORATE_END = 55 // 1.25s
        const val TICK_CONVERGE_END = 75 // 1.0s
        const val TICK_SPIN_STOP = 255 // 9.0s of spin
        const val TICK_REVEAL_END = 275 // 1.0s of victory reveal & confetti
        const val TOTAL_DURATION = 280 // ~14s total

        const val CARD_WIDTH = 96
        const val CARD_HEIGHT = 130
        const val CARD_GAP = 14
        const val CARD_STRIDE = CARD_WIDTH + CARD_GAP
    }

    private var currentTick = 0
    private var lastClickIndex = -1
    private var musicStarted = false
    private var revealPlayed = false
    private var spinSoundInstance: SimpleSoundInstance? = null
    private var finishedSent = false
    private var forceClosed = false

    // Particle systems
    private val confetti = mutableListOf<ConfettiParticle>()
    private val sparkles = mutableListOf<SparkleParticle>()

    init {
        currentScreen = this

        // Build 10 distinct finalists including the target mob
        val otherCandidates =
            payload.mobCandidates
                .filter { it != payload.targetMobId }
                .distinct()
                .shuffled()
        val finalistsPicked = (otherCandidates.take(9) + payload.targetMobId).shuffled().toMutableList()
        // Ensure target is properly embedded
        if (!finalistsPicked.contains(payload.targetMobId)) {
            finalistsPicked[finalistsPicked.size - 1] = payload.targetMobId
        }
        finalists = finalistsPicked

        // Grid contains up to 28 candidate mobs for showcase
        val allUnique = (payload.mobCandidates + payload.targetMobId).distinct()
        gridMobIds = allUnique.take(28)
        finalistIndicesInGrid =
            finalists
                .map { mobId -> gridMobIds.indexOf(mobId) }
                .filter { it >= 0 }
                .toSet()

        // Carousel strip: repeat items to provide dramatic momentum before stopping
        val loopCount = 4
        val repeated = mutableListOf<String>()
        for (r in 0 until loopCount) {
            repeated.addAll(finalists.shuffled())
        }
        // Insert winning target item at targetReelIndex
        targetReelIndex = repeated.size
        repeated.add(payload.targetMobId)
        repeated.addAll(finalists.take(5))
        carouselItems = repeated
    }

    override fun isPauseScreen(): Boolean = false

    override fun tick() {
        super.tick()
        currentTick++

        // 1. Play evaporation puff sound
        if (currentTick == TICK_SHOWCASE_END + 1) {
            minecraft.soundManager.play(
                SimpleSoundInstance.forUI(SoundEvents.CHORUS_FRUIT_TELEPORT, 1.2f, 0.7f),
            )
        }

        // 2. Play convergence whoosh
        if (currentTick == TICK_EVAPORATE_END + 1) {
            minecraft.soundManager.play(
                SimpleSoundInstance.forUI(SoundEvents.ENDER_EYE_DEATH, 0.9f, 0.6f),
            )
        }

        // 3. Start custom 10s music when spin begins
        if (currentTick == TICK_CONVERGE_END && !musicStarted) {
            musicStarted = true
            try {
                val sound =
                    SimpleSoundInstance(
                        ModSounds.ROULETTE_SPIN_ID,
                        SoundSource.MASTER,
                        1.0f,
                        1.0f,
                        SoundInstance.createUnseededRandom(),
                        false,
                        0,
                        SoundInstance.Attenuation.NONE,
                        0.0,
                        0.0,
                        0.0,
                        true,
                    )
                minecraft.soundManager.play(sound)
                spinSoundInstance = sound
            } catch (_: Throwable) {
            }
        }

        // 4. Spin progression & clicking
        if (currentTick in TICK_CONVERGE_END..TICK_SPIN_STOP) {
            val spinProgress = (currentTick - TICK_CONVERGE_END).toDouble() / (TICK_SPIN_STOP - TICK_CONVERGE_END)
            val eased = RouletteAnimation.easeOutCubic(spinProgress)
            val currentScrollIndex = (eased * targetReelIndex).toInt().coerceIn(0, carouselItems.size - 1)

            if (currentScrollIndex != lastClickIndex) {
                val pitch = 0.85f + (eased * 0.75f).toFloat()
                minecraft.soundManager.play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.8f),
                )
                lastClickIndex = currentScrollIndex
            }
        }

        // 5. Climax Reveal & Confetti Explosion
        if (currentTick >= TICK_SPIN_STOP && !revealPlayed) {
            revealPlayed = true
            minecraft.soundManager.play(
                SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f),
            )
            minecraft.soundManager.play(
                SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2f, 0.8f),
            )
            spawnConfettiExplosion(width / 2f, height / 2f)
        }

        // 6. Update Confetti particles
        val confettiIterator = confetti.iterator()
        while (confettiIterator.hasNext()) {
            val p = confettiIterator.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.22f // Gravity
            p.vx *= 0.985f // Drag
            p.rotation += p.rotSpeed
            p.alpha -= 0.007f
            if (p.alpha <= 0f || p.y > height + 20) {
                confettiIterator.remove()
            }
        }

        // 7. Update golden sparkle particles around center card
        if (currentTick >= TICK_CONVERGE_END) {
            if (sparkles.size < 35 && Random.nextFloat() < 0.6f) {
                val cx = width / 2f
                val cy = height / 2f
                val ox = (Random.nextFloat() - 0.5f) * (CARD_WIDTH + 30)
                val oy = (Random.nextFloat() - 0.5f) * (CARD_HEIGHT + 30)
                sparkles.add(
                    SparkleParticle(
                        x = cx + ox,
                        y = cy + oy,
                        vx = (Random.nextFloat() - 0.5f) * 0.6f,
                        vy = -Random.nextFloat() * 0.8f - 0.2f,
                        size = Random.nextFloat() * 3.5f + 2.0f,
                        color = if (Random.nextBoolean()) 0xFFFFD700.toInt() else 0xFF80EEFF.toInt(),
                        alpha = 1.0f,
                    ),
                )
            }
            val sparkleIterator = sparkles.iterator()
            while (sparkleIterator.hasNext()) {
                val s = sparkleIterator.next()
                s.x += s.vx
                s.y += s.vy
                s.alpha -= 0.025f
                if (s.alpha <= 0f) {
                    sparkleIterator.remove()
                }
            }
        }

        // 8. Completion & close
        if (currentTick >= TOTAL_DURATION) {
            finishAndClose()
        }
    }

    private fun finishAndClose() {
        if (!finishedSent) {
            finishedSent = true
            try {
                ClientPlayNetworking.send(RouletteFinishedPayload())
            } catch (_: Throwable) {
            }
        }
        onClose()
    }

    /**
     * Server-initiated close (game aborted / new round started). Must not send
     * a cancel payload back to the server.
     */
    fun forceClose() {
        forceClosed = true
        onClose()
    }

    override fun onClose() {
        currentScreen = null
        // Player dismissed the wheel without finishing it: tell the server so the
        // pending disguise is dropped and the round is aborted instead of continuing.
        if (!finishedSent && !forceClosed) {
            try {
                ClientPlayNetworking.send(RouletteCanceledPayload())
            } catch (_: Throwable) {
            }
        }
        spinSoundInstance?.let {
            minecraft.soundManager.stop(it)
        }
        entityCache.values.forEach { it.discard() }
        entityCache.clear()
        super.onClose()
    }

    override fun removed() {
        // Screen replaced without onClose (e.g. another GUI opened on top)
        if (currentScreen === this) {
            currentScreen = null
        }
        super.removed()
    }

    private fun spawnConfettiExplosion(
        originX: Float,
        originY: Float,
    ) {
        val colors =
            intArrayOf(
                0xFFFFD700.toInt(), // Gold
                0xFF00E5FF.toInt(), // Cyan
                0xFFFF1493.toInt(), // Deep Pink
                0xFFFF4500.toInt(), // Orange Red
                0xFF32CD32.toInt(), // Lime Green
                0xFF9B59B6.toInt(), // Purple
                0xFFFFFFFF.toInt(), // Diamond White
                0xFFFFEA00.toInt(), // Sun Yellow
            )

        for (i in 0 until 160) {
            val angle = Random.nextFloat() * 6.2831855f
            val speed = Random.nextFloat() * 9.0f + 2.5f
            val vx = cos(angle) * speed
            val vy = sin(angle) * speed - 4.5f // Initial upward burst
            val color = colors[Random.nextInt(colors.size)]
            val size = Random.nextFloat() * 4.5f + 3.0f
            confetti.add(
                ConfettiParticle(
                    x = originX,
                    y = originY,
                    vx = vx,
                    vy = vy,
                    rotation = Random.nextFloat() * 360f,
                    rotSpeed = (Random.nextFloat() - 0.5f) * 14f,
                    color = color,
                    size = size,
                    alpha = 1.0f,
                ),
            )
        }
    }

    private fun getOrCreateEntity(mobId: String): LivingEntity? {
        val existing = entityCache[mobId]
        if (existing != null) return existing
        val level = minecraft.level ?: return null
        val id = Identifier.tryParse(mobId) ?: return null
        val type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null) ?: return null
        val created = type.create(level, EntitySpawnReason.EVENT) as? LivingEntity ?: return null

        created.id = -1000 - entityCache.size - 1
        val px = minecraft.player?.x ?: 0.0
        val py = minecraft.player?.y ?: 64.0
        val pz = minecraft.player?.z ?: 0.0
        created.setPos(px, py, pz)
        created.xo = px
        created.yo = py
        created.zo = pz
        created.xOld = px
        created.yOld = py
        created.zOld = pz
        created.tickCount = 10

        entityCache[mobId] = created
        return created
    }

    override fun extractRenderState(
        extractor: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        extractTransparentBackground(extractor)
        super.extractRenderState(extractor, mouseX, mouseY, partialTick)

        val cx = width / 2
        val cy = height / 2

        // Top Header: "MOB RANDOMIZER" with fiery golden gradient layers
        renderTitleHeader(extractor, cx, 16)

        // Phase 1 & 2: Responsive Candidate Grid (Showcase -> Evaporate -> Fly-in)
        if (currentTick < TICK_CONVERGE_END) {
            renderGridShowcase(extractor, mouseX, mouseY, partialTick)
        }

        // Phase 3 & 4: The Roulette Reel Strip
        if (currentTick >= TICK_EVAPORATE_END) {
            renderCarousel(extractor, cx, cy, mouseX, mouseY, partialTick)
        }

        // Render Particles (Confetti & Golden Sparkles)
        renderParticles(extractor)
    }

    private fun renderTitleHeader(
        extractor: GuiGraphicsExtractor,
        cx: Int,
        topY: Int,
    ) {
        val titleText = "MOB RANDOMIZER"

        // Dramatic multi-layer drop shadow
        extractor.centeredText(font, titleText, cx + 2, topY + 2, 0xAA2A0800.toInt())
        extractor.centeredText(font, titleText, cx + 1, topY + 1, 0xEE7A2600.toInt())
        // Brilliant fiery golden text
        extractor.centeredText(font, titleText, cx, topY, 0xFFFFD700.toInt())

        // Subtitle status
        val status =
            when {
                currentTick < TICK_SHOWCASE_END -> "✦ CANDIDATE SHOWCASE ✦"
                currentTick < TICK_EVAPORATE_END -> "✦ EVAPORATING NON-FINALISTS ✦"
                currentTick < TICK_CONVERGE_END -> "✦ CONVERGING TO WHEEL ✦"
                currentTick < TICK_SPIN_STOP -> "✦ SPINNING MOB WHEEL ✦"
                else -> "★ DISGUISE SELECTED ★"
            }
        val statusColor = if (currentTick >= TICK_SPIN_STOP) 0xFF55FF55.toInt() else 0xFFFFAA00.toInt()
        extractor.centeredText(font, status, cx, topY + 13, statusColor)
    }

    private fun renderGridShowcase(
        extractor: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        // Compute responsive grid layout based on screen width/height
        val cellW = 60
        val cellH = 50
        val cols = ((width - 40) / (cellW + 6)).coerceIn(4, 7)
        val rows = ((height - 85) / (cellH + 6)).coerceIn(3, 4)
        val maxGrid = cols * rows
        val displayMobs = gridMobIds.take(maxGrid)

        val gridWidth = cols * (cellW + 6) - 6
        val gridHeight = rows * (cellH + 6) - 6
        val startX = (width - gridWidth) / 2
        val startY = (height - gridHeight) / 2 + 12

        // Evaporation alpha factor
        val evaporateAlpha =
            if (currentTick in TICK_SHOWCASE_END..TICK_EVAPORATE_END) {
                1.0f - (currentTick - TICK_SHOWCASE_END + partialTick) / (TICK_EVAPORATE_END - TICK_SHOWCASE_END)
            } else if (currentTick > TICK_EVAPORATE_END) {
                0.0f
            } else {
                1.0f
            }

        // Convergence progress
        val convergeProgress =
            if (currentTick in TICK_EVAPORATE_END..TICK_CONVERGE_END) {
                val p = ((currentTick - TICK_EVAPORATE_END + partialTick) / (TICK_CONVERGE_END - TICK_EVAPORATE_END)).coerceIn(0f, 1f)
                RouletteAnimation.easeOutCubic(p.toDouble()).toFloat()
            } else {
                0.0f
            }

        val carouselY = height / 2 - CARD_HEIGHT / 2

        for (i in displayMobs.indices) {
            val mobId = displayMobs[i]
            val isFinalist = finalistIndicesInGrid.contains(i)

            // If non-finalist during evaporate/converge, handle fade-out
            if (!isFinalist && evaporateAlpha <= 0.01f) {
                continue
            }

            val col = i % cols
            val row = i / cols
            val gridX = startX + col * (cellW + 6)
            val gridY = startY + row * (cellH + 6)

            var renderX = gridX
            var renderY = gridY
            var renderW = cellW
            var renderH = cellH

            if (isFinalist && currentTick >= TICK_EVAPORATE_END) {
                // Interpolate finalist towards carousel center
                val finalistIndex = finalists.indexOf(mobId).coerceAtLeast(0)
                val targetCarouselX = (width / 2) + (finalistIndex - 4) * CARD_STRIDE - CARD_WIDTH / 2
                renderX = (gridX + (targetCarouselX - gridX) * convergeProgress).toInt()
                renderY = (gridY + (carouselY - gridY) * convergeProgress).toInt()
                renderW = (cellW + (CARD_WIDTH - cellW) * convergeProgress).toInt()
                renderH = (cellH + (CARD_HEIGHT - cellH) * convergeProgress).toInt()
            }

            val alphaByte =
                if (isFinalist) {
                    240
                } else {
                    (evaporateAlpha.coerceIn(0f, 1f) * 200).toInt()
                }

            val bgCol = (alphaByte shl 24) or 0x14161C
            val borderCol =
                if (isFinalist) {
                    (alphaByte shl 24) or 0xFFD700
                } else {
                    (alphaByte shl 24) or 0x444455
                }

            extractor.fill(renderX, renderY, renderX + renderW, renderY + renderH, bgCol)
            extractor.outline(renderX, renderY, renderW, renderH, borderCol)

            // Render mini entity inside grid card
            val entity = getOrCreateEntity(mobId)
            if (entity != null && alphaByte > 30) {
                val maxDim =
                    max(entity.boundingBox.ysize.toFloat(), max(entity.boundingBox.xsize.toFloat(), entity.boundingBox.zsize.toFloat()))
                val scale = if (maxDim > 0.1f) min(20, (18 / maxDim).toInt()) else 14
                try {
                    InventoryScreen.extractEntityInInventoryFollowsMouse(
                        extractor,
                        renderX + 2,
                        renderY + 2,
                        renderX + renderW - 2,
                        renderY + renderH - 12,
                        scale,
                        0.0625f,
                        mouseX.toFloat(),
                        mouseY.toFloat(),
                        entity,
                    )
                } catch (_: Throwable) {
                }
            }

            // Name
            if (alphaByte > 40) {
                val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(mobId)).orElse(null)
                val mobName = entityType?.description?.string ?: mobId
                val shortName = if (font.width(mobName) > renderW - 4) font.plainSubstrByWidth(mobName, renderW - 8) + "." else mobName
                val nameColor = if (isFinalist) 0xFFFFE066.toInt() else 0xFFAAAAAA.toInt()
                extractor.centeredText(font, shortName, renderX + renderW / 2, renderY + renderH - 10, nameColor)
            }
        }
    }

    private fun renderCarousel(
        extractor: GuiGraphicsExtractor,
        cx: Int,
        cy: Int,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        val cardTop = cy - CARD_HEIGHT / 2
        val cardBottom = cy + CARD_HEIGHT / 2
        val carouselTop = cardTop - 14
        val carouselBottom = cardBottom + 14

        // Carousel entry fade-in during converge phase
        val carouselAlpha =
            if (currentTick in TICK_EVAPORATE_END..TICK_CONVERGE_END) {
                ((currentTick - TICK_EVAPORATE_END + partialTick) / (TICK_CONVERGE_END - TICK_EVAPORATE_END)).coerceIn(0f, 1f)
            } else {
                1.0f
            }

        val bandBg = ((carouselAlpha * 170).toInt() shl 24) or 0x0A0D12
        val bandLine = ((carouselAlpha * 120).toInt() shl 24) or 0x334455

        // Backdrop strip for cards carousel
        extractor.fill(0, carouselTop, width, carouselBottom, bandBg)
        extractor.fill(0, carouselTop, width, carouselTop + 1, bandLine)
        extractor.fill(0, carouselBottom - 1, width, carouselBottom, bandLine)

        // Smooth scroll progress
        val scrollProgress =
            if (currentTick < TICK_CONVERGE_END) {
                0.0
            } else if (currentTick >= TICK_SPIN_STOP) {
                targetReelIndex.toDouble()
            } else {
                val progress =
                    ((currentTick - TICK_CONVERGE_END + partialTick) / (TICK_SPIN_STOP - TICK_CONVERGE_END)).toDouble().coerceIn(0.0, 1.0)
                RouletteAnimation.easeOutCubic(progress) * targetReelIndex
            }

        // Scissor viewport to clip carousel edges smoothly
        extractor.enableScissor(0, carouselTop - 12, width, carouselBottom + 12)

        for (i in carouselItems.indices) {
            val cardCenterX = cx + ((i - scrollProgress) * CARD_STRIDE).toInt()
            val cardLeft = cardCenterX - CARD_WIDTH / 2
            val cardRight = cardLeft + CARD_WIDTH

            if (cardRight < -20 || cardLeft > width + 20) {
                continue
            }

            val isTargetSlot = (i == targetReelIndex)
            val isFinalRevealed = isTargetSlot && (currentTick >= TICK_SPIN_STOP)
            val isCenterFocused = (cardCenterX - cx).let { it >= -CARD_WIDTH / 2 && it <= CARD_WIDTH / 2 }

            // Card backgrounds & borders matching the prototype
            val mobId = carouselItems[i]
            renderCarouselCard(
                extractor,
                cardLeft,
                cardTop,
                cardRight,
                cardBottom,
                mobId,
                isFinalRevealed,
                isCenterFocused,
                mouseX,
                mouseY,
            )
        }

        extractor.disableScissor()

        // Center golden selector arrows pointing at the winning slot (matching prototype)
        renderCenterSelectorArrows(extractor, cx, cardTop, cardBottom)
    }

    private fun renderCarouselCard(
        extractor: GuiGraphicsExtractor,
        cardLeft: Int,
        cardTop: Int,
        cardRight: Int,
        cardBottom: Int,
        mobId: String,
        isFinalRevealed: Boolean,
        isCenterFocused: Boolean,
        mouseX: Int,
        mouseY: Int,
    ) {
        val cardW = cardRight - cardLeft
        val cardH = cardBottom - cardTop
        val cardCx = cardLeft + cardW / 2

        if (isCenterFocused || isFinalRevealed) {
            // Highlighted Golden Card (matching center card in prototype)
            // Outer golden glow
            extractor.fill(cardLeft - 2, cardTop - 2, cardRight + 2, cardBottom + 2, 0x44FFD700)
            // Rich inner dark background
            extractor.fill(cardLeft, cardTop, cardRight, cardBottom, 0xEE16130C.toInt())
            // Ornate double border
            extractor.outline(cardLeft, cardTop, cardW, cardH, 0xFFFFD700.toInt())
            extractor.outline(cardLeft + 2, cardTop + 2, cardW - 4, cardH - 4, 0xFFFFAA00.toInt())

            // 4 Corner diamonds (✦) in aqua/diamond blue as in prototype
            val diamondColor = 0xFF80EEFF.toInt()
            extractor.text(font, "✦", cardLeft + 4, cardTop + 4, diamondColor)
            extractor.text(font, "✦", cardRight - 10, cardTop + 4, diamondColor)
            extractor.text(font, "✦", cardLeft + 4, cardBottom - 14, diamondColor)
            extractor.text(font, "✦", cardRight - 10, cardBottom - 14, diamondColor)
        } else {
            // Neighbor card (dark themed frame)
            extractor.fill(cardLeft, cardTop, cardRight, cardBottom, 0xDD12131A.toInt())
            extractor.outline(cardLeft, cardTop, cardW, cardH, 0xFF353C4D.toInt())
            // Corner rivets
            extractor.fill(cardLeft + 2, cardTop + 2, cardLeft + 4, cardTop + 4, 0xFF882222.toInt())
            extractor.fill(cardRight - 4, cardTop + 2, cardRight - 2, cardTop + 4, 0xFF882222.toInt())
            extractor.fill(cardLeft + 2, cardBottom - 4, cardLeft + 4, cardBottom - 2, 0xFF882222.toInt())
            extractor.fill(cardRight - 4, cardBottom - 4, cardRight - 2, cardBottom - 2, 0xFF882222.toInt())
        }

        // 3D Living Mob Model
        val entity = getOrCreateEntity(mobId)
        if (entity != null) {
            val maxDim =
                max(entity.boundingBox.ysize.toFloat(), max(entity.boundingBox.xsize.toFloat(), entity.boundingBox.zsize.toFloat()))
            val scale = if (maxDim > 0.1f) min(36, (38 / maxDim).toInt()) else 26
            try {
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                    extractor,
                    cardLeft + 6,
                    cardTop + 10,
                    cardRight - 6,
                    cardBottom - 24,
                    scale,
                    0.0625f,
                    mouseX.toFloat(),
                    mouseY.toFloat(),
                    entity,
                )
            } catch (_: Throwable) {
            }
        }

        // Mob Name Banner at bottom of card
        val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(mobId)).orElse(null)
        val mobName = entityType?.description?.string ?: mobId
        val displayName =
            if (font.width(mobName) > cardW - 8) {
                font.plainSubstrByWidth(mobName, cardW - 14) + ".."
            } else {
                mobName
            }

        val namePlateY = cardBottom - 20
        if (isCenterFocused || isFinalRevealed) {
            extractor.fill(cardLeft + 3, namePlateY, cardRight - 3, cardBottom - 3, 0xEE2A1E05.toInt())
            extractor.outline(cardLeft + 3, namePlateY, cardW - 6, 17, 0xFFFFD700.toInt())
            extractor.centeredText(font, displayName, cardCx, namePlateY + 4, 0xFFFFE066.toInt())
        } else {
            extractor.fill(cardLeft + 3, namePlateY, cardRight - 3, cardBottom - 3, 0xCC0E1015.toInt())
            extractor.outline(cardLeft + 3, namePlateY, cardW - 6, 17, 0xFF2A2E3D.toInt())
            extractor.centeredText(font, displayName, cardCx, namePlateY + 4, 0xFFC0C5D0.toInt())
        }
    }

    private fun renderCenterSelectorArrows(
        extractor: GuiGraphicsExtractor,
        cx: Int,
        cardTop: Int,
        cardBottom: Int,
    ) {
        val arrowColor = 0xFFFFD700.toInt()
        val highlightColor = 0xFFFFF2AA.toInt()

        // Top Golden Arrow pointing DOWN (matching prototype)
        extractor.fill(cx - 7, cardTop - 12, cx + 8, cardTop - 10, arrowColor)
        extractor.fill(cx - 5, cardTop - 10, cx + 6, cardTop - 8, arrowColor)
        extractor.fill(cx - 3, cardTop - 8, cx + 4, cardTop - 6, arrowColor)
        extractor.fill(cx - 2, cardTop - 6, cx + 3, cardTop - 4, highlightColor)
        extractor.fill(cx - 1, cardTop - 4, cx + 2, cardTop - 2, highlightColor)

        // Bottom Golden Arrow pointing UP (matching prototype)
        extractor.fill(cx - 1, cardBottom + 2, cx + 2, cardBottom + 4, highlightColor)
        extractor.fill(cx - 2, cardBottom + 4, cx + 3, cardBottom + 6, highlightColor)
        extractor.fill(cx - 3, cardBottom + 6, cx + 4, cardBottom + 8, arrowColor)
        extractor.fill(cx - 5, cardBottom + 8, cx + 6, cardBottom + 10, arrowColor)
        extractor.fill(cx - 7, cardBottom + 10, cx + 8, cardBottom + 12, arrowColor)
    }

    private fun renderParticles(extractor: GuiGraphicsExtractor) {
        // Render Confetti
        for (p in confetti) {
            val alphaByte = (p.alpha.coerceIn(0f, 1f) * 255).toInt()
            val colorWithAlpha = (alphaByte shl 24) or (p.color and 0x00FFFFFF)
            val s = p.size
            extractor.fill(p.x.toInt(), p.y.toInt(), (p.x + s).toInt(), (p.y + s * 0.65f).toInt(), colorWithAlpha)
        }

        // Render Golden Sparkles
        for (s in sparkles) {
            val alphaByte = (s.alpha.coerceIn(0f, 1f) * 255).toInt()
            val col = (alphaByte shl 24) or (s.color and 0x00FFFFFF)
            val sz = s.size.toInt()
            extractor.fill(s.x.toInt(), s.y.toInt(), s.x.toInt() + sz, s.y.toInt() + sz, col)
        }
    }

    private data class ConfettiParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var rotation: Float,
        val rotSpeed: Float,
        val color: Int,
        val size: Float,
        var alpha: Float,
    )

    private data class SparkleParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        val color: Int,
        var alpha: Float,
    )
}
