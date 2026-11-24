package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A variant (plan 18.8): a registered entity that hangs off a tier, inherits the tier's body, equipment and drop table,
 * and replaces some of it. Its loadout is either a fixed focus list ({@link #fixedLoadout}, the roller is skipped) or its
 * own roller config ({@link #loadout}); with neither it rolls the tier's loadout. {@link #ai} is the fully resolved tuning:
 * the global block, then the tier's overrides, then the variant's.
 */
public final class VariantDef {
    public String id;
    public int tier = 1;
    /** Focus ids carried as they are, in order; null when the variant rolls. */
    public List<String> fixedLoadout;
    /** Roller config of its own; null inherits the tier's. */
    public LoadoutRoller.Config loadout;
    /** Resolved tuning; never null once the library is parsed. */
    public AiTuning ai;
    /** Role to selection weight multiplier (for example bolt 2.0). */
    public Map<String, Double> roleWeights = new LinkedHashMap<>();
    /** Body overrides; null keeps the tier's number. */
    public Double health;
    public Double armor;
    public Double focusPower;
    public Double shieldMax;
    /** Slot overrides applied over the tier's equipment (usually none). */
    public Map<String, EquipmentDef> equipment = new LinkedHashMap<>();
    /** Escort the mage brings (read by the variant that spawns one). */
    public List<EscortDef> escort = new ArrayList<>();

    /** The weight multiplier for a role, 1.0 when the variant does not mention it. */
    public double roleWeight(String role) {
        Double w = roleWeights.get(role);
        return w != null ? w : 1.0D;
    }
}
