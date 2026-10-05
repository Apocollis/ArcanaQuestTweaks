package com.apocollis.aqtweaks.reskillable;

import codersafterdark.reskillable.api.data.RequirementHolder;
import codersafterdark.reskillable.api.unlockable.Trait;
import codersafterdark.reskillable.api.unlockable.UnlockableConfig;
import net.minecraft.util.ResourceLocation;

public class AqtweaksTrait extends Trait {

    /** Registered traits and their layouts, restamped once every trait exists (see ReskillablePerkRegistry). */
    static final java.util.Map<String, ReskillablePerkLayout> LAYOUTS = new java.util.LinkedHashMap<>();

    public AqtweaksTrait(String path, ReskillablePerkLayout layout) {
        super(new ResourceLocation("aqtweaks", path), layout.x, layout.y,
                new ResourceLocation(parentOf(layout)), layout.cost, withoutTraitRefs(reqsOf(layout)));
        UnlockableConfig cfg = getUnlockableConfig();
        cfg.setEnabled(layout.enable);
        cfg.setX(layout.x);
        cfg.setY(layout.y);
        cfg.setCost(layout.cost);
        cfg.setRequirementHolder(RequirementHolder.fromStringList(withoutTraitRefs(reqsOf(layout))));
        LAYOUTS.put(path, layout);
    }

    /**
     * Requirement rows that name another trait ({@code trait|...}, {@code not|trait|...}) are left out at
     * construction: Reskillable resolves them immediately, and the trait may not be registered yet, which
     * logged an "Unlockable not found" error per forward reference. The registry restamps the full list
     * after all traits exist.
     */
    static String[] withoutTraitRefs(String[] reqs) {
        java.util.List<String> kept = new java.util.ArrayList<>();
        for (String r : reqs) {
            if (r != null && !r.contains("trait|")) kept.add(r);
        }
        return kept.toArray(new String[0]);
    }

    static boolean hasTraitRefs(String[] reqs) {
        if (reqs == null) return false;
        for (String r : reqs) {
            if (r != null && r.contains("trait|")) return true;
        }
        return false;
    }

    private static String parentOf(ReskillablePerkLayout layout) {
        if (layout.parentSkill == null || layout.parentSkill.isEmpty()) {
            return "reskillable:attack";
        }
        return layout.parentSkill;
    }

    private static String[] reqsOf(ReskillablePerkLayout layout) {
        if (layout == null) {
            return new String[0];
        }
        layout.sanitizeRequirements();
        return layout.requirements != null ? layout.requirements : new String[0];
    }
}
