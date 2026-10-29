package com.apocollis.aqtweaks.spawning.rules;

/** A mistake found in a rule file: which file, which line (the line a rule starts on), and what is wrong. */
public final class RuleProblem {
    public final String file;
    public final int line;
    public final String message;

    public RuleProblem(String file, int line, String message) {
        this.file = file;
        this.line = line;
        this.message = message;
    }

    @Override
    public String toString() {
        return file + ":" + line + ": " + message;
    }
}
