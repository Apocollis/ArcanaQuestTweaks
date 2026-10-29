package com.apocollis.aqtweaks.spawning;

import java.util.Set;

/**
 * Mixed into In Control's {@code PotentialSpawnRule}: the parts of a rule that decide, from the dimension, the biome
 * and the creature type alone, whether the rule can match. {@link PotentialSpawnIndex} uses them to skip the rest.
 * A null field means the rule does not restrict that, or its JSON could not be read, and the rule is kept.
 */
public interface IndexedSpawnRule {
    void aqtweaks$setIndex(String creatureType, int[] dimensions, Set<String> biomes);

    String aqtweaks$creatureType();

    int[] aqtweaks$dimensions();

    Set<String> aqtweaks$biomes();
}
