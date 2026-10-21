package com.fome.exservermod.sound

import com.fome.exservermod.ExServerMod
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

object ModSounds {
    val ROULETTE_SPIN_ID: Identifier = Identifier.fromNamespaceAndPath(ExServerMod.MOD_ID, "roulette_spin")
    val ROULETTE_SPIN: SoundEvent = SoundEvent.createVariableRangeEvent(ROULETTE_SPIN_ID)

    fun register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, ROULETTE_SPIN_ID, ROULETTE_SPIN)
    }
}
