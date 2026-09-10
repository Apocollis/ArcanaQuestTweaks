package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public final class SpawnGroupSizes {

    public record Range(int min, int max) {}

    private static volatile Map<String, Range> CACHE;

    private SpawnGroupSizes() {}

    public static void invalidate() {
        CACHE = null;
    }

    public static boolean moduleEnabled() {
        return SpawningModuleConfig.general.enable;
    }

    public static boolean fillPackSize() {
        return moduleEnabled() && SpawningModuleConfig.general.fillPackSize;
    }

    /**
     * Value for {@code findChunksForSpawning} only. Must not call {@code getMaxNumberOfCreature()}
     * (that invoke is redirected). Vanilla: MONSTER 70, CREATURE 10, AMBIENT 15, WATER 5.
     */
    public static int capForFindChunks(EnumCreatureType type) {
        if (type == EnumCreatureType.MONSTER) {
            if (!moduleEnabled()) {
                return 70;
            }
            return Math.max(1, SpawningModuleConfig.general.hostileMobCap);
        }
        if (type == EnumCreatureType.CREATURE) {
            return 10;
        }
        if (type == EnumCreatureType.AMBIENT) {
            return 15;
        }
        if (type == EnumCreatureType.WATER_CREATURE) {
            return 5;
        }
        return 15;
    }

    public static boolean appliesTo(World world, EntityLiving living) {
        if (world == null || living == null || !fillPackSize()) {
            return false;
        }
        var general = SpawningModuleConfig.general;
        if (general.overworldOnly && world.provider.getDimension() != 0) {
            return false;
        }
        return !general.monsterOnly || living instanceof IMob;
    }

    public static Range resolve(Biome.SpawnListEntry entry, EntityLiving living) {
        int min = entry != null ? entry.minGroupCount : 1;
        int max = entry != null ? entry.maxGroupCount : 1;
        String id = idOf(living);
        if (id == null && entry != null && entry.entityClass != null) {
            ResourceLocation key = EntityList.getKey(entry.entityClass);
            id = key != null ? key.toString() : null;
        }
        Range override = id != null ? overrides().get(id) : null;
        if (override != null) {
            min = override.min();
            max = override.max();
        } else {
            Range tier = id != null ? SpawnGroupCounts.rangeOf(id) : null;
            if (tier != null) {
                min = tier.min();
                max = tier.max();
            }
        }
        int cap = Math.max(1, SpawningModuleConfig.general.groupSizeCap);
        min = Math.max(1, min);
        max = Math.max(min, max);
        min = Math.min(min, cap);
        max = Math.min(max, cap);
        return new Range(min, max);
    }

    public static int rollTarget(World world, Range range) {
        int span = range.max() - range.min() + 1;
        if (span <= 1) {
            return range.min();
        }
        return range.min() + world.rand.nextInt(span);
    }

    private static String idOf(EntityLiving living) {
        if (living == null) {
            return null;
        }
        ResourceLocation key = EntityList.getKey(living);
        return key != null ? key.toString() : null;
    }

    private static Map<String, Range> overrides() {
        Map<String, Range> map = CACHE;
        if (map == null) {
            map = parse(SpawningModuleConfig.general.groupSizeOverrides);
            CACHE = map;
        }
        return map;
    }

    private static Map<String, Range> parse(String[] entries) {
        if (entries == null || entries.length == 0) {
            return Collections.emptyMap();
        }
        Map<String, Range> map = new HashMap<>();
        for (String raw : entries) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.lastIndexOf('=');
            if (eq <= 0 || eq == line.length() - 1) {
                continue;
            }
            String name = line.substring(0, eq).trim();
            String rhs = line.substring(eq + 1).trim().replace(',', '-');
            String[] parts = rhs.split("-", 2);
            try {
                int min = Integer.parseInt(parts[0].trim());
                int max = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : min;
                if (min < 1 || max < min) {
                    continue;
                }
                map.put(name, new Range(min, max));
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }
}
