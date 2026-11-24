package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The parsed focus library: foci by id, tiers, variants and the AI tuning. */
public final class LibraryData {
    public final Map<String, FocusDef> foci = new LinkedHashMap<>();
    public final Map<Integer, TierDef> tiers = new LinkedHashMap<>();
    public final Map<String, VariantDef> variants = new LinkedHashMap<>();
    /** The global {@code ai} block; tiers and variants carry their own resolved copies. */
    public AiTuning ai = new AiTuning();
    public final List<String> problems = new ArrayList<>();
    /** {@code schemaVersion} of the file, 0 when it has none. */
    public int schemaVersion;

    public FocusDef focus(String id) {
        return foci.get(id);
    }

    public TierDef tier(int tier) {
        TierDef t = tiers.get(tier);
        if (t != null) {
            return t;
        }
        return tiers.get(1);
    }

    /** The variant with this id, or null (also for a null id, the plain Battlemage and Master Battlemage). */
    public VariantDef variant(String id) {
        return id == null ? null : variants.get(id);
    }

    /** The resolved tuning for a mage: its variant's if it has one, else its tier's, else the global block. */
    public AiTuning aiFor(int tier, String variantId) {
        VariantDef v = variant(variantId);
        if (v != null && v.ai != null) {
            return v.ai;
        }
        TierDef t = tier(tier);
        if (t != null && t.ai != null) {
            return t.ai;
        }
        return ai;
    }
}
