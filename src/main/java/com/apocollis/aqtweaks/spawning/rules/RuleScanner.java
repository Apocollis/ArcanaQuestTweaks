package com.apocollis.aqtweaks.spawning.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a rule file into its top-level objects while remembering the line each starts on and the {@code //}
 * comment lines directly above it, which a plain JSON parse throws away. Comments, strings and nested braces are
 * skipped correctly; the objects themselves are parsed afterwards, one at a time.
 */
final class RuleScanner {
    private RuleScanner() {
    }

    static List<RawRule> scan(String text, String file, List<RuleProblem> problems) {
        List<RawRule> rules = new ArrayList<RawRule>();
        int line = 1;
        int depth = 0;
        boolean sawArray = false;
        boolean inString = false;
        int start = -1;
        int startLine = 0;
        List<String> comments = new ArrayList<String>();
        List<String> pending = new ArrayList<String>();
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i += 2;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                } else if (c == '\n') {
                    line++;
                }
                i++;
                continue;
            }
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '/') {
                int end = text.indexOf('\n', i);
                if (end < 0) {
                    end = n;
                }
                if (depth == 0) {
                    pending.add(text.substring(i + 2, end).trim());
                }
                i = end;
                continue;
            }
            if (c == '/' && i + 1 < n && text.charAt(i + 1) == '*') {
                int end = text.indexOf("*/", i + 2);
                if (end < 0) {
                    problems.add(new RuleProblem(file, line, "a /* comment is never closed"));
                    return rules;
                }
                for (int k = i; k < end; k++) {
                    if (text.charAt(k) == '\n') {
                        line++;
                    }
                }
                i = end + 2;
                continue;
            }
            if (c == '\n') {
                line++;
            } else if (c == '"') {
                inString = true;
            } else if (c == '[' && depth == 0 && !sawArray) {
                sawArray = true;
            } else if (c == '{') {
                if (!sawArray) {
                    problems.add(new RuleProblem(file, line, "the file must be a list of rules: start it with ["));
                    return rules;
                }
                if (depth == 0) {
                    start = i;
                    startLine = line;
                    comments = pending;
                    pending = new ArrayList<String>();
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth < 0) {
                    problems.add(new RuleProblem(file, line, "unmatched }"));
                    return rules;
                }
                if (depth == 0) {
                    rules.add(new RawRule(startLine, comments, text.substring(start, i + 1)));
                }
            }
            i++;
        }
        if (!sawArray) {
            problems.add(new RuleProblem(file, 1, "the file must be a list of rules: start it with ["));
        } else if (depth > 0) {
            problems.add(new RuleProblem(file, startLine, "the rule starting here is never closed with }"));
        } else if (inString) {
            problems.add(new RuleProblem(file, line, "a string is never closed"));
        }
        return rules;
    }
}
