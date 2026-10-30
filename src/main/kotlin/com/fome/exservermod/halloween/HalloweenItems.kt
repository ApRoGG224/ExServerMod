package com.fome.exservermod.halloween

import com.fome.exservermod.halloween.item.CandyPouchItem
import com.fome.exservermod.halloween.item.PumpkinBowItem
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

object HalloweenItems {
    val CANDY_POUCH_TIER1_ID: Identifier = Identifier.parse("exservermod:candy_pouch_tier1")
    val CANDY_POUCH_TIER2_ID: Identifier = Identifier.parse("exservermod:candy_pouch_tier2")
    val CANDY_POUCH_TIER3_ID: Identifier = Identifier.parse("exservermod:candy_pouch_tier3")
    val PUMPKIN_BOW_ID: Identifier = Identifier.parse("exservermod:pumpkin_bow")

    val CANDY_POUCH_TIER1: CandyPouchItem =
        CandyPouchItem(
            tier = 1,
            channelDurationTicks = CandyPouchItem.TIER_1_TICKS,
            candyReward = CandyPouchItem.TIER_1_YIELD,
            properties = Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, CANDY_POUCH_TIER1_ID)),
        )

    val CANDY_POUCH_TIER2: CandyPouchItem =
        CandyPouchItem(
            tier = 2,
            channelDurationTicks = CandyPouchItem.TIER_2_TICKS,
            candyReward = CandyPouchItem.TIER_2_YIELD,
            properties = Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, CANDY_POUCH_TIER2_ID)),
        )

    val CANDY_POUCH_TIER3: CandyPouchItem =
        CandyPouchItem(
            tier = 3,
            channelDurationTicks = CandyPouchItem.TIER_3_TICKS,
            candyReward = CandyPouchItem.TIER_3_YIELD,
            properties = Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, CANDY_POUCH_TIER3_ID)),
        )

    val PUMPKIN_BOW: PumpkinBowItem =
        PumpkinBowItem(
            properties = Item.Properties().durability(384).setId(ResourceKey.create(Registries.ITEM, PUMPKIN_BOW_ID)),
        )

    fun register() {
        Registry.register(BuiltInRegistries.ITEM, CANDY_POUCH_TIER1_ID, CANDY_POUCH_TIER1)
        Registry.register(BuiltInRegistries.ITEM, CANDY_POUCH_TIER2_ID, CANDY_POUCH_TIER2)
        Registry.register(BuiltInRegistries.ITEM, CANDY_POUCH_TIER3_ID, CANDY_POUCH_TIER3)
        Registry.register(BuiltInRegistries.ITEM, PUMPKIN_BOW_ID, PUMPKIN_BOW)

        registerCreativeTabs()
    }

    private fun registerCreativeTabs() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register { entries ->
            entries.accept(ItemStack(CANDY_POUCH_TIER1))
            entries.accept(ItemStack(CANDY_POUCH_TIER2))
            entries.accept(ItemStack(CANDY_POUCH_TIER3))
            entries.accept(ItemStack(PUMPKIN_BOW))
        }
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register { entries ->
            entries.accept(ItemStack(PUMPKIN_BOW))
        }
    }
}
