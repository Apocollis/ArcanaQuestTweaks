package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.spawning.rules.CountSpec;
import com.apocollis.aqtweaks.spawning.rules.SpawnQuery;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import mcjty.incontrol.InControl;
import mcjty.incontrol.config.GeneralConfiguration;
import mcjty.incontrol.rules.RuleCache;
import mcjty.tools.cache.StructureCache;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * A spawn as the conditions table sees it, read from the game the way In Control reads it: the same position, the same
 * nearest-player search, the same counts. Answers are fetched only when a row asks.
 */
abstract class LiveQuery implements SpawnQuery {
    private static final Random RANDOM = new Random();
    private static final Map<String, Class<? extends Entity>> CLASSES = new HashMap<String, Class<? extends Entity>>();

    final World world;
    final Entity entity;
    final BlockPos pos;

    LiveQuery(World world, Entity entity, BlockPos pos) {
        this.world = world;
        this.entity = entity;
        this.pos = pos;
    }

    @Override
    public String mobId() {
        ResourceLocation key = EntityList.getKey(entity.getClass());
        return key == null ? "" : key.toString();
    }

    @Override
    public String modId() {
        ResourceLocation key = EntityList.getKey(entity.getClass());
        return key == null ? "$not_found$" : key.getNamespace();
    }

    @Override
    public boolean hostile() {
        return entity instanceof IMob;
    }

    @Override
    public int dimension() {
        return world.provider.getDimension();
    }

    @Override
    public int timeOfDay() {
        return (int) world.getWorldTime();
    }

    @Override
    public boolean raining() {
        return world.isRaining();
    }

    @Override
    public boolean thundering() {
        return world.isThundering();
    }

    @Override
    public boolean seesSky() {
        return world.canBlockSeeSky(pos);
    }

    @Override
    public String biomeId() {
        Biome biome = world.getBiome(pos);
        ResourceLocation name = biome.getRegistryName();
        return name == null ? "" : name.toString();
    }

    @Override
    public boolean inStructure(String structure) {
        return StructureCache.CACHE.isInStructure(world, structure, pos);
    }

    @Override
    public boolean notColliding() {
        return entity instanceof EntityLiving && ((EntityLiving) entity).isNotColliding();
    }

    @Override
    public boolean hasStage(String stage) {
        EntityPlayer player = world.getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(),
                GeneralConfiguration.MAX_PLAYER_DISTANCE, false);
        return player != null && Loader.isModLoaded("gamestages") && Stages.has(player, stage);
    }

    @Override
    public boolean anyPlayerWithin(double blocks) {
        return world.isAnyPlayerWithinRangeAt(pos.getX(), pos.getY(), pos.getZ(), blocks);
    }

    @Override
    public int count(CountSpec spec) {
        RuleCache cache = InControl.setup.cache;
        switch (spec.kind) {
            case MOD:
                if (spec.hostileOnly) {
                    return cache.getCountPerModHostile(world, spec.mod);
                }
                return spec.passiveOnly ? cache.getCountPerModPassive(world, spec.mod)
                        : cache.getCountPerMod(world, spec.mod);
            case HOSTILE:
                return cache.getCountHostile(world);
            case PASSIVE:
                return cache.getCountPassive(world);
            case MOBS:
                int total = 0;
                for (String id : spec.mobs) {
                    Class<? extends Entity> clazz = classOf(id);
                    if (clazz != null) {
                        total += cache.getCount(world, clazz);
                    }
                }
                return total;
            default:
                return cache.getCount(world, entity.getClass());
        }
    }

    @Override
    public int scaledAmount(CountSpec spec) {
        RuleCache cache = InControl.setup.cache;
        switch (spec.scale) {
            case PER_CHUNK:
                return spec.amount * cache.getValidSpawnChunks(world) / 289;
            case PER_PLAYER:
                return spec.amount * cache.getValidPlayers(world);
            default:
                return spec.amount;
        }
    }

    @Override
    public float roll() {
        return RANDOM.nextFloat();
    }

    private static Class<? extends Entity> classOf(String id) {
        Class<? extends Entity> known = CLASSES.get(id);
        if (known == null && !CLASSES.containsKey(id)) {
            EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(id));
            known = entry == null ? null : entry.getEntityClass();
            CLASSES.put(id, known);
        }
        return known;
    }

    /** Game Stages is optional; its class is only loaded when a row asks for a stage and the mod is present. */
    private static final class Stages {
        static boolean has(EntityPlayer player, String stage) {
            return net.darkhax.gamestages.GameStageHelper.getPlayerData(player).hasStage(stage);
        }
    }

    /** A spawn check: the position is the one the game proposes, and a spawner may have made it. */
    static final class Check extends LiveQuery {
        private final LivingSpawnEvent.CheckSpawn event;

        Check(LivingSpawnEvent.CheckSpawn event) {
            super(event.getWorld(), event.getEntity(), new BlockPos(event.getX(), event.getY(), event.getZ()));
            this.event = event;
        }

        @Override
        public Boolean spawner() {
            return event.isSpawner();
        }

        @Override
        public int y() {
            return (int) event.getY();
        }
    }

    /** A join: the mob is already in place, and no spawner is known. */
    static final class Join extends LiveQuery {
        Join(EntityJoinWorldEvent event) {
            super(event.getWorld(), event.getEntity(), event.getEntity().getPosition());
        }

        @Override
        public Boolean spawner() {
            return null;
        }

        @Override
        public int y() {
            return entity.getPosition().getY();
        }
    }
}
