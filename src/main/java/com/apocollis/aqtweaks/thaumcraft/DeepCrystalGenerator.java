package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.perf.StageTimer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.registry.GameRegistry;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.world.ore.ShardType;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.world.biomes.BiomeHandler;

import java.util.Random;

/**
 * Seeds Thaumcraft vis-crystal clusters on cave surfaces below Y 0.
 * Mirrors {@code ThaumcraftWorldGenerator.generateOres}'s crystal pass, with Y chosen in a config band
 * instead of {@code nextInt(max(5, surfaceY - 5))}, which never samples below 0.
 */
public final class DeepCrystalGenerator implements IWorldGenerator {

    private DeepCrystalGenerator() {}

    public static void register() {
        GameRegistry.registerWorldGenerator(new DeepCrystalGenerator(), 0);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.isRemote) {
            return;
        }
        if (!ArcanaQuestTweaksConfig.ThaumcraftConfig.enableDeepVisCrystals) {
            return;
        }
        if (!ModConfig.CONFIG_WORLD.generateCrystals) {
            return;
        }
        int dimension = world.provider.getDimension();
        if (dimension != ModConfig.CONFIG_WORLD.overworldDim) {
            return;
        }
        int dimList = BiomeHandler.getDimBlacklist(dimension);
        if (dimList == 0 || dimList == 2) {
            return;
        }

        int biomeList = BiomeHandler.getBiomeBlacklist(Biome.getIdForBiome(world.getBiome(new BlockPos(chunkX * 16 + 8, 50, chunkZ * 16 + 8))));
        if (biomeList == 0 || biomeList == 2) {
            return;
        }

        int minY = ArcanaQuestTweaksConfig.ThaumcraftConfig.deepCrystalMinY;
        int maxY = ArcanaQuestTweaksConfig.ThaumcraftConfig.deepCrystalMaxY;
        if (minY > maxY) {
            return;
        }
        float scale = ArcanaQuestTweaksConfig.ThaumcraftConfig.deepCrystalDensityScale;
        if (scale <= 0.0F) {
            return;
        }
        float density = ModConfig.CONFIG_WORLD.oreDensity / 100.0F;
        int attempts = Math.round(8.0F * density * scale);
        int sizeBudget = Math.round(64.0F * density * scale);
        if (attempts <= 0) {
            return;
        }

        long started = StageTimer.start();
        int span = maxY - minY + 1;
        int placedSize = 0;
        for (int n = 0; n < attempts; n++) {
            int x = chunkX * 16 + 8 + MathHelper.getInt(random, -6, 6);
            int z = chunkZ * 16 + 8 + MathHelper.getInt(random, -6, 6);
            int y = minY + random.nextInt(span);
            BlockPos origin = new BlockPos(x, y, z);

            int meta = random.nextInt(6);
            if (random.nextInt(3) == 0) {
                Aspect tag = BiomeHandler.getRandomBiomeTag(Biome.getIdForBiome(world.getBiome(origin)), random);
                if (tag == null) {
                    meta = random.nextInt(6);
                } else {
                    meta = ShardType.getMetaByAspect(tag);
                }
            }
            Block ore = ShardType.byMetadata(meta).getOre();
            if (ore == null) {
                continue;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (random.nextInt(3) == 0) {
                            continue;
                        }
                        BlockPos pos = origin.add(dx, dy, dz);
                        IBlockState state = world.getBlockState(pos);
                        if (state.getMaterial().isLiquid()) {
                            continue;
                        }
                        if (!world.isAirBlock(pos) && !state.getBlock().isReplaceable(world, pos)) {
                            continue;
                        }
                        if (!BlockUtils.isBlockTouching(world, pos, Material.ROCK, true)) {
                            continue;
                        }
                        int size = 1 + random.nextInt(3);
                        if (world.setBlockState(pos, ore.getStateFromMeta(size), 0)) {
                            placedSize += size;
                        }
                    }
                }
            }
            if (placedSize > sizeBudget) {
                break;
            }
        }
        StageTimer.end("thaum.deepCrystals", started);
    }
}
