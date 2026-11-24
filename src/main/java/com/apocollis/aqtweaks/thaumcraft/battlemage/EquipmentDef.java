package com.apocollis.aqtweaks.thaumcraft.battlemage;

import java.util.ArrayList;
import java.util.List;

/**
 * One equipment slot of a tier (schema 3): the item, an optional SNBT tag (Minecraft SNBT, parsed with
 * {@code JsonToNBT} when the mage spawns) and optional enchantments named by registry id (resolved at spawn, because
 * Forge hands out numeric enchantment ids per world). A bare string in the JSON is an item with no tag.
 */
public final class EquipmentDef {

    /** One enchantment: registry id plus level. */
    public static final class Ench {
        public final String id;
        public final int level;

        public Ench(String id, int level) {
            this.id = id;
            this.level = level;
        }
    }

    public String item;
    /** SNBT compound, or null. */
    public String nbt;
    public List<Ench> enchantments = new ArrayList<>();

    public EquipmentDef(String item) {
        this.item = item;
    }
}
