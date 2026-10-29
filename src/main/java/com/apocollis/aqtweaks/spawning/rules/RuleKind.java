package com.apocollis.aqtweaks.spawning.rules;

/** The three rule files. */
public enum RuleKind {
    CONDITIONS("spawn_conditions.json"),
    MODIFIERS("spawn_modifiers.json"),
    GEAR("spawn_gear.json");

    public final String fileName;

    RuleKind(String fileName) {
        this.fileName = fileName;
    }
}
