package com.fome.exservermod.stealth

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AbstractCauldronBlock
import net.minecraft.world.level.block.HopperBlock

object CauldronStealthUtils {
    const val CROUCH_Y_OFFSET: Double = 0.125 // 2.0 / 16.0 blocks to cancel MC-48191 / MC-228666

    @JvmStatic
    fun isInHollowBlock(entity: Entity): Boolean {
        val level = entity.level()
        val pos = entity.blockPosition()
        val blockState = level.getBlockState(pos)
        if (blockState.block is AbstractCauldronBlock || blockState.block is HopperBlock) {
            return true
        }
        val belowPos = pos.below()
        val belowState = level.getBlockState(belowPos)
        if ((belowState.block is AbstractCauldronBlock || belowState.block is HopperBlock) && entity.y <= belowPos.y + 1.0) {
            return true
        }
        return false
    }

    @JvmStatic
    fun isInHollowBlockAt(
        level: Level,
        x: Double,
        y: Double,
        z: Double,
    ): Boolean {
        val pos = BlockPos.containing(x, y, z)
        val state = level.getBlockState(pos)
        if (state.block is AbstractCauldronBlock || state.block is HopperBlock) {
            return true
        }
        val belowPos = pos.below()
        val belowState = level.getBlockState(belowPos)
        if ((belowState.block is AbstractCauldronBlock || belowState.block is HopperBlock) && y <= belowPos.y + 1.0) {
            return true
        }
        return false
    }
}
