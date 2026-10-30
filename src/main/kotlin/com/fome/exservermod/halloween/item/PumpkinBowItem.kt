package com.fome.exservermod.halloween.item

import com.fome.exservermod.halloween.entity.PumpkinProjectileEntity
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BowItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level
import java.util.function.Consumer

class PumpkinBowItem(
    properties: Item.Properties,
) : BowItem(properties) {
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        display: TooltipDisplay,
        tooltipOutput: Consumer<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        tooltipOutput.accept(
            Component
                .translatable("item.exservermod.pumpkin_bow.desc")
                .withStyle(ChatFormatting.GOLD),
        )
        tooltipOutput.accept(
            Component
                .translatable("item.exservermod.pumpkin_bow.lore")
                .withStyle(ChatFormatting.DARK_AQUA),
        )
    }

    override fun use(
        level: Level,
        player: Player,
        hand: InteractionHand,
    ): InteractionResult {
        val hasAmmo = player.abilities.instabuild || findPumpkinAmmo(player) != null
        return if (!hasAmmo) {
            if (level is ServerLevel) {
                val sPlayer = player as? ServerPlayer
                val msg = Component.translatable("exservermod.item.pumpkin_bow.no_ammo").withStyle(ChatFormatting.RED)
                sPlayer?.sendSystemMessage(msg, true) ?: player.sendSystemMessage(msg)

                level.playSound(
                    null,
                    player.x,
                    player.y,
                    player.z,
                    SoundEvents.DISPENSER_FAIL,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.2f,
                )
            }
            InteractionResult.FAIL
        } else {
            player.startUsingItem(hand)
            InteractionResult.CONSUME
        }
    }

    override fun releaseUsing(
        stack: ItemStack,
        level: Level,
        livingEntity: LivingEntity,
        timeLeft: Int,
    ): Boolean {
        if (livingEntity !is Player) return false

        val duration = getUseDuration(stack, livingEntity) - timeLeft
        val power = getPowerForTime(duration)
        if (power < 0.15f) return false

        val sLevel = level as? ServerLevel ?: return true
        val isCreative = livingEntity.abilities.instabuild
        val ammoStack = findPumpkinAmmo(livingEntity)

        if (!isCreative && ammoStack == null) {
            val sPlayer = livingEntity as? ServerPlayer
            val msg = Component.translatable("exservermod.item.pumpkin_bow.no_ammo").withStyle(ChatFormatting.RED)
            sPlayer?.sendSystemMessage(msg, true) ?: livingEntity.sendSystemMessage(msg)
            return false
        }

        if (!isCreative && ammoStack != null) {
            ammoStack.shrink(1)
        }

        val projectile = PumpkinProjectileEntity(sLevel, livingEntity)
        projectile.shootFromRotation(
            livingEntity,
            livingEntity.xRot,
            livingEntity.yRot,
            0.0f,
            power * 2.8f,
            1.0f,
        )
        sLevel.addFreshEntity(projectile)

        sLevel.playSound(
            null,
            livingEntity.x,
            livingEntity.y,
            livingEntity.z,
            SoundEvents.ARROW_SHOOT,
            SoundSource.PLAYERS,
            1.0f,
            0.7f,
        )

        if (!isCreative) {
            val slot = if (livingEntity.usedItemHand == InteractionHand.MAIN_HAND) EquipmentSlot.MAINHAND else EquipmentSlot.OFFHAND
            stack.hurtAndBreak(1, livingEntity, slot)
        }

        return true
    }

    private fun findPumpkinAmmo(player: Player): ItemStack? {
        val inventory = player.inventory
        for (i in 0 until inventory.containerSize) {
            val itemStack = inventory.getItem(i)
            if (isPumpkin(itemStack)) {
                return itemStack
            }
        }
        return null
    }

    private fun isPumpkin(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val item = stack.item
        return item == Items.CARVED_PUMPKIN || item == Items.PUMPKIN || item == Items.JACK_O_LANTERN
    }
}
