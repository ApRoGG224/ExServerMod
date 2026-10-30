package com.fome.exservermod.sound

import com.fome.exservermod.ExServerMod
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

object ModSounds {
    val ROULETTE_SPIN_ID: Identifier = Identifier.fromNamespaceAndPath(ExServerMod.MOD_ID, "roulette_spin")
    val ROULETTE_SPIN: SoundEvent = SoundEvent.createVariableRangeEvent(ROULETTE_SPIN_ID)

    val LULLABY_AMBIENT_ID: Identifier = Identifier.fromNamespaceAndPath(ExServerMod.MOD_ID, "lullaby_ambient")
    val LULLABY_AMBIENT: SoundEvent = SoundEvent.createVariableRangeEvent(LULLABY_AMBIENT_ID)

    val LULLABY_INTRO_30S_ID: Identifier = Identifier.fromNamespaceAndPath(ExServerMod.MOD_ID, "lullaby_intro_30s")
    val LULLABY_INTRO_30S: SoundEvent = SoundEvent.createVariableRangeEvent(LULLABY_INTRO_30S_ID)

    val LULLABY_COMBAT_DROP_ID: Identifier = Identifier.fromNamespaceAndPath(ExServerMod.MOD_ID, "lullaby_combat_drop")
    val LULLABY_COMBAT_DROP: SoundEvent = SoundEvent.createVariableRangeEvent(LULLABY_COMBAT_DROP_ID)

    fun register() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, ROULETTE_SPIN_ID, ROULETTE_SPIN)
        Registry.register(BuiltInRegistries.SOUND_EVENT, LULLABY_AMBIENT_ID, LULLABY_AMBIENT)
        Registry.register(BuiltInRegistries.SOUND_EVENT, LULLABY_INTRO_30S_ID, LULLABY_INTRO_30S)
        Registry.register(BuiltInRegistries.SOUND_EVENT, LULLABY_COMBAT_DROP_ID, LULLABY_COMBAT_DROP)
    }
}
