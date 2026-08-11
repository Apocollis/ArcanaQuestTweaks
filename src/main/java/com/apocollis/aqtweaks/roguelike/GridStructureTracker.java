package com.apocollis.aqtweaks.roguelike;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import com.apocollis.aqtweaks.util.Reflect;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import java.util.Random;

public class GridStructureTracker {

    public static boolean shouldSpawnAt(World world, int chunkX, int chunkZ) {
        if (!ArcanaQuestTweaksConfig.RoguelikeDungeonsConfig.enableGridSpawning) {
            return true; // Use default mod rules if grid spawning is disabled
        }

        int minSpacing = ArcanaQuestTweaksConfig.RoguelikeDungeonsConfig.minChunkDistance;
        int maxSpacing = ArcanaQuestTweaksConfig.RoguelikeDungeonsConfig.maxChunkDistance;
        if (minSpacing >= maxSpacing) {
            maxSpacing = minSpacing + 1;
        }
        if (maxSpacing <= 0) maxSpacing = 32;

        int gridX = chunkX < 0 ? (chunkX - maxSpacing + 1) / maxSpacing : chunkX / maxSpacing;
        int gridZ = chunkZ < 0 ? (chunkZ - maxSpacing + 1) / maxSpacing : chunkZ / maxSpacing;

        long seed = world != null ? Reflect.getSeed(world) : 0L;
        Random rand = new Random(
            gridX * 341873128712L + 
            gridZ * 132897987541L + 
            seed + 
            ArcanaQuestTweaksConfig.RoguelikeDungeonsConfig.gridSeedOffset
        );

        int offsetMax = maxSpacing - minSpacing;
        if (offsetMax <= 0) offsetMax = 1;

        int offsetX = rand.nextInt(offsetMax);
        int offsetZ = rand.nextInt(offsetMax);

        int targetChunkX = gridX * maxSpacing + offsetX;
        int targetChunkZ = gridZ * maxSpacing + offsetZ;

        if (chunkX == targetChunkX && chunkZ == targetChunkZ) {
            if (world != null) {
                return isValidSpawnLocation(world, chunkX, chunkZ);
            }
            return true;
        }
        return false;
    }

    public static ChunkPos getNearestStructure(World world, int currentChunkX, int currentChunkZ) {
        int maxSpacing = ArcanaQuestTweaksConfig.RoguelikeDungeonsConfig.maxChunkDistance;
        if (maxSpacing <= 0) maxSpacing = 32;

        int minSpacing = ArcanaQuestTweaksConfig.RoguelikeDungeonsConfig.minChunkDistance;
        if (minSpacing >= maxSpacing) minSpacing = maxSpacing - 1;
        if (minSpacing < 0) minSpacing = 0;
        int offsetMax = maxSpacing - minSpacing;
        if (offsetMax <= 0) offsetMax = 1;

        long seed = world != null ? Reflect.getSeed(world) : 0L;

        int currentGridX = currentChunkX < 0 ? (currentChunkX - maxSpacing + 1) / maxSpacing : currentChunkX / maxSpacing;
        int currentGridZ = currentChunkZ < 0 ? (currentChunkZ - maxSpacing + 1) / maxSpacing : currentChunkZ / maxSpacing;

        // Spiral search outwards up to 100 cells
        for (int distance = 0; distance <= 100; distance++) {
            for (int x = -distance; x <= distance; x++) {
                boolean isEdgeX = Math.abs(x) == distance;
                for (int z = -distance; z <= distance; z++) {
                    boolean isEdgeZ = Math.abs(z) == distance;
                    if (!isEdgeX && !isEdgeZ) continue;

                    int cellX = currentGridX + x;
                    int cellZ = currentGridZ + z;

                    Random rand = new Random(
                        cellX * 341873128712L + 
                        cellZ * 132897987541L + 
                        seed + 
                        ArcanaQuestTweaksConfig.RoguelikeDungeonsConfig.gridSeedOffset
                    );

                    int offsetX = rand.nextInt(offsetMax);
                    int offsetZ = rand.nextInt(offsetMax);

                    int chunkX = cellX * maxSpacing + offsetX;
                    int chunkZ = cellZ * maxSpacing + offsetZ;

                    if (world == null || isValidSpawnLocation(world, chunkX, chunkZ)) {
                        return new ChunkPos(chunkX, chunkZ);
                    }
                }
            }
        }
        return null;
    }

    public static boolean isValidSpawnLocation(World world, int chunkX, int chunkZ) {
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;

        // Force chunk loading for block access
        if (world instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.gen.ChunkProviderServer provider = ((net.minecraft.world.WorldServer) world).getChunkProvider();
            if (!provider.chunkExists(chunkX, chunkZ)) {
                provider.loadChunk(chunkX, chunkZ);
            }
        }

        try {
            com.github.fnar.minecraft.WorldEditor1_12 editor = new com.github.fnar.minecraft.WorldEditor1_12(world);
            greymerk.roguelike.dungeon.Dungeon dungeon = new greymerk.roguelike.dungeon.Dungeon(editor);
            greymerk.roguelike.worldgen.Coord coord = new greymerk.roguelike.worldgen.Coord(x, 0, z);

            // 1. Terrain Check (not water, solid below, overhead free, vanilla structures distance)
            if (!dungeon.canGenerateDungeonHere(coord)) {
                return false;
            }

            // 2. Biome & Settings Check (has valid template)
            greymerk.roguelike.dungeon.settings.SettingsResolver resolver = 
                greymerk.roguelike.dungeon.settings.SettingsResolver.getInstance(editor.getModLoader());
            
            java.util.Optional<?> settings = resolver.chooseRandom(editor, coord);
            return settings.isPresent();

        } catch (Throwable t) {
            // Fallback to basic biome check if anything fails
            BlockPos pos = new BlockPos(x, 64, z);
            Biome biome = Reflect.getBiome(world, pos);
            if (biome != null && biome.getRegistryName() != null) {
                String name = biome.getRegistryName().toString().toLowerCase();
                return !name.contains("ocean") && !name.contains("coral_reef") && !name.contains("kelp_forest");
            }
            return true;
        }
    }
}
