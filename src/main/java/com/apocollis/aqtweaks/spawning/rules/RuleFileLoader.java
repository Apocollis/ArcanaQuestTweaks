package com.apocollis.aqtweaks.spawning.rules;

import com.apocollis.aqtweaks.spawning.table.LenientJson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Reads one rule file: finds each rule, parses it, checks it, and reports every problem with its line. */
public final class RuleFileLoader {
    /** The rules that passed and the problems found; the file is usable only when there are no problems. */
    public static final class Result {
        public final List<RuleRow> rows;
        public final List<RuleProblem> problems;

        Result(List<RuleRow> rows, List<RuleProblem> problems) {
            this.rows = Collections.unmodifiableList(rows);
            this.problems = Collections.unmodifiableList(problems);
        }

        public boolean ok() {
            return problems.isEmpty();
        }
    }

    private RuleFileLoader() {
    }

    /**
     * @param text       the file's text
     * @param categories the biome category names of biome_mappings.json
     */
    public static Result load(RuleKind kind, String text, Collection<String> categories) {
        List<RuleProblem> problems = new ArrayList<RuleProblem>();
        List<RuleRow> rows = new ArrayList<RuleRow>();
        RuleSchema schema = new RuleSchema(categories);
        String file = kind.fileName;
        for (RawRule raw : RuleScanner.scan(text, file, problems)) {
            JsonObject json;
            try {
                JsonElement parsed = LenientJson.parse(raw.text);
                if (!parsed.isJsonObject()) {
                    problems.add(new RuleProblem(file, raw.line, "not a rule"));
                    continue;
                }
                json = parsed.getAsJsonObject();
            } catch (RuntimeException malformed) {
                problems.add(new RuleProblem(file, raw.line, "malformed rule: " + rootMessage(malformed)));
                continue;
            }
            int before = problems.size();
            schema.validate(kind, json, file, raw.line, problems);
            if (problems.size() == before) {
                rows.add(new RuleRow(kind, raw.line, raw.comments, json));
            }
        }
        return new Result(rows, problems);
    }

    private static String rootMessage(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return message == null ? cause.getClass().getSimpleName() : message;
    }
}
