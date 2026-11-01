package com.apocollis.aqtweaks.spawning.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ModifierGearTableTest {
    private static final Map<String, List<String>> NO_CATEGORIES = Map.of();

    private static ModifierTable modifiers(String rules) {
        RuleFileLoader.Result result = RuleFileLoader.load(RuleKind.MODIFIERS, rules, NO_CATEGORIES.keySet());
        assertTrue(result.ok(), result.problems.toString());
        return ModifierTable.compile(result.rows, NO_CATEGORIES);
    }

    private static GearTable gear(String rules) {
        RuleFileLoader.Result result = RuleFileLoader.load(RuleKind.GEAR, rules, NO_CATEGORIES.keySet());
        assertTrue(result.ok(), result.problems.toString());
        return GearTable.compile(result.rows, NO_CATEGORIES);
    }

    @Test
    void eachStatComesFromTheFirstRowThatSetsItAndLaterRowsFillTheRest() {
        ModifierTable table = modifiers("[\n"
                + "{ \"mob\": \"minecraft:zombie\", \"healthmultiply\": 2.0 },\n"
                + "{ \"dimension\": 0, \"healthmultiply\": 5.0, \"damagemultiply\": 1.5, \"armoradd\": 4 }\n]");
        FakeQuery q = new FakeQuery();
        ModifierPlan plan = table.decide(q);
        assertEquals(20.0, plan.expected("health", 10.0), 1e-9, "the first row sets health; the second cannot change it");
        assertEquals(15.0, plan.expected("damage", 10.0), 1e-9);
        assertEquals(4.0, plan.expected("armor", 0.0), 1e-9);
        assertEquals(10.0, plan.expected("speed", 10.0), 1e-9, "a stat no row sets is left alone");
        assertEquals(List.of(0, 1), plan.rows());
    }

    @Test
    void aStopRowEndsTheSearchAfterItApplies() {
        ModifierTable table = modifiers("[\n"
                + "{ \"mob\": \"minecraft:zombie\", \"healthmultiply\": 2.0, \"stop\": true },\n"
                + "{ \"dimension\": 0, \"damagemultiply\": 3.0, \"potion\": \"minecraft:speed,100,1\" }\n]");
        FakeQuery q = new FakeQuery();
        ModifierPlan plan = table.decide(q);
        assertEquals(List.of(0), plan.rows());
        assertEquals(10.0, plan.expected("damage", 10.0), 1e-9);
        assertTrue(plan.potions().isEmpty());
        q.mob = "minecraft:creeper";
        assertEquals(30.0, table.decide(q).expected("damage", 10.0), 1e-9);
    }

    @Test
    void potionsAddUpAcrossRowsAndAngryAppliesIfAnyRowHasIt() {
        ModifierTable table = modifiers("[\n"
                + "{ \"dimension\": 0, \"potion\": \"minecraft:speed,100,1\" },\n"
                + "{ \"hostile\": true, \"potion\": [\"minecraft:speed,100,1\", \"minecraft:strength,50,0\"], \"angry\": true }\n]");
        ModifierPlan plan = table.decide(new FakeQuery());
        assertEquals(List.of("minecraft:speed,100,1", "minecraft:strength,50,0"), plan.potions());
        assertTrue(plan.angry());
        assertFalse(plan.isEmpty());
    }

    @Test
    void noMatchGivesAnEmptyPlan() {
        ModifierTable table = modifiers("[{ \"mob\": \"minecraft:ghast\", \"healthmultiply\": 2.0 }]");
        ModifierPlan plan = table.decide(new FakeQuery());
        assertTrue(plan.isEmpty());
        assertTrue(plan.rows().isEmpty());
        assertFalse(plan.usedRandom());
    }

    @Test
    void aRandomModifierRowIsFlaggedOnlyWhereItsRollCouldDecide() {
        ModifierTable table = modifiers("[{ \"mob\": \"minecraft:creeper\", \"dimension\": 0, \"random\": 0.25, \"healthmultiply\": 2.0, \"stop\": true }]");
        FakeQuery q = new FakeQuery();
        q.mob = "minecraft:creeper";
        q.roll = 0.1f;
        assertTrue(table.decide(q).usedRandom());
        q.roll = 0.9f;
        ModifierPlan miss = table.decide(q);
        assertTrue(miss.isEmpty());
        assertTrue(miss.usedRandom());
        q.mob = "minecraft:zombie";
        assertFalse(table.decide(q).usedRandom(), "a zombie never reaches the roll");
        q.mob = "minecraft:creeper";
        q.dimension = -1;
        assertFalse(table.decide(q).usedRandom(), "nor does a creeper in the Nether");
    }

    @Test
    void theFirstMatchingGearRowSuppliesTheWholeLoadout() {
        GearTable table = gear("[\n"
                + "{ \"mob\": \"minecraft:zombie\", \"maxheight\": 15, \"hand\": \"minecraft:diamond_sword\", \"head\": \"minecraft:diamond_helmet\" },\n"
                + "{ \"mob\": \"minecraft:zombie\", \"hand\": \"minecraft:iron_sword\" }\n]");
        FakeQuery q = new FakeQuery();
        q.y = 10;
        GearTable.Loadout deep = table.decide(q);
        assertEquals(0, deep.row());
        assertEquals("minecraft:diamond_helmet", deep.slots().get("head"));
        q.y = 40;
        GearTable.Loadout shallow = table.decide(q);
        assertEquals(1, shallow.row());
        assertEquals(Map.of("hand", "minecraft:iron_sword"), shallow.slots(), "rows never merge slot by slot");
        q.mob = "minecraft:creeper";
        assertFalse(table.decide(q).matched());
    }

    @Test
    void aRandomGearRowAboveThePlainRowGivesTheAlternativeSet() {
        GearTable table = gear("[\n"
                + "{ \"mob\": \"minecraft:zombie\", \"random\": 0.3, \"hand\": \"minecraft:bow\" },\n"
                + "{ \"mob\": \"minecraft:zombie\", \"hand\": \"minecraft:iron_sword\" }\n]");
        FakeQuery q = new FakeQuery();
        q.roll = 0.1f;
        GearTable.Loadout lucky = table.decide(q);
        assertEquals("minecraft:bow", lucky.slots().get("hand"));
        assertTrue(lucky.usedRandom());
        q.roll = 0.9f;
        GearTable.Loadout plain = table.decide(q);
        assertEquals("minecraft:iron_sword", plain.slots().get("hand"));
        assertTrue(plain.usedRandom(), "a missed roll on a row that otherwise applies is still random-dependent");
    }
}
