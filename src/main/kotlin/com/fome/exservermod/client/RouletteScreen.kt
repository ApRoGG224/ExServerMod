package com.fome.exservermod.client

import com.fome.exservermod.network.OpenRoulettePayload
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvents

class RouletteScreen(
    private val payload: OpenRoulettePayload,
) : Screen(Component.translatable("exservermod.roulette.title")) {
    private val items: List<String> = payload.mobCandidates + payload.targetMobId
    private var currentTick = 0
    private var currentIndex = 0
    private var lastSoundIndex = -1
    private var isCompleted = false

    companion object {
        const val SPIN_TICKS = 80
        const val LINGER_TICKS = 25
    }

    override fun isPauseScreen(): Boolean = false

    override fun tick() {
        super.tick()
        currentTick++

        if (currentTick <= SPIN_TICKS) {
            val progress = currentTick.toDouble() / SPIN_TICKS
            val eased = RouletteAnimation.easeOutCubic(progress)
            currentIndex = (eased * (items.size - 1)).toInt().coerceIn(0, items.size - 1)

            if (currentIndex != lastSoundIndex) {
                val pitch = 1.0f + (eased * 0.5f).toFloat()
                minecraft.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch))
                lastSoundIndex = currentIndex
            }

            if (currentTick == SPIN_TICKS) {
                isCompleted = true
                currentIndex = items.size - 1
                minecraft.soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f))
            }
        } else if (currentTick >= SPIN_TICKS + LINGER_TICKS) {
            onClose()
        }
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

        val boxWidth = 240
        val boxHeight = 100
        val boxLeft = cx - boxWidth / 2
        val boxTop = cy - boxHeight / 2
        val boxRight = cx + boxWidth / 2
        val boxBottom = cy + boxHeight / 2

        extractor.fill(boxLeft, boxTop, boxRight, boxBottom, 0xDD101010.toInt())
        val borderColor = if (isCompleted) 0xFF55FF55.toInt() else 0xFFFFAA00.toInt()
        extractor.outline(boxLeft, boxTop, boxWidth, boxHeight, borderColor)

        extractor.centeredText(font, title, cx, boxTop + 10, 0xFFFFFF)

        val statusText =
            if (isCompleted) {
                Component.translatable("exservermod.roulette.ready")
            } else {
                Component.translatable("exservermod.roulette.spinning")
            }
        val statusColor = if (isCompleted) 0x55FF55 else 0xAAAAAA
        extractor.centeredText(font, statusText, cx, boxTop + 26, statusColor)

        val slotWidth = 200
        val slotHeight = 24
        val slotLeft = cx - slotWidth / 2
        val slotTop = cy + 4
        extractor.fill(slotLeft, slotTop, slotLeft + slotWidth, slotTop + slotHeight, 0x88202020.toInt())
        val slotBorderColor = if (isCompleted) 0xFFFFFF00.toInt() else 0xFF666666.toInt()
        extractor.outline(slotLeft, slotTop, slotWidth, slotHeight, slotBorderColor)

        val currentMobId = items.getOrElse(currentIndex) { payload.targetMobId }
        val entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(currentMobId))
        val mobName = entityType.description
        val mobText = Component.translatable("exservermod.roulette.mob", mobName)
        val mobColor = if (isCompleted) 0xFFD700 else 0xFFFFFF
        extractor.centeredText(font, mobText, cx, slotTop + 8, mobColor)
    }
}
