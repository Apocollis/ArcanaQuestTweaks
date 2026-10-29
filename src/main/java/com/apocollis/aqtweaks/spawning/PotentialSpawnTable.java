package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import com.apocollis.aqtweaks.spawning.table.SpawnRuleSpec;
import com.apocollis.aqtweaks.spawning.table.SpawnTableBuilder;
import com.apocollis.aqtweaks.spawning.table.SpawnTableSources;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mcjty.incontrol.InControl;
import mcjty.incontrol.rules.PotentialSpawnRule;
import mcjty.incontrol.rules.RuleCache;
import mcjty.tools.cache.StructureCache;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The potential-spawn rows that {@code regenerate_potentialspawn.py} writes to In Control's {@code potentialspawn.json},
 * built here from the same classification files. A spawn-list query is one hash lookup for the dimension, biome and
 * creature type, then the tests that need the position (height, water, structure) and In Control's own per-player
 * count for the rows it returns; no rule is walked and no biome is looked up per rule.
 *
 * <p>The files are read at start-up and on reload. Entity classes are resolved on the first query, once the registry
 * is complete. Anything unexpected hands the query back to In Control. {@code /aqspawntable verify} compares the table
 * with In Control's file for every dimension, biome and creature type.
 */
public final class PotentialSpawnTable {
    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Spawning");

    private static final class Key {
        final int dimension;
        final String biome;
        final EnumCreatureType type;
        final int hash;

        Key(int dimension, String biome, EnumCreatureType type) {
            this.dimension = dimension;
            this.biome = biome;
            this.type = type;
            this.hash = (dimension * 31 + biome.hashCode()) * 31 + (type == null ? 0 : type.ordinal());
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) {
                return false;
            }
            Key key = (Key) other;
            return key.dimension == dimension && key.type == type && key.biome.equals(biome);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /** A row with its classes resolved; a null entry is a removal row. */
    private static final class Row {
        final SpawnRuleSpec spec;
        final Biome.SpawnListEntry entry;
        final Class<? extends Entity> countClass;
        final List<Class<?>> removeClasses;

        Row(SpawnRuleSpec spec, Biome.SpawnListEntry entry, Class<? extends Entity> countClass,
            List<Class<?>> removeClasses) {
            this.spec = spec;
            this.entry = entry;
            this.countClass = countClass;
            this.removeClasses = removeClasses;
        }
    }

    private static final class Loaded {
        final List<SpawnRuleSpec> specs;
        volatile List<Row> rows;
        final Map<Key, Row[]> byQuery = new ConcurrentHashMap<Key, Row[]>();

        Loaded(List<SpawnRuleSpec> specs) {
            this.specs = specs;
        }
    }

    private static final Row[] NONE = new Row[0];

    private static File configDir;
    private static volatile Loaded loaded;
    private static volatile boolean failureLogged;

    private PotentialSpawnTable() {
    }

    public static void load(File configDirectory) {
        configDir = configDirectory;
        reload();
    }

    /** Reads the classification files again and drops everything resolved from the old ones. */
    public static String reload() {
        File config = configDir;
        failureLogged = false;
        if (config == null) {
            loaded = null;
            return "no config directory";
        }
        File dir = packDirectory(config);
        try {
            SpawnTableSources sources = SpawnTableSources.load(name -> {
                File file = new File(dir, name);
                return file.isFile() ? new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8) : null;
            });
            List<SpawnRuleSpec> specs = SpawnTableBuilder.build(sources);
            loaded = new Loaded(specs);
            String message = "built " + specs.size() + " potential-spawn rows from " + dir.getAbsolutePath();
            LOGGER.info("[AQTweaks] Potential spawn table: {}.", message);
            return message;
        } catch (IOException | RuntimeException failure) {
            loaded = null;
            LOGGER.warn("[AQTweaks] Potential spawn table not built from {}; In Control's rules stay in charge.",
                    dir.getAbsolutePath(), failure);
            return "not built: " + failure.getMessage();
        }
    }

    private static File packDirectory(File config) {
        String name = SpawningModuleConfig.general.potentialSpawnTableDirectory;
        File dir = new File(config, name == null || name.isBlank() ? "arcanaquest" : name);
        if (!dir.isDirectory() && "arcanaquest".equals(name)) {
            File legacy = new File(config, "ArcanaQuest");
            if (legacy.isDirectory()) {
                return legacy;
            }
        }
        return dir;
    }

    /** The pack folder the table and the rule files are read from, or null before the config folder is known. */
    public static File packDirectory() {
        File config = configDir;
        return config == null ? null : packDirectory(config);
    }

    public static File configDirectory() {
        return configDir;
    }

    /** The rows as built, or null when the files could not be read. */
    public static List<SpawnRuleSpec> specs() {
        Loaded current = loaded;
        return current == null ? null : current.specs;
    }

    public static String describe() {
        Loaded current = loaded;
        boolean on = SpawningModuleConfig.general.usePotentialSpawnTable;
        if (current == null) {
            return "table " + (on ? "on" : "off") + ", not built";
        }
        return "table " + (on ? "on" : "off") + ", " + current.specs.size() + " rows, "
                + current.byQuery.size() + " queries cached";
    }

    /**
     * Adds the table's entries for this query to the spawn list. Returns false, leaving the list alone, when the table
     * is off or unavailable; the caller then lets In Control's rules run.
     */
    public static boolean apply(WorldEvent.PotentialSpawns event) {
        if (!SpawningModuleConfig.general.usePotentialSpawnTable) {
            return false;
        }
        Loaded current = loaded;
        World world = event.getWorld();
        if (current == null || world == null) {
            return false;
        }
        Row[] rows;
        BlockPos pos = event.getPos();
        Row[] passed;
        try {
            rows = rowsFor(current, world, event);
            if (rows == null) {
                return false;
            }
            passed = rows.length == 0 ? NONE : new Row[rows.length];
            int count = 0;
            for (Row row : rows) {
                if (passes(row, world, pos)) {
                    passed[count++] = row;
                }
            }
            if (count < passed.length) {
                Row[] trimmed = new Row[count];
                System.arraycopy(passed, 0, trimmed, 0, count);
                passed = trimmed;
            }
        } catch (RuntimeException unexpected) {
            if (!failureLogged) {
                failureLogged = true;
                LOGGER.warn("[AQTweaks] Potential spawn table failed; In Control's rules handle this query.",
                        unexpected);
            }
            return false;
        }
        List<Biome.SpawnListEntry> list = event.getList();
        for (Row row : passed) {
            if (row.entry == null) {
                for (Class<?> clazz : row.removeClasses) {
                    for (int index = list.size() - 1; index >= 0; index--) {
                        if (list.get(index).entityClass == clazz) {
                            list.remove(index);
                        }
                    }
                }
            } else {
                list.add(row.entry);
            }
        }
        return true;
    }

    private static boolean passes(Row row, World world, BlockPos pos) {
        SpawnRuleSpec spec = row.spec;
        if (spec.isRemoval()) {
            return true;
        }
        if (spec.maxHeight != null && pos.getY() > spec.maxHeight) {
            return false;
        }
        if (spec.water && world.getBlockState(pos.down()).getBlock() != Blocks.WATER) {
            return false;
        }
        if (spec.structure != null && !StructureCache.CACHE.isInStructure(world, spec.structure, pos)) {
            return false;
        }
        RuleCache cache = InControl.setup.cache;
        return cache.getCount(world, row.countClass) < spec.limit * cache.getValidPlayers(world);
    }

    private static Row[] rowsFor(Loaded current, World world, WorldEvent.PotentialSpawns event) {
        Biome biome = world.getBiome(event.getPos());
        ResourceLocation name = biome.getRegistryName();
        String biomeName = name == null ? "" : name.toString();
        int dimension = world.provider.getDimension();
        EnumCreatureType type = event.getType();
        Key key = new Key(dimension, biomeName, type);
        Row[] rows = current.byQuery.get(key);
        if (rows == null) {
            List<Row> resolved = current.rows;
            if (resolved == null) {
                resolved = resolve(current.specs);
                current.rows = resolved;
            }
            List<Row> kept = new ArrayList<Row>();
            for (Row row : resolved) {
                if (row.spec.appliesTo(dimension, biomeName, type == null ? "" : type.name())) {
                    kept.add(row);
                }
            }
            rows = kept.isEmpty() ? NONE : kept.toArray(new Row[0]);
            current.byQuery.put(key, rows);
        }
        return rows;
    }

    /** Looks up the entity classes the way In Control does; a mob it cannot find leaves the row out, as there. */
    @SuppressWarnings("unchecked")
    private static List<Row> resolve(List<SpawnRuleSpec> specs) {
        List<Row> rows = new ArrayList<Row>();
        for (SpawnRuleSpec spec : specs) {
            if (spec.isRemoval()) {
                List<Class<?>> classes = new ArrayList<Class<?>>();
                for (String id : spec.remove) {
                    Class<? extends Entity> clazz = entityClass(id);
                    if (clazz == null) {
                        LOGGER.warn("[AQTweaks] Potential spawn table: cannot find mob '{}' to remove.", id);
                        break;
                    }
                    classes.add(clazz);
                }
                rows.add(new Row(spec, null, null, classes));
                continue;
            }
            Class<? extends Entity> clazz = entityClass(spec.mob);
            if (clazz == null) {
                LOGGER.warn("[AQTweaks] Potential spawn table: cannot find mob '{}'; its row is skipped.", spec.mob);
                continue;
            }
            Biome.SpawnListEntry entry = new Biome.SpawnListEntry((Class<? extends EntityLiving>) clazz, spec.weight,
                    spec.groupMin, spec.groupMax);
            rows.add(new Row(spec, entry, clazz, null));
        }
        return rows;
    }

    private static Class<? extends Entity> entityClass(String id) {
        String fixed;
        try {
            fixed = PotentialSpawnRule.fixEntityId(id);
        } catch (RuntimeException unavailable) {
            fixed = id;
        }
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(fixed));
        return entry == null ? null : entry.getEntityClass();
    }
}
