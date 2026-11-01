package com.apocollis.aqtweaks.spawning;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.SpawningModuleConfig;
import mcjty.incontrol.config.GeneralConfiguration;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

public final class SpawnPackFiller {

    @FunctionalInterface
    public interface ExtraFactory {
        EntityLiving create(World world) throws Exception;
    }

    private SpawnPackFiller() {}

    public static boolean ownsVanillaRemainder(EntityLiving living) {
        return living != null && SpawnGroupSizes.appliesTo(living.world, living);
    }

    public static void onVanillaSpawned(EntityLiving living) {
        if (living == null || SpawnPackContext.filling() || !SpawnPackContext.inSpawner()) {
            return;
        }
        if (!SpawnGroupSizes.moduleEnabled()) {
            return;
        }
        SpawnPackContext.setFilling(true);
        try {
            fillSameSpecies(living);
            SpawnParties.tryAfterLeader(living);
        } finally {
            SpawnPackContext.setFilling(false);
        }
    }

    private static void fillSameSpecies(EntityLiving living) {
        World world = living.world;
        if (!SpawnGroupSizes.appliesTo(world, living)) {
            return;
        }
        Biome.SpawnListEntry entry = SpawnPackContext.entry();
        if (entry == null || entry.entityClass == null || !entry.entityClass.isInstance(living)) {
            return;
        }
        SpawnGroupSizes.Range range = SpawnGroupSizes.resolve(entry, living);
        int target = SpawnGroupSizes.rollTarget(world, range);
        SpawnTrace.log(SpawnTraceHandler.idOf(living), "pack   " + SpawnTraceHandler.idOf(living) + " group range "
                + range.min() + ".." + range.max() + ", rolled group size " + target);
        if (target <= 1) {
            return;
        }
        placeNearby(world, living, target - 1, entry.entityClass, w -> entry.newInstance(w), false);
    }

    /**
     * @param skipIfLayerStrips when true, skip a candidate pos if {@link SpawnLayerFilter} would
     *                          drop this id there (mixed-group companions).
     */
    public static int placeNearby(World world, EntityLiving first, int need,
                                  Class<? extends net.minecraft.entity.Entity> spawnClass, ExtraFactory factory,
                                  boolean skipIfLayerStrips) {
        int[] stats = new int[4];
        int placed = placeNearby(world, first, need, spawnClass, factory, skipIfLayerStrips, stats);
        net.minecraft.util.ResourceLocation wanted = spawnClass == null ? null
                : net.minecraft.entity.EntityList.getKey(spawnClass);
        String leader = first == null ? null : SpawnTraceHandler.idOf(first);
        if (SpawnTrace.watching(leader) || wanted != null && SpawnTrace.watching(wanted.toString())) {
            SpawnTrace.line("place  " + wanted + " beside " + leader + ": wanted " + need + ", placed " + placed
                    + "; candidate spots refused: by the layer filter " + stats[0] + ", by the spawn check (In Control or"
                    + " another mod, or vanilla placement) " + stats[1] + ", not colliding-free or join refused " + stats[2]);
        }
        return placed;
    }

    private static int placeNearby(World world, EntityLiving first, int need,
                                   Class<? extends net.minecraft.entity.Entity> spawnClass, ExtraFactory factory,
                                   boolean skipIfLayerStrips, int[] stats) {
        if (world == null || first == null || factory == null || spawnClass == null || need <= 0) {
            return 0;
        }
        var general = SpawningModuleConfig.general;
        int attempts = Math.max(1, general.maxExtraAttempts);
        int radius = Math.max(1, general.packRadius);
        int yRange = Math.max(0, general.yRange);
        EntityLiving.SpawnPlacementType placement = EntitySpawnPlacementRegistry.getPlacementForEntity(spawnClass);
        EnumCreatureType type = first instanceof IMob ? EnumCreatureType.MONSTER : EnumCreatureType.CREATURE;
        double minPlayer = type == EnumCreatureType.MONSTER
                ? GeneralConfiguration.MIN_PLAYER_MONSTER_SPAWN_DISTANCE
                : GeneralConfiguration.MIN_PLAYER_MOB_SPAWN_DISTANCE;
        IEntityLivingData data = null;
        int spawned = 0;
        int originX = MathHelper.floor(first.posX);
        int originY = MathHelper.floor(first.posY);
        int originZ = MathHelper.floor(first.posZ);

        for (int attempt = 0; attempt < attempts && spawned < need; attempt++) {
            int x = originX + world.rand.nextInt(radius * 2 + 1) - radius;
            int z = originZ + world.rand.nextInt(radius * 2 + 1) - radius;
            for (int dy = -yRange; dy <= yRange && spawned < need; dy++) {
                int y = originY + dy;
                BlockPos pos = new BlockPos(x, y, z);
                if (!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(placement, world, pos)) {
                    continue;
                }
                double px = x + 0.5;
                double pz = z + 0.5;
                if (world.isAnyPlayerWithinRangeAt(px, y, pz, minPlayer)) {
                    continue;
                }
                EntityLiving extra;
                try {
                    extra = factory.create(world);
                } catch (Exception e) {
                    return spawned;
                }
                if (extra == null) {
                    return spawned;
                }
                if (skipIfLayerStrips && SpawnLayerFilter.wouldStrip(world, pos, extra)) {
                    extra.setDead();
                    stats[0]++;
                    continue;
                }
                extra.setLocationAndAngles(px, y, pz, world.rand.nextFloat() * 360.0F, 0.0F);
                Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(extra, world, (float) px, y, (float) pz, false);
                boolean allowed = canSpawn == Event.Result.ALLOW
                        || (canSpawn == Event.Result.DEFAULT && extra.getCanSpawnHere() && extra.isNotColliding());
                if (!allowed) {
                    extra.setDead();
                    stats[1]++;
                    continue;
                }
                if (!ForgeEventFactory.doSpecialSpawn(extra, world, (float) px, y, (float) pz)) {
                    data = extra.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(extra)), data);
                }
                if (extra.isNotColliding()) {
                    if (world.spawnEntity(extra)) {
                        spawned++;
                    } else {
                        extra.setDead();
                        stats[2]++;
                    }
                } else {
                    extra.setDead();
                    stats[2]++;
                }
            }
        }
        return spawned;
    }
}
