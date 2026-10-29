package com.apocollis.aqtweaks.spawning.rules;

import java.util.List;

/** One top-level {@code { ... }} of a rule file, as written: where it starts, the comments above it, its text. */
final class RawRule {
    final int line;
    final List<String> comments;
    final String text;

    RawRule(int line, List<String> comments, String text) {
        this.line = line;
        this.comments = comments;
        this.text = text;
    }
}
