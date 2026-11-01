package com.apocollis.aqtweaks.spawning.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apocollis.aqtweaks.spawning.table.LenientJson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Runs the pack's original {@code spawn.json} the way In Control gives stats, potions and gear at join (the first
 * matching {@code onjoin} rule, whatever it holds) beside the converted modifiers and gear tables, over generated
 * situations. Where In Control's first match carries actions, the tables must give exactly those. Where it carries
 * none (an earlier condition-only rule ended the walk), In Control gives nothing and the tables may give something:
 * that is the documented difference, and it is counted so a change in it shows up.
 */
class ActionDifferentialTest {
    private static final Map<String, String> GEAR_KEYS = Map.of("helditem", "hand", "armorhelmet", "head",
            "armorchest", "chest", "armorlegs", "legs", "armorboots", "feet");

    private static boolean hasModifierActions(JsonObject rule) {
        for (String stat : ModifierPlan.STATS) {
            if (rule.has(stat + "multiply") || rule.has(stat + "add")) {
                return true;
            }
        }
        return rule.has("potion") || rule.has("angry");
    }

    private static boolean hasGearActions(JsonObject rule) {
        return GEAR_KEYS.keySet().stream().anyMatch(rule::has);
    }

    /** Stats, potions and angry as one comparable string; base values are 10 and 3 so multiply and add both show. */
    private static String describe(ModifierPlan plan) {
        StringBuilder text = new StringBuilder();
        for (String stat : ModifierPlan.STATS) {
            if (plan.stats().contains(stat)) {
                text.append(stat).append('=').append(plan.expected(stat, 10.0)).append(',').append(plan.expected(stat, 3.0)).append(';');
            }
        }
        text.append("potions=").append(plan.potions()).append(";angry=").append(plan.angry());
        return text.toString();
    }

    private static String describe(JsonObject rule) {
        StringBuilder text = new StringBuilder();
        for (String stat : ModifierPlan.STATS) {
            if (rule.has(stat + "multiply") || rule.has(stat + "add")) {
                double m = rule.has(stat + "multiply") ? rule.get(stat + "multiply").getAsDouble() : 1.0;
                double a = rule.has(stat + "add") ? rule.get(stat + "add").getAsDouble() : 0.0;
                text.append(stat).append('=').append(10.0 * m + a).append(',').append(3.0 * m + a).append(';');
            }
        }
        List<String> potions = new ArrayList<String>();
        if (rule.has("potion")) {
            JsonElement value = rule.get("potion");
            if (value.isJsonArray()) {
                value.getAsJsonArray().forEach(p -> potions.add(p.getAsString()));
            } else {
                potions.add(value.getAsString());
            }
            potions.remove("none");
        }
        text.append("potions=").append(potions).append(";angry=")
                .append(rule.has("angry") && rule.get("angry").getAsBoolean());
        return text.toString();
    }

    private static Map<String, String> gearOf(JsonObject rule) {
        Map<String, String> slots = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> key : GEAR_KEYS.entrySet()) {
            if (rule.has(key.getKey())) {
                slots.put(key.getValue(), rule.get(key.getKey()).getAsString());
            }
        }
        return slots;
    }

    private static void compare(float roll, long seed) throws IOException {
        String text = ConditionDifferentialTest.resource("spawn.json");
        List<RuleProblem> problems = new ArrayList<RuleProblem>();
        List<JsonObject> originals = new ArrayList<JsonObject>();
        for (RawRule raw : RuleScanner.scan(text, "spawn.json", problems)) {
            originals.add(LenientJson.parse(raw.text).getAsJsonObject());
        }
        assertEquals(List.of(), problems);
        List<ConditionRow> matchers = new ArrayList<ConditionRow>();
        for (JsonObject rule : originals) {
            JsonObject copy = LenientJson.parse(rule.toString()).getAsJsonObject();
            copy.remove("result");
            matchers.add(ConditionRow.matcher(copy, matchers.size(), new HashMap<String, List<String>>()));
        }

        SpawnJsonConverter.Converted converted = SpawnJsonConverter.convert(text, new ArrayList<RuleProblem>());
        RuleFileLoader.Result modifierFile = RuleFileLoader.load(RuleKind.MODIFIERS, converted.modifiersText(), List.of());
        assertEquals(List.of(), modifierFile.problems);
        RuleFileLoader.Result gearFile = RuleFileLoader.load(RuleKind.GEAR, converted.gearText(), List.of());
        assertEquals(List.of(), gearFile.problems);
        ModifierTable modifiers = ModifierTable.compile(modifierFile.rows, new HashMap<String, List<String>>());
        GearTable gear = GearTable.compile(gearFile.rows, new HashMap<String, List<String>>());

        Set<String> mobSet = new LinkedHashSet<String>(List.of("minecraft:zombie", "minecraft:skeleton",
                "minecraft:creeper", "lycanitesmobs:unlisted"));
        for (JsonObject rule : originals) {
            if (rule.has("mob")) {
                if (rule.get("mob").isJsonArray()) {
                    rule.get("mob").getAsJsonArray().forEach(m -> mobSet.add(m.getAsString()));
                } else {
                    mobSet.add(rule.get("mob").getAsString());
                }
            }
        }
        List<String> mobs = new ArrayList<String>(mobSet);

        Random rnd = new Random(seed);
        Map<String, Integer> kinds = new LinkedHashMap<String, Integer>();
        List<String> examples = new ArrayList<String>();
        int compared = 0;
        int giveNothingByDesign = 0;
        int runs = 300000;
        for (int n = 0; n < runs; n++) {
            FakeQuery q = rnd.nextBoolean() ? ConditionDifferentialTest.scenario(rnd, mobs, false, roll)
                    : ConditionDifferentialTest.directed(rnd, originals.get(rnd.nextInt(originals.size())), false, roll);
            // In Control at join: the first matching onjoin rule, whatever it holds.
            JsonObject first = null;
            for (int i = 0; i < originals.size(); i++) {
                JsonObject rule = originals.get(i);
                if (rule.has("onjoin") && rule.get("onjoin").getAsBoolean() && matchers.get(i).matches(q)) {
                    first = rule;
                    break;
                }
            }
            // A rule without onjoin acts at the spawn check instead; it is not part of this comparison.
            ModifierPlan plan = modifiers.decide(q);
            GearTable.Loadout loadout = gear.decide(q);
            boolean modifiersToCompare = first != null && hasModifierActions(first);
            boolean gearToCompare = first != null && hasGearActions(first);
            if (!modifiersToCompare && !plan.isEmpty() || !gearToCompare && !loadout.slots().isEmpty()) {
                giveNothingByDesign++;
            }
            if (modifiersToCompare) {
                compared++;
                String theirs = describe(first);
                String ours = describe(plan);
                if (!theirs.equals(ours)) {
                    String kind = "modifiers differ for first rule " + first.get("mob");
                    kinds.merge(kind, 1, Integer::sum);
                    if (examples.size() < 12) {
                        examples.add(kind + "\n   In Control " + theirs + "\n   Tweaks     " + ours + "\n   " + q);
                    }
                }
            }
            if (gearToCompare) {
                compared++;
                Map<String, String> theirGear = gearOf(first);
                if (!theirGear.equals(loadout.slots())) {
                    String kind = "gear differs for first rule " + first.get("mob");
                    kinds.merge(kind, 1, Integer::sum);
                    if (examples.size() < 12) {
                        examples.add(kind + "\n   In Control " + theirGear + "\n   Tweaks     " + loadout.slots() + "\n   " + q);
                    }
                }
            }
        }
        System.out.println("roll " + roll + ": compared " + compared + " modifier or gear sets where In Control's first match gives them; "
                + giveNothingByDesign + " joins where In Control gives nothing of a kind and the tables do");
        assertTrue(compared > 1000, "too few comparisons: " + compared);
        assertTrue(kinds.isEmpty(), "mismatches (roll " + roll + "): " + kinds + "\n" + String.join("\n", examples));
    }

    @Test
    void convertedModifiersAndGearGiveWhatInControlGivesWhenRandomRowsPass() throws IOException {
        compare(0.0f, 7L);
    }

    @Test
    void convertedModifiersAndGearGiveWhatInControlGivesWhenRandomRowsFail() throws IOException {
        compare(0.999f, 11L);
    }
}
