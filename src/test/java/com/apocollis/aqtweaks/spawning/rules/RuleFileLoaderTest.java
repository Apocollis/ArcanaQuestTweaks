package com.apocollis.aqtweaks.spawning.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuleFileLoaderTest {
    private static final List<String> CATEGORIES = List.of("spooky", "desert", "swamp");

    private static RuleFileLoader.Result conditions(String text) {
        return RuleFileLoader.load(RuleKind.CONDITIONS, text, CATEGORIES);
    }

    private static String only(RuleFileLoader.Result result) {
        assertEquals(1, result.problems.size(), result.problems.toString());
        return result.problems.get(0).toString();
    }

    @Test
    void goodRowsLoadWithTheirLinesAndComments() {
        String text = "[\n"
                + "  // first\n"
                + "  // second line of comment\n"
                + "  { \"mob\": \"a:b\", \"result\": \"deny\" },\n"
                + "\n"
                + "  { \"dimension\": [0, -1], \"time\": \"day\", \"result\": \"default\", \"phase\": \"spawn\", },\n"
                + "]\n";
        RuleFileLoader.Result result = conditions(text);
        assertTrue(result.ok(), result.problems.toString());
        assertEquals(2, result.rows.size());
        assertEquals(4, result.rows.get(0).line);
        assertEquals(List.of("first", "second line of comment"), result.rows.get(0).comments);
        assertEquals(6, result.rows.get(1).line);
        assertEquals(List.of(), result.rows.get(1).comments);
    }

    @Test
    void aProblemNamesTheFileAndTheLineOfTheRule() {
        String text = "[\n{ \"mob\": \"a:b\", \"result\": \"deny\" },\n\n{ \"mob\": \"a:b\", \"result\": \"maybe\" }\n]";
        RuleFileLoader.Result result = conditions(text);
        assertEquals(1, result.rows.size());
        String problem = only(result);
        assertTrue(problem.startsWith("spawn_conditions.json:4: "), problem);
        assertTrue(problem.contains("'result'") && problem.contains("allow"), problem);
    }

    @Test
    void aMisspelledKeyGetsASuggestion() {
        String problem = only(conditions("[{ \"dimenson\": 0, \"result\": \"deny\" }]"));
        assertTrue(problem.contains("unknown key 'dimenson'") && problem.contains("did you mean 'dimension'"), problem);
    }

    @Test
    void anUnknownBiomeCategoryGetsASuggestion() {
        String problem = only(conditions("[{ \"biomecat\": \"spoky\", \"result\": \"allow\" }]"));
        assertTrue(problem.contains("no biome category 'spoky'") && problem.contains("did you mean 'spooky'"), problem);
        assertTrue(conditions("[{ \"biomecat\": [\"spooky\", \"swamp\"], \"result\": \"allow\" }]").ok());
    }

    @Test
    void missingResultAndEmptyRowsAreReported() {
        assertTrue(only(conditions("[{ \"mob\": \"a:b\" }]")).contains("missing 'result'"));
        RuleFileLoader.Result modifiers = RuleFileLoader.load(RuleKind.MODIFIERS, "[{ \"mob\": \"a:b\" }]", CATEGORIES);
        assertTrue(only(modifiers).contains("changes nothing"));
        RuleFileLoader.Result gear = RuleFileLoader.load(RuleKind.GEAR, "[{ \"mob\": \"a:b\" }]", CATEGORIES);
        assertTrue(only(gear).contains("equips nothing"));
        RuleFileLoader.Result noMob = RuleFileLoader.load(RuleKind.GEAR, "[{ \"hand\": \"a:b\" }]", CATEGORIES);
        assertTrue(only(noMob).contains("missing 'mob'"));
    }

    @Test
    void timeWindowsAreChecked() {
        assertTrue(conditions("[{ \"time\": \"night\", \"result\": \"deny\" }]").ok());
        assertTrue(conditions("[{ \"time\": [[0, 6000], [18000, 24000]], \"result\": \"deny\" }]").ok());
        assertTrue(only(conditions("[{ \"time\": \"noon\", \"result\": \"deny\" }]")).contains("'time'"));
        assertTrue(only(conditions("[{ \"time\": [[5000, 1000]], \"result\": \"deny\" }]")).contains("0 <= from <= to"));
        assertTrue(only(conditions("[{ \"time\": \"day\", \"maxtime\": 13000, \"result\": \"deny\" }]"))
                .contains("not both"));
    }

    @Test
    void heightsAndChancesAreChecked() {
        assertTrue(only(conditions("[{ \"minheight\": 70, \"maxheight\": 60, \"result\": \"deny\" }]"))
                .contains("can never match"));
        assertTrue(only(conditions("[{ \"random\": 1.5, \"result\": \"deny\" }]")).contains("'random'"));
        assertTrue(conditions("[{ \"random\": \"0.25\", \"result\": \"deny\" }]").ok());
    }

    @Test
    void countsFollowInControlsForms() {
        assertTrue(conditions("[{ \"mincount\": 100, \"result\": \"deny\" }]").ok());
        assertTrue(conditions("[{ \"maxcount\": \"5,a:b\", \"result\": \"deny\" }]").ok());
        assertTrue(conditions("[{ \"mincount\": { \"amount\": 20, \"perplayer\": true, \"mob\": [\"a:b\", \"c:d\"] },"
                + " \"result\": \"deny\" }]").ok());
        assertTrue(only(conditions("[{ \"mincount\": { \"perplayer\": true }, \"result\": \"deny\" }]"))
                .contains("amount"));
        assertTrue(only(conditions("[{ \"mincount\": { \"amount\": 1, \"perplayer\": true, \"perchunk\": true },"
                + " \"result\": \"deny\" }]")).contains("cannot be combined"));
    }

    @Test
    void potionsAndIdsAreChecked() {
        RuleFileLoader.Result good = RuleFileLoader.load(RuleKind.MODIFIERS,
                "[{ \"mob\": \"a:b\", \"potion\": [\"minecraft:night_vision,999999,0\"], \"healthmultiply\": 2.0,"
                        + " \"stop\": true }]", CATEGORIES);
        assertTrue(good.ok(), good.problems.toString());
        RuleFileLoader.Result bad = RuleFileLoader.load(RuleKind.MODIFIERS,
                "[{ \"mob\": \"NoNamespace\", \"potion\": \"night_vision\" }]", CATEGORIES);
        assertEquals(2, bad.problems.size(), bad.problems.toString());
    }

    @Test
    void generateRowsStandAloneAndMatchTheirFile() {
        assertTrue(conditions("[{ \"generate\": \"stage_gating\" }]").ok());
        assertTrue(only(conditions("[{ \"generate\": \"stage_gating\", \"mob\": \"a:b\" }]")).contains("stands alone"));
        assertTrue(only(conditions("[{ \"generate\": \"dimension_buffs\" }]")).contains("'generate'"));
        assertTrue(RuleFileLoader.load(RuleKind.MODIFIERS, "[{ \"generate\": \"dimension_buffs\" }]", CATEGORIES).ok());
    }

    @Test
    void brokenFilesAreReportedNotThrown() {
        String missingComma = "[\n{ \"mob\": \"a:b\" \"result\": \"deny\" }\n]";
        assertTrue(only(conditions(missingComma)).contains("spawn_conditions.json:2: malformed rule"));
        assertTrue(only(conditions("[\n{ \"mob\": \"a:b\", \"result\": \"deny\"\n")).contains("never closed"));
        assertTrue(only(conditions("{ \"mob\": \"a:b\" }")).contains("start it with ["));
        assertTrue(conditions("[]").ok());
        assertTrue(conditions("[ /* nothing yet */ ]").ok());
        assertFalse(conditions("[ /* unclosed ]").ok());
    }

    @Test
    void commentsAndStringsWithBracesDoNotConfuseTheScanner() {
        String text = "[\n{ \"mob\": \"a:b\", \"result\": \"deny\" }, // trailing } comment {\n"
                + "{ \"structure\": \"Vil{}lage\", \"result\": \"deny\" }\n]";
        RuleFileLoader.Result result = conditions(text);
        assertTrue(result.ok(), result.problems.toString());
        assertEquals(2, result.rows.size());
        assertEquals(3, result.rows.get(1).line);
    }
}
