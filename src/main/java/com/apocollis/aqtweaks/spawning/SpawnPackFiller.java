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

    private SpawnPackFiller() {}

    public static boolean ownsVanillaRemainder(EntityLiving living) {
        return living != null && SpawnGroupSizes.appliesTo(living.world, living);
    }

    public static void onVanillaSpawned(EntityLiving living) {
        if (living == null || SpawnPackContext.filling() || !SpawnPackContext.inSpawner()) {
            return;
        }
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
        if (target <= 1) {
            return;
        }
        SpawnPackContext.setFilling(true);
        try {
            fill(world, entry, living, target - 1);
        } finally {
            SpawnPackContext.setFilling(false);
        }
    }

    private static void fill(World world, Biome.SpawnListEntry entry, EntityLiving first, int need) {
        var general = SpawningModuleConfig.general;
        int attempts = Math.max(1, general.maxExtraAttempts);
        int radius = Math.max(1, general.packRadius);
        int yRange = Math.max(0, general.yRange);
        EntityLiving.SpawnPlacementType placement = EntitySpawnPlacementRegistry.getPlacementForEntity(entry.entityClass);
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
                    extra = entry.newInstance(world);
                } catch (Exception e) {
                    return;
                }
                extra.setLocationAndAngles(px, y, pz, world.rand.nextFloat() * 360.0F, 0.0F);
                Event.Result canSpawn = ForgeEventFactory.canEntitySpawn(extra, world, (float) px, y, (float) pz, false);
                boolean allowed = canSpawn == Event.Result.ALLOW
                        || (canSpawn == Event.Result.DEFAULT && extra.getCanSpawnHere() && extra.isNotColliding());
                if (!allowed) {
                    extra.setDead();
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
                    }
                } else {
                    extra.setDead();
                }
            }
        }
    }
}
