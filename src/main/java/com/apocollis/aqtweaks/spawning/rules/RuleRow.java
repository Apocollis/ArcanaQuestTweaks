package com.apocollis.aqtweaks.spawning.rules;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.List;

/** One validated rule: the file it came from, the line it starts on, the comments above it, and its keys. */
public final class RuleRow {
    public final RuleKind kind;
    public final int line;
    public final List<String> comments;
    public final JsonObject json;

    RuleRow(RuleKind kind, int line, List<String> comments, JsonObject json) {
        this.kind = kind;
        this.line = line;
        this.comments = Collections.unmodifiableList(comments);
        this.json = json;
    }

    /** The rule as one line of JSON, the form the files use. */
    @Override
    public String toString() {
        return json.toString();
    }
}
