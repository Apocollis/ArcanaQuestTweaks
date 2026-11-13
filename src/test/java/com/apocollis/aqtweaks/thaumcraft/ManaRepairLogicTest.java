package com.apocollis.aqtweaks.thaumcraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.apocollis.aqtweaks.thaumcraft.ManaRepairLogic.Kind;

class ManaRepairLogicTest {
    @Test
    void botaniaPricesAreTheDefaults() {
        assertEquals(120, ManaRepairLogic.repairCost(false, 1.0));
        assertEquals(140, ManaRepairLogic.repairCost(true, 1.0));
        assertEquals(60, ManaRepairLogic.damageCost(1, false, 1.0));
        assertEquals(70, ManaRepairLogic.damageCost(1, true, 1.0));
        assertEquals(180, ManaRepairLogic.damageCost(3, false, 1.0));
    }

    @Test
    void multiplierScalesAndZeroIsFree() {
        assertEquals(60, ManaRepairLogic.repairCost(false, 0.5));
        assertEquals(240, ManaRepairLogic.repairCost(false, 2.0));
        assertEquals(0, ManaRepairLogic.repairCost(false, 0.0));
        assertEquals(0, ManaRepairLogic.damageCost(5, true, 0.0));
        assertEquals(1, ManaRepairLogic.scaled(1, 0.1));
        assertEquals(0, ManaRepairLogic.damageCost(0, false, 1.0));
        assertEquals(Integer.MAX_VALUE, ManaRepairLogic.damageCost(Integer.MAX_VALUE, true, 100.0));
    }

    @Test
    void tabletMustBeNormalAndFull() {
        assertTrue(ManaRepairLogic.tabletQualifies(500_000, false));
        assertTrue(ManaRepairLogic.tabletQualifies(900_000, false));
        assertFalse(ManaRepairLogic.tabletQualifies(499_999, false));
        assertFalse(ManaRepairLogic.tabletQualifies(500_000, true));
        assertFalse(ManaRepairLogic.tabletQualifies(0, false));
    }

    @Test
    void vanillaStyleItemsMapByKind() {
        assertEquals(List.of("botania:manasteelchest"), ManaRepairLogic.cousins("minecraft:iron_chestplate", Kind.CHEST));
        assertEquals(List.of("botania:manasteellegs"), ManaRepairLogic.cousins("minecraft:diamond_leggings", Kind.LEGS));
        assertEquals(List.of("botania:manasteelboots"), ManaRepairLogic.cousins("x:boots", Kind.BOOTS));
        assertEquals(List.of("botania:manasteelhelm", "botania:manasteelhelmreveal"),
                ManaRepairLogic.cousins("minecraft:iron_helmet", Kind.HELMET));
        assertEquals(List.of("botania:manasteelpick"), ManaRepairLogic.cousins("minecraft:diamond_pickaxe", Kind.PICKAXE));
        assertEquals(List.of("botania:manasteelaxe"), ManaRepairLogic.cousins("minecraft:iron_axe", Kind.AXE));
        assertEquals(List.of("botania:manasteelshovel"), ManaRepairLogic.cousins("minecraft:iron_shovel", Kind.SHOVEL));
        assertEquals(List.of("botania:manasteelsword"), ManaRepairLogic.cousins("minecraft:iron_sword", Kind.SWORD));
        assertEquals(List.of("botania:manasteelshears"), ManaRepairLogic.cousins("minecraft:shears", Kind.SHEARS));
    }

    @Test
    void itemsWithoutACousinUseTheBlock() {
        assertTrue(ManaRepairLogic.cousins("minecraft:bow", Kind.OTHER).isEmpty());
        assertTrue(ManaRepairLogic.cousins("minecraft:shield", Kind.OTHER).isEmpty());
        assertTrue(ManaRepairLogic.cousins("minecraft:iron_hoe", Kind.OTHER).isEmpty());
        assertTrue(ManaRepairLogic.cousins("minecraft:elytra", Kind.OTHER).isEmpty());
        assertTrue(ManaRepairLogic.cousins(null, null).isEmpty());
    }

    @Test
    void spartanItemsMapByName() {
        assertEquals(List.of("spartanshields:shield_botania_manasteel"),
                ManaRepairLogic.cousins("spartanshields:shield_basic_iron", Kind.OTHER));
        assertEquals(List.of("spartanweaponryarcana:longbow_manasteel"),
                ManaRepairLogic.cousins("spartanweaponry:longbow_iron", Kind.OTHER));
        assertEquals(List.of("spartanweaponryarcana:crossbow_manasteel"),
                ManaRepairLogic.cousins("spartanweaponry:crossbow_diamond", Kind.OTHER));
        assertEquals(List.of("spartanweaponryarcana:throwing_knife_manasteel"),
                ManaRepairLogic.cousins("spartanweaponry:throwing_knife_iron", Kind.OTHER));
        assertEquals(List.of("spartanweaponryarcana:parrying_dagger_manasteel"),
                ManaRepairLogic.cousins("spartanweaponry:parrying_dagger_gold", Kind.OTHER));
        assertEquals(List.of("spartanweaponryarcana:dagger_manasteel"),
                ManaRepairLogic.cousins("spartanweaponry:dagger_iron", Kind.OTHER));
        assertEquals(List.of("spartanweaponryarcana:warhammer_manasteel"),
                ManaRepairLogic.cousins("spartanweaponry:warhammer_steel", Kind.OTHER));
        // a Spartan type with no Manasteel variant falls back to the block
        assertTrue(ManaRepairLogic.cousins("spartanweaponry:unknowntype_iron", Kind.OTHER).isEmpty());
    }

    @Test
    void manasteelItemsAreRecognised() {
        for (String name : ManaRepairLogic.allKnownCousinNames()) {
            assertTrue(ManaRepairLogic.isManasteelItem(name), name);
        }
        assertEquals(10 + 1 + 23, ManaRepairLogic.allKnownCousinNames().size());
        assertTrue(ManaRepairLogic.isManasteelItem("botania:manasteelhelmreveal"));
        assertFalse(ManaRepairLogic.isManasteelItem("botania:terrasteelpick"));
        assertFalse(ManaRepairLogic.isManasteelItem("spartanweaponryarcana:longbow_terrasteel"));
        assertFalse(ManaRepairLogic.isManasteelItem("spartanweaponryarcana:nonsense_manasteel"));
        assertFalse(ManaRepairLogic.isManasteelItem("minecraft:iron_pickaxe"));
        assertFalse(ManaRepairLogic.isManasteelItem(null));
    }
}
