package com.fome.exservermod.client

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.Identifier
import java.util.Locale

object BossCustomTextures {
    val WITCH_QUEEN: Identifier = Identifier.fromNamespaceAndPath("exservermod", "textures/entity/witch_queen.png")
    val INCINERATOR: Identifier = Identifier.fromNamespaceAndPath("exservermod", "textures/entity/incinerator.png")
    val DREAD_CHAMPION: Identifier = Identifier.fromNamespaceAndPath("exservermod", "textures/entity/dread_champion.png")
    val WARDEN_BOSS: Identifier = Identifier.fromNamespaceAndPath("exservermod", "textures/entity/warden_boss.png")

    fun getBossTextureFromComponent(component: Component?): Identifier? {
        if (component == null) return null
        val text = component.string.lowercase(Locale.ROOT)
        val contents = component.contents
        val key = if (contents is TranslatableContents) contents.key.lowercase(Locale.ROOT) else ""

        if (key.contains("witch") || text.contains("witch") || text.contains("ведьм") || text.contains("шабаш")) {
            return WITCH_QUEEN
        }
        if (key.contains("blaze") ||
            text.contains("incinerator") ||
            text.contains("испепелитель") ||
            text.contains("пламен") ||
            text.contains("blaze")
        ) {
            return INCINERATOR
        }
        if (key.contains("dread") ||
            text.contains("dread") ||
            text.contains("погибел") ||
            text.contains("преисподн") ||
            text.contains("рыцарь")
        ) {
            return DREAD_CHAMPION
        }
        if (key.contains("warden") ||
            text.contains("warden") ||
            text.contains("варден") ||
            text.contains("хранител") ||
            text.contains("страж")
        ) {
            return WARDEN_BOSS
        }
        return null
    }

    fun getBossTexture(name: String): Identifier? {
        val lower = name.lowercase(Locale.ROOT)
        if (lower.contains("witch") || lower.contains("ведьм") || lower.contains("шабаш")) {
            return WITCH_QUEEN
        }
        if (lower.contains("incinerator") || lower.contains("испепелитель") || lower.contains("пламен") || lower.contains("blaze")) {
            return INCINERATOR
        }
        if (lower.contains("dread") || lower.contains("погибел") || lower.contains("преисподн") || lower.contains("рыцарь")) {
            return DREAD_CHAMPION
        }
        if (lower.contains("warden") || lower.contains("варден") || lower.contains("хранител") || lower.contains("страж")) {
            return WARDEN_BOSS
        }
        return null
    }
}
