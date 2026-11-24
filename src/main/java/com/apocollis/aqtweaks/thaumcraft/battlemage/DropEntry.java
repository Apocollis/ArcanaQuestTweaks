package com.apocollis.aqtweaks.thaumcraft.battlemage;

/** One loot-table row of a tier (plan 13): an item, optional damage and SNBT tag, a chance and a count range. */
public final class DropEntry {
    public String item;
    public int meta;
    /** SNBT compound, or null. */
    public String nbt;
    /** 0 to 1; 1 is guaranteed. */
    public double chance = 1.0D;
    public int min = 1;
    public int max = 1;
    /** Used by the bauble list only: relative weight in the pick. */
    public double weight = 1.0D;
}
