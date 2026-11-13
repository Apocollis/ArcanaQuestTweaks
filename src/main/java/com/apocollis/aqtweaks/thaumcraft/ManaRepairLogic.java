package com.apocollis.aqtweaks.thaumcraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Pure rules of the Mana-Weave infusion: Botania's prices, which Manasteel item a given item is infused with, and
 * which items are already Manasteel. No Minecraft or mod types, so it is unit tested.
 */
public final class ManaRepairLogic {
    private ManaRepairLogic() {}

    /** Botania's own prices: repair per durability point, and mana paid per point of damage prevented. */
    public static final int TOOL_REPAIR_COST = 120;
    public static final int ARMOR_REPAIR_COST = 140;
    public static final int TOOL_DAMAGE_COST = 60;
    public static final int ARMOR_DAMAGE_COST = 70;

    /** A normal Mana Tablet holds 500,000; that much is enough for the recipe whatever the tablet's own maximum. */
    public static final int REQUIRED_TABLET_MANA = 500_000;

    /** Infusion numbers, kept together so they can be tuned after a test. */
    public static final int INSTABILITY = 9;
    public static final int ESSENTIA_INSTRUMENTUM = 30;
    public static final int ESSENTIA_PRAEMUNIO = 30;
    public static final int ESSENTIA_POTENTIA = 60;
    public static final int ESSENTIA_PERMUTATIO = 30;

    public enum Kind { HELMET, CHEST, LEGS, BOOTS, PICKAXE, AXE, SHOVEL, SWORD, SHEARS, OTHER }

    /** Botania's Manasteel tools and armor (registry names). */
    private static final Set<String> BOTANIA_MANASTEEL = Set.of(
            "botania:manasteelhelm", "botania:manasteelhelmreveal", "botania:manasteelchest", "botania:manasteellegs",
            "botania:manasteelboots", "botania:manasteelpick", "botania:manasteelshovel", "botania:manasteelaxe",
            "botania:manasteelsword", "botania:manasteelshears");

    private static final String SPARTAN_SHIELD_MANASTEEL = "spartanshields:shield_botania_manasteel";

    /** Weapon types for which Spartan Weaponry Arcana has a Manasteel variant (checked against the FantasticLib dump). */
    private static final List<String> SPARTAN_TYPES = List.of(
            "dagger", "longsword", "katana", "saber", "rapier", "greatsword", "hammer", "warhammer", "spear", "halberd",
            "pike", "lance", "longbow", "crossbow", "throwing_knife", "throwing_axe", "javelin", "boomerang", "battleaxe",
            "mace", "glaive", "staff", "parrying_dagger");

    /** True for an item that already is a Manasteel item (Botania's gear, or Spartan's Manasteel shield and weapons). */
    public static boolean isManasteelItem(String registryName) {
        if (registryName == null) return false;
        if (BOTANIA_MANASTEEL.contains(registryName) || SPARTAN_SHIELD_MANASTEEL.equals(registryName)) return true;
        if (!registryName.startsWith("spartanweaponryarcana:") || !registryName.endsWith("_manasteel")) return false;
        String path = registryName.substring("spartanweaponryarcana:".length(), registryName.length() - "_manasteel".length());
        return SPARTAN_TYPES.contains(path);
    }

    /**
     * Registry names of the Manasteel item the given item is infused with, or an empty list when it has no cousin
     * and a Manasteel block is used instead.
     */
    public static List<String> cousins(String registryName, Kind kind) {
        if (registryName != null) {
            int colon = registryName.indexOf(':');
            String namespace = colon < 0 ? "" : registryName.substring(0, colon);
            String path = colon < 0 ? registryName : registryName.substring(colon + 1);
            if (namespace.equals("spartanshields") && path.startsWith("shield_")) {
                return List.of(SPARTAN_SHIELD_MANASTEEL);
            }
            if (namespace.startsWith("spartanweaponry")) {
                String type = spartanType(path);
                if (type != null) return List.of("spartanweaponryarcana:" + type + "_manasteel");
                return Collections.emptyList();
            }
        }
        if (kind == null) return Collections.emptyList();
        return switch (kind) {
            case HELMET -> List.of("botania:manasteelhelm", "botania:manasteelhelmreveal");
            case CHEST -> List.of("botania:manasteelchest");
            case LEGS -> List.of("botania:manasteellegs");
            case BOOTS -> List.of("botania:manasteelboots");
            case PICKAXE -> List.of("botania:manasteelpick");
            case AXE -> List.of("botania:manasteelaxe");
            case SHOVEL -> List.of("botania:manasteelshovel");
            case SWORD -> List.of("botania:manasteelsword");
            case SHEARS -> List.of("botania:manasteelshears");
            case OTHER -> Collections.emptyList();
        };
    }

    /** The longest known weapon type that a Spartan item path starts with ({@code type_material}), or null. */
    static String spartanType(String path) {
        String best = null;
        for (String type : SPARTAN_TYPES) {
            if (path.startsWith(type + "_") && (best == null || type.length() > best.length())) best = type;
        }
        return best;
    }

    /** A price scaled by the config multiplier; never below 1 unless the multiplier is 0 (free). */
    public static int scaled(int base, double multiplier) {
        if (multiplier <= 0.0D) return 0;
        return (int) Math.max(1L, Math.round(base * multiplier));
    }

    /** Mana to pay for {@code amount} points of durability damage, saturating at the int range. */
    public static int damageCost(int amount, boolean armor, double multiplier) {
        if (amount <= 0) return 0;
        long cost = (long) scaled(armor ? ARMOR_DAMAGE_COST : TOOL_DAMAGE_COST, multiplier) * amount;
        return (int) Math.min(Integer.MAX_VALUE, cost);
    }

    public static int repairCost(boolean armor, double multiplier) {
        return scaled(armor ? ARMOR_REPAIR_COST : TOOL_REPAIR_COST, multiplier);
    }

    /** A usable tablet: a normal (not creative) tablet holding at least {@link #REQUIRED_TABLET_MANA}. */
    public static boolean tabletQualifies(int mana, boolean creative) {
        return !creative && mana >= REQUIRED_TABLET_MANA;
    }

    /** All registry names that can serve as a central item's cousin, for tests and docs. */
    public static List<String> allKnownCousinNames() {
        List<String> all = new ArrayList<>(BOTANIA_MANASTEEL);
        all.add(SPARTAN_SHIELD_MANASTEEL);
        for (String type : SPARTAN_TYPES) all.add("spartanweaponryarcana:" + type + "_manasteel");
        return all;
    }
}
