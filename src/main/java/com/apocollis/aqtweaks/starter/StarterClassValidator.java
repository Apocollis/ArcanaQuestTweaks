package com.apocollis.aqtweaks.starter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure validation of the class list (plan 12.2 and 12.9). An invalid class is left off the screen and reported. */
public final class StarterClassValidator {

    /** What the validator asks the running game. */
    public interface Environment {
        boolean skillExists(String skillId);

        boolean kitExists(String kit);

        /** True when the registry id names a Tweaks class-exclusive perk. */
        boolean isClassPerk(String perkId);

        /** Shared utility slots of the default inventory that this kit does not reproduce; empty when it is fine. */
        List<String> kitDrift(String kit);
    }

    public static final class Result {
        public final List<StarterClassDef> valid = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
    }

    private StarterClassValidator() {}

    public static Result validate(List<StarterClassDef> defs, Environment env) {
        Result result = new Result();
        if (defs == null) {
            result.errors.add("no classes list in the config");
            return result;
        }
        Set<String> ids = new HashSet<>();
        Map<String, String> perkOwner = new HashMap<>();
        for (int i = 0; i < defs.size(); i++) {
            StarterClassDef def = defs.get(i);
            String label = "class #" + (i + 1);
            if (def == null) {
                result.errors.add(label + ": empty entry");
                continue;
            }
            if (def.id == null || def.id.trim().isEmpty()) {
                result.errors.add(label + ": empty id");
                continue;
            }
            String id = def.id.trim();
            label = "class '" + id + "'";
            if (!ids.add(id)) {
                result.errors.add(label + ": duplicate id");
                continue;
            }
            String error = check(def, env, perkOwner, id);
            if (error != null) {
                result.errors.add(label + ": " + error);
                continue;
            }
            if (def.perk != null && !def.perk.isEmpty()) {
                perkOwner.put(def.perk, id);
            }
            result.valid.add(def);
        }
        return result;
    }

    private static String check(StarterClassDef def, Environment env, Map<String, String> perkOwner, String id) {
        if (def.csgKit == null || def.csgKit.isEmpty()) {
            return "no csg_kit";
        }
        if (!env.kitExists(def.csgKit)) {
            return "unknown csg_kit '" + def.csgKit + "'";
        }
        List<String> drift = env.kitDrift(def.csgKit);
        if (drift != null && !drift.isEmpty()) {
            return "kit '" + def.csgKit + "' differs from the default inventory in shared slots " + drift;
        }
        if (def.skills != null) {
            for (Map.Entry<String, Integer> e : def.skills.entrySet()) {
                if (e.getKey() == null || !env.skillExists(e.getKey())) {
                    return "unknown skill '" + e.getKey() + "'";
                }
                if (e.getValue() == null || e.getValue() <= 0) {
                    return "non-positive level for " + e.getKey();
                }
            }
        }
        if (def.perk != null && !def.perk.isEmpty()) {
            if (!env.isClassPerk(def.perk)) {
                return "perk '" + def.perk + "' is not a registered class perk";
            }
            String owner = perkOwner.get(def.perk);
            if (owner != null) {
                return "perk '" + def.perk + "' is already used by class '" + owner + "'";
            }
        }
        return null;
    }
}
