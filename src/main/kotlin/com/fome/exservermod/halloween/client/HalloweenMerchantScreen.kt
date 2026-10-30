package com.fome.exservermod.halloween.client

import com.fome.exservermod.halloween.HalloweenItems
import com.fome.exservermod.halloween.network.MerchantBuyPayload
import com.fome.exservermod.halloween.network.OpenMerchantGuiPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.math.max

class HalloweenMerchantScreen(
    private var payload: OpenMerchantGuiPayload,
) : Screen(Component.translatable("exservermod.halloween.merchant_gui_title")) {
    private val rowHeight = 24
    private val panelWidth = 320

    private fun getPanelHeight(): Int = 40 + payload.wares.size * rowHeight + 32

    private fun getPanelLeft(): Int = (width - panelWidth) / 2

    private fun getPanelRight(): Int = getPanelLeft() + panelWidth

    private fun getPanelTop(): Int = max(6, (height - getPanelHeight()) / 2)

    private fun getPanelBottom(): Int = getPanelTop() + getPanelHeight()

    private fun getStartY(): Int = getPanelTop() + 36

    private fun getWareStack(wareKey: String): ItemStack =
        when (wareKey) {
            "pumpkin_bow" -> ItemStack(HalloweenItems.PUMPKIN_BOW)
            "ghost_pet" -> {
                val stack = ItemStack(Items.ALLAY_SPAWN_EGG)
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("exservermod.ware.ghost_pet"))
                stack
            }
            "golden_apple" -> ItemStack(Items.ENCHANTED_GOLDEN_APPLE)
            "golden_carrots" -> ItemStack(Items.GOLDEN_CARROT, 64)
            "totem" -> ItemStack(Items.TOTEM_OF_UNDYING)
            "fireworks_shulker" -> {
                val stack = ItemStack(Items.SHULKER_BOX)
                stack.set(DataComponents.CUSTOM_NAME, Component.translatable("exservermod.ware.fireworks_shulker"))
                stack
            }
            "wither_skulls" -> ItemStack(Items.WITHER_SKELETON_SKULL, 3)
            "netherite_ingot" -> ItemStack(Items.NETHERITE_INGOT)
            "slime_blocks" -> ItemStack(Items.SLIME_BLOCK, 16)
            else -> {
                val id = Identifier.tryParse(wareKey)
                if (id != null) {
                    val item = BuiltInRegistries.ITEM.getOptional(id).orElse(null)
                    if (item != null) ItemStack(item) else ItemStack(Items.PUMPKIN)
                } else {
                    ItemStack(Items.PUMPKIN)
                }
            }
        }

    fun updatePayload(newPayload: OpenMerchantGuiPayload) {
        this.payload = newPayload
        clearWidgets()
        init()
    }

    override fun init() {
        super.init()

        val panelRight = getPanelRight()
        val startY = getStartY()
        val buttonWidth = 60
        val buttonHeight = 20
        val buttonX = panelRight - 12 - buttonWidth

        payload.wares.forEachIndexed { index, wareKey ->
            val price = payload.prices.getOrElse(index) { 100 }
            val y = startY + (index * rowHeight)
            val canAfford = payload.balance >= price

            val buyButton =
                Button
                    .builder(Component.translatable("exservermod.halloween.buy")) {
                        ClientPlayNetworking.send(MerchantBuyPayload(wareKey))
                    }.bounds(buttonX, y + 2, buttonWidth, buttonHeight)
                    .build()
            buyButton.active = canAfford
            addRenderableWidget(buyButton)
        }

        val centerX = width / 2
        val closeButton =
            Button
                .builder(Component.translatable("exservermod.halloween.close")) {
                    onClose()
                }.bounds(centerX - 40, getPanelBottom() - 26, 80, 20)
                .build()
        addRenderableWidget(closeButton)
    }

    override fun extractRenderState(
        extractor: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        extractTransparentBackground(extractor)

        val cx = width / 2
        val pLeft = getPanelLeft()
        val pRight = getPanelRight()
        val pTop = getPanelTop()
        val pBottom = getPanelBottom()
        val pWidth = panelWidth
        val pHeight = getPanelHeight()

        // 1. Halloween styled panel background and borders
        extractor.fill(pLeft - 2, pTop - 2, pRight + 2, pBottom + 2, 0x44FFA500.toInt())
        extractor.fill(pLeft, pTop, pRight, pBottom, 0xEE141218.toInt())
        extractor.outline(pLeft, pTop, pWidth, pHeight, 0xFFFFA500.toInt())
        extractor.outline(pLeft + 2, pTop + 2, pWidth - 4, pHeight - 4, 0xFF5C3317.toInt())

        // 2. Title & Balance header
        val title = Component.translatable("exservermod.halloween.merchant_gui_title")
        extractor.centeredText(font, title, cx + 1, pTop + 9, 0xAA4A2000.toInt())
        extractor.centeredText(font, title, cx, pTop + 8, 0xFFFF9900.toInt())

        val balance = Component.translatable("exservermod.halloween.merchant_gui_balance", payload.balance)
        extractor.centeredText(font, balance, cx, pTop + 22, 0xFFFFFF55.toInt())

        // 3. Render each ware row
        val startY = getStartY()
        val buttonWidth = 60
        val buttonX = pRight - 12 - buttonWidth
        var hoveredStack: ItemStack? = null

        payload.wares.forEachIndexed { index, wareKey ->
            val price = payload.prices.getOrElse(index) { 100 }
            val y = startY + (index * rowHeight)
            val stack = getWareStack(wareKey)

            // Slot box
            val slotX = pLeft + 12
            val slotY = y + 2
            val slotSize = 20
            extractor.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0xDD12131A.toInt())
            extractor.outline(slotX, slotY, slotSize, slotSize, 0xFF3E4352.toInt())

            // Item icon & count decorations
            if (!stack.isEmpty) {
                extractor.item(stack, slotX + 2, slotY + 2)
                extractor.itemDecorations(font, stack, slotX + 2, slotY + 2)
            }

            // Price text (aligned to the left of the Buy button)
            val canAfford = payload.balance >= price
            val cost = Component.translatable("exservermod.halloween.cost", price)
            val costColor = if (canAfford) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
            val costWidth = font.width(cost)
            val costX = buttonX - 8 - costWidth
            extractor.text(font, cost, costX, y + 8, costColor)

            // Item name (between slot and price)
            val itemName = Component.translatable("exservermod.ware.$wareKey")
            val nameX = slotX + slotSize + 6
            val maxNameWidth = costX - nameX - 4
            val nameStr = itemName.string
            val displayStr =
                if (font.width(nameStr) > maxNameWidth && maxNameWidth > 20) {
                    font.plainSubstrByWidth(nameStr, maxNameWidth - 8) + "…"
                } else {
                    nameStr
                }
            extractor.text(font, displayStr, nameX, y + 8, 0xFFFFFFFF.toInt())

            // Check if hovered over slot or name to show tooltip
            if (mouseX in slotX until costX && mouseY in slotY..(slotY + slotSize)) {
                hoveredStack = stack
            }
        }

        // 4. Render buttons on top
        super.extractRenderState(extractor, mouseX, mouseY, partialTick)

        // 5. Tooltip rendered on top of everything
        if (hoveredStack != null && !hoveredStack.isEmpty) {
            extractor.setTooltipForNextFrame(font, hoveredStack, mouseX, mouseY)
        }
    }

    override fun isPauseScreen(): Boolean = false
}
