package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One tier of the Battlemage: body, power, shield value, equipment, AI tuning and how its loadout is rolled. */
public final class TierDef {
    public int tier;
    /** Fixed list used only when the tier has no {@link #loadout} (or the roll comes back empty). */
    public List<String> stage1Foci = new ArrayList<>();
    public double health = 80.0D;
    public double armor = 1.0D;
    /** Multiplies the power of every cast of this tier (plan 11: Tier 1 1.25, Tier 2 1.5). */
    public double focusPower = 1.0D;
    /** Runic shield capacity (plan 12). Stored for stage 3; not used yet. */
    public double shieldMax;
    /** Null when the JSON has no {@code loadout} block. */
    public LoadoutRoller.Config loadout;
    /** Slot name (head, chest, legs, feet, mainhand, offhand) to its item, tag and enchantments. */
    public Map<String, EquipmentDef> equipment = new LinkedHashMap<>();
    /** Loot table rows (plan 13), rolled on a player kill. */
    public List<DropEntry> drops = new ArrayList<>();
    /** The faked baubles a kill can drop (plan 13b), picked by weight. */
    public List<DropEntry> baubleDrops = new ArrayList<>();
    /** Resolved tuning: the global {@code ai} block plus this tier's overrides; never null once parsed. */
    public AiTuning ai;
}
