package com.apocollis.aqtweaks.spawning.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConditionTableTest {
    private static final Map<String, List<String>> CATEGORIES = Map.of(
            "spooky", List.of("biomesoplenty:ominous_woods", "minecraft:roofed_forest"));

    private static ConditionTable table(String rules) {
        RuleFileLoader.Result result = RuleFileLoader.load(RuleKind.CONDITIONS, rules, CATEGORIES.keySet());
        assertTrue(result.ok(), result.problems.toString());
        return ConditionTable.compile(result.rows, CATEGORIES);
    }

    @Test
    void theFirstMatchingRowDecidesAndLaterRowsAreNotLookedAt() {
        ConditionTable table = table("[\n"
                + "{ \"mob\": \"minecraft:zombie\", \"dimension\": 0, \"result\": \"default\" },\n"
                + "{ \"dimension\": 0, \"result\": \"deny\" }\n]");
        FakeQuery zombie = new FakeQuery();
        Decision first = table.decide(ConditionTable.Phase.SPAWN, zombie);
        assertEquals(Decision.Result.DEFAULT, first.result);
        assertEquals(0, first.row);
        zombie.mob = "minecraft:creeper";
        assertEquals(Decision.Result.DENY, table.decide(ConditionTable.Phase.SPAWN, zombie).result);
        zombie.dimension = -1;
        assertFalse(table.decide(ConditionTable.Phase.SPAWN, zombie).matched());
    }

    @Test
    void spawnOnlyRowsAreSkippedAtJoin() {
        ConditionTable table = table("[\n"
                + "{ \"dimension\": 0, \"result\": \"deny\", \"phase\": \"spawn\" },\n"
                + "{ \"mob\": \"minecraft:zombie\", \"result\": \"allow\" }\n]");
        FakeQuery zombie = new FakeQuery();
        assertEquals(Decision.Result.DENY, table.decide(ConditionTable.Phase.SPAWN, zombie).result);
        Decision join = table.decide(ConditionTable.Phase.JOIN, zombie);
        assertEquals(Decision.Result.ALLOW, join.result);
        assertEquals(1, join.row);
    }

    @Test
    void aSpawnerRowNeverMatchesAtJoin() {
        ConditionTable table = table("[{ \"spawner\": false, \"result\": \"deny\" }, { \"spawner\": true, \"result\": \"deny\" }]");
        FakeQuery query = new FakeQuery();
        assertEquals(Decision.Result.DENY, table.decide(ConditionTable.Phase.SPAWN, query).result);
        query.spawner = Boolean.TRUE;
        assertEquals(Decision.Result.DENY, table.decide(ConditionTable.Phase.SPAWN, query).result);
        query.spawner = null;
        assertFalse(table.decide(ConditionTable.Phase.JOIN, query).matched());
    }

    @Test
    void heightsTimesAndWindowsAreInclusive() {
        ConditionTable table = table("[\n"
                + "{ \"minheight\": 60, \"maxheight\": 70, \"time\": \"day\", \"result\": \"deny\" }\n]");
        FakeQuery q = new FakeQuery();
        for (int y : new int[] {60, 70}) {
            q.y = y;
            q.time = 13000;
            assertTrue(table.decide(ConditionTable.Phase.SPAWN, q).matched(), "y " + y);
        }
        q.y = 71;
        assertFalse(table.decide(ConditionTable.Phase.SPAWN, q).matched());
        q.y = 64;
        q.time = 23000;
        assertTrue(table.decide(ConditionTable.Phase.SPAWN, q).matched());
        q.time = 18000;
        assertFalse(table.decide(ConditionTable.Phase.SPAWN, q).matched());
        q.time = 24000 + 6000;
        assertTrue(table.decide(ConditionTable.Phase.SPAWN, q).matched(), "time wraps at 24000");
    }

    @Test
    void biomeCategoriesExpandToTheirBiomes() {
        ConditionTable table = table("[{ \"biomecat\": \"spooky\", \"seesky\": true, \"result\": \"default\" }]");
        FakeQuery q = new FakeQuery();
        q.biome = "biomesoplenty:ominous_woods";
        assertTrue(table.decide(ConditionTable.Phase.SPAWN, q).matched());
        q.biome = "minecraft:plains";
        assertFalse(table.decide(ConditionTable.Phase.SPAWN, q).matched());
    }

    @Test
    void countsScalePerPlayerAndMinAndMaxAreOpposites() {
        ConditionTable table = table("[\n"
                + "{ \"mincount\": { \"amount\": 250, \"hostile\": true, \"perplayer\": true }, \"result\": \"deny\" },\n"
                + "{ \"maxcount\": { \"amount\": 5, \"perplayer\": true, \"mob\": \"minecraft:zombie\" }, \"result\": \"allow\" }\n]");
        FakeQuery q = new FakeQuery();
        q.players = 2;
        q.count = 499;
        // 499 is below the cap of 2 x 250, and not below the 2 x 5 limit of the allow row: neither row matches
        assertFalse(table.decide(ConditionTable.Phase.SPAWN, q).matched());
        q.count = 500;
        assertEquals(Decision.Result.DENY, table.decide(ConditionTable.Phase.SPAWN, q).result);
        q.count = 3;
        assertEquals(1, table.decide(ConditionTable.Phase.SPAWN, q).row);
    }

    @Test
    void randomRowsAreFlaggedSoACallerCanSkipComparingThem() {
        ConditionTable table = table("[{ \"mob\": \"minecraft:zombie\", \"random\": 0.25, \"result\": \"deny\" }]");
        FakeQuery q = new FakeQuery();
        q.roll = 0.1f;
        Decision hit = table.decide(ConditionTable.Phase.SPAWN, q);
        assertEquals(Decision.Result.DENY, hit.result);
        assertTrue(hit.usedRandom);
        q.roll = 0.9f;
        Decision miss = table.decide(ConditionTable.Phase.SPAWN, q);
        assertNull(miss.result);
        assertTrue(miss.usedRandom, "a miss on a random row is still random-dependent");
    }

    @Test
    void generateRowsAreSkippedWithANote() {
        ConditionTable table = table("[{ \"generate\": \"stage_gating\" }, { \"mob\": \"a:b\", \"result\": \"deny\" }]");
        assertEquals(1, table.size());
        assertEquals(1, table.notes().size());
        assertTrue(table.notes().get(0).contains("stage_gating"));
    }
}
