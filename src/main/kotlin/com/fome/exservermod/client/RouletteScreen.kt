package com.fome.exservermod.client

import com.fome.exservermod.network.OpenRoulettePayload
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import kotlin.math.max
import kotlin.math.min

class RouletteScreen(
    private val payload: OpenRoulettePayload,
) : Screen(Component.translatable("exservermod.roulette.title")) {
    private val targetIndex = payload.mobCandidates.size
    private val items: List<String> = payload.mobCandidates + payload.targetMobId + payload.mobCandidates.take(5)
    private val entityCache = mutableMapOf<String, LivingEntity>()

    private var currentTick = 0
    private var lastSoundIndex = -1
    private var isCompleted = false

    companion object {
        const val SPIN_TICKS = 80
        const val LINGER_TICKS = 30
        const val CARD_WIDTH = 80
        const val CARD_HEIGHT = 100
        const val CARD_GAP = 12
        const val CARD_STRIDE = CARD_WIDTH + CARD_GAP
    }

    override fun isPauseScreen(): Boolean = false

    override fun tick() {
        super.tick()
        currentTick++

        if (currentTick <= SPIN_TICKS) {
            val progress = currentTick.toDouble() / SPIN_TICKS
            val eased = RouletteAnimation.easeOutCubic(progress)
            val scroll = eased * targetIndex
            val currentIndex = scroll.toInt().coerceIn(0, items.size - 1)

            if (currentIndex != lastSoundIndex) {
                val pitch = 0.8f + (eased * 0.8f).toFloat()
                minecraft.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch))
                lastSoundIndex = currentIndex
            }

            if (currentTick == SPIN_TICKS) {
                isCompleted = true
                minecraft.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f))
            }
        } else if (currentTick >= SPIN_TICKS + LINGER_TICKS) {
            onClose()
        }
    }

    override fun onClose() {
        entityCache.values.forEach { it.discard() }
        entityCache.clear()
        super.onClose()
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

        val cardTop = cy - CARD_HEIGHT / 2
        val cardBottom = cy + CARD_HEIGHT / 2
        val carouselTop = cardTop - 10
        val carouselBottom = cardBottom + 10

        // Backdrop band for the cards carousel
        extractor.fill(0, carouselTop, width, carouselBottom, 0xAA080808.toInt())
        extractor.fill(0, carouselTop, width, carouselTop + 1, 0x44FFFFFF)
        extractor.fill(0, carouselBottom - 1, width, carouselBottom, 0x44FFFFFF)

        // Title and status text
        extractor.centeredText(font, title, cx, carouselTop - 34, 0xFFFFFF)

        val statusText =
            if (isCompleted) {
                Component.translatable("exservermod.roulette.ready")
            } else {
                Component.translatable("exservermod.roulette.spinning")
            }
        val statusColor = if (isCompleted) 0x55FF55 else 0xAAAAAA
        extractor.centeredText(font, statusText, cx, carouselTop - 18, statusColor)

        // Smooth continuous scroll position
        val scrollProgress =
            if (isCompleted) {
                targetIndex.toDouble()
            } else {
                val progress = ((currentTick - 1 + partialTick).coerceAtLeast(0f) / SPIN_TICKS).toDouble().coerceIn(0.0, 1.0)
                RouletteAnimation.easeOutCubic(progress) * targetIndex
            }

        // Render visible cards
        for (i in items.indices) {
            val cardCenterX = cx + ((i - scrollProgress) * CARD_STRIDE).toInt()
            val cardLeft = cardCenterX - CARD_WIDTH / 2
            val cardRight = cardLeft + CARD_WIDTH

            if (cardRight < 0 || cardLeft > width) {
                continue
            }

            val isWinningCard = (i == targetIndex)
            val isSelected = isWinningCard && isCompleted

            val bgColor = if (isSelected) 0xEE1E281E.toInt() else 0xCC151515.toInt()
            extractor.fill(cardLeft, cardTop, cardRight, cardBottom, bgColor)

            val borderColor =
                when {
                    isSelected -> 0xFFFFD700.toInt()
                    isWinningCard -> 0xFFFFAA00.toInt()
                    else -> 0xFF444444.toInt()
                }
            extractor.outline(cardLeft, cardTop, CARD_WIDTH, CARD_HEIGHT, borderColor)

            val mobId = items[i]
            val entity = getOrCreateEntity(mobId)
            if (entity != null) {
                val maxDim =
                    max(entity.boundingBox.ysize.toFloat(), max(entity.boundingBox.xsize.toFloat(), entity.boundingBox.zsize.toFloat()))
                val scale = if (maxDim > 0.1f) min(32, (38 / maxDim).toInt()) else 24

                val renderX0 = cardLeft + 4
                val renderY0 = cardTop + 6
                val renderX1 = cardRight - 4
                val renderY1 = cardBottom - 18

                try {
                    InventoryScreen.extractEntityInInventoryFollowsMouse(
                        extractor,
                        renderX0,
                        renderY0,
                        renderX1,
                        renderY1,
                        scale,
                        0.0625f,
                        mouseX.toFloat(),
                        mouseY.toFloat(),
                        entity,
                    )
                } catch (_: Throwable) {
                }
            }

            val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(mobId)).orElse(null)
            val mobName = entityType?.description?.string ?: mobId
            val displayName =
                if (font.width(mobName) > CARD_WIDTH - 6) {
                    font.plainSubstrByWidth(mobName, CARD_WIDTH - 12) + ".."
                } else {
                    mobName
                }
            val nameColor = if (isSelected) 0xFFFFD700.toInt() else 0xFFCCCCCC.toInt()
            extractor.centeredText(font, displayName, cardCenterX, cardBottom - 12, nameColor)
        }

        // Center selector indicator needle
        val indicatorColor = if (isCompleted) 0xFFFFD700.toInt() else 0xFFFF5555.toInt()
        extractor.fill(cx - 2, carouselTop - 4, cx + 2, carouselTop + 4, indicatorColor)
        extractor.fill(cx - 2, carouselBottom - 4, cx + 2, carouselBottom + 4, indicatorColor)
    }
}
