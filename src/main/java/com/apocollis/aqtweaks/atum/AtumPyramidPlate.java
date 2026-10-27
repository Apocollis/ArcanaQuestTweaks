package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.teammetallurgy.atum.world.ChunkGeneratorAtum;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One Atum pyramid plateau. {@code plateY} is the dune apron. Template Y=8 sits {@code pyramidDoorLift}
 * blocks above it. The 32×32 footprint follows the template rotation.
 * Atum has no sandstone block, so the cap under the sand is limestone.
 */
public final class AtumPyramidPlate {

    public static final int FOOTPRINT = 32;
    /** Template cells below the exterior base. */
    public static final int BASE_TEMPLATE_Y = 8;
    public static final int MIN_PLATE_Y = 55;

    private static final ThreadLocal<Sample> PREPARE = new ThreadLocal<>();
    private static final ConcurrentHashMap<Key, AtumPyramidPlate> PLATES = new ConcurrentHashMap<>();

    public final int dimension;
    public final int minX;
    public final int maxX;
    public final int minZ;
    public final int maxZ;
    public final int plateY;
    public final EnumFacing entranceOut;

    private AtumPyramidPlate(int dimension, int minX, int maxX, int minZ, int maxZ, int plateY, EnumFacing entranceOut) {
        this.dimension = dimension;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.plateY = plateY;
        this.entranceOut = entranceOut;
    }

    public static void prepare(World world, ChunkGeneratorAtum generator) {
        Sample sample = new Sample();
        sample.world = world;
        sample.generator = generator;
        PREPARE.set(sample);
    }

    public static void clearPrepare() {
        PREPARE.remove();
    }

    /**
     * @return the template origin. Template Y=8 is the plateau plus the door lift, so the entrance stairs stand on the apron
     */
    public static BlockPos retarget(BlockPos origin, Rotation rotation) {
        if (origin == null || !enabled()) return origin;
        Sample sample = PREPARE.get();
        int[] box = footprint(origin.getX(), origin.getZ(), rotation);
        int average = sample != null && sample.generator != null ? averageGround(sample.generator, box) : -1;
        int plateY = average > 0 ? Math.max(MIN_PLATE_Y, average) : Math.max(MIN_PLATE_Y, origin.getY() + BASE_TEMPLATE_Y);
        BlockPos raised = new BlockPos(origin.getX(), plateY - BASE_TEMPLATE_Y + doorLift(), origin.getZ());
        if (sample != null && sample.world != null) {
            register(sample.world, box, plateY, rotation);
        }
        return raised;
    }

    public static void adopt(World world, StructureStart start) {
        if (world == null || start == null || !enabled() || !start.isSizeableStructure()) return;
        StructureComponent template = null;
        for (StructureComponent component : start.getComponents()) {
            if (component != null && component.getClass().getName().endsWith("PyramidTemplate")) {
                template = component;
                break;
            }
        }
        if (template == null) return;
        StructureBoundingBox box = template.getBoundingBox();
        if (box == null || world.provider == null) return;
        if (PLATES.containsKey(new Key(world.provider.getDimension(), box.minX, box.minZ))) return;
        Rotation rotation = null;
        if (template instanceof com.apocollis.aqtweaks.mixin.atum.AccessorPyramidTemplate) {
            rotation = ((com.apocollis.aqtweaks.mixin.atum.AccessorPyramidTemplate) template).aqtweaks$rotation();
        }
        int plateY = box.minY + BASE_TEMPLATE_Y - doorLift();
        register(world, new int[] {box.minX, box.maxX, box.minZ, box.maxZ}, plateY, rotation);
    }

    public static List<AtumPyramidPlate> overlapping(int dimension, int chunkX, int chunkZ) {
        if (PLATES.isEmpty()) return Collections.emptyList();
        int margin = margin();
        int x0 = chunkX << 4;
        int z0 = chunkZ << 4;
        List<AtumPyramidPlate> hits = null;
        for (AtumPyramidPlate plate : PLATES.values()) {
            if (plate.dimension != dimension) continue;
            if (plate.maxX + margin < x0 || plate.minX - margin > x0 + 15) continue;
            if (plate.maxZ + margin < z0 || plate.minZ - margin > z0 + 15) continue;
            if (hits == null) hits = new ArrayList<>(1);
            hits.add(plate);
        }
        return hits == null ? Collections.emptyList() : hits;
    }

    public boolean contains(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /** Euclidean distance outside the footprint. Zero inside. */
    public double distanceOutside(int x, int z) {
        int dx = 0;
        if (x < minX) dx = minX - x;
        else if (x > maxX) dx = x - maxX;
        int dz = 0;
        if (z < minZ) dz = minZ - z;
        else if (z > maxZ) dz = z - maxZ;
        if (dx == 0 && dz == 0) return 0.0D;
        return Math.sqrt(dx * (double) dx + dz * (double) dz);
    }

    /**
     * @return corridor distance starting at 1 on the first block outside the face, or 0 when the column is not in the walk
     */
    public int corridorDistance(int x, int z, int length) {
        if (entranceOut == null || length <= 0 || contains(x, z)) return 0;
        int along;
        int across;
        switch (entranceOut) {
            case NORTH:
                along = minZ - z;
                across = x - center(minX, maxX);
                break;
            case SOUTH:
                along = z - maxZ;
                across = x - center(minX, maxX);
                break;
            case WEST:
                along = minX - x;
                across = z - center(minZ, maxZ);
                break;
            case EAST:
                along = x - maxX;
                across = z - center(minZ, maxZ);
                break;
            default:
                return 0;
        }
        if (along < 1 || along > length) return 0;
        if (across < -2 || across > 3) return 0;
        return along;
    }

    private static int center(int min, int max) {
        return (min + max) >> 1;
    }

    private static void register(World world, int[] box, int plateY, Rotation rotation) {
        if (world == null || world.provider == null || box == null) return;
        int dimension = world.provider.getDimension();
        EnumFacing out = rotation == null ? null : rotation.rotate(EnumFacing.NORTH);
        AtumPyramidPlate plate = new AtumPyramidPlate(dimension, box[0], box[1], box[2], box[3], plateY, out);
        PLATES.put(new Key(dimension, box[0], box[2]), plate);
    }

    static int[] footprint(int originX, int originZ, Rotation rotation) {
        Rotation turn = rotation != null ? rotation : Rotation.NONE;
        BlockPos far = new BlockPos(FOOTPRINT - 1, 0, FOOTPRINT - 1).rotate(turn);
        int x1 = originX + far.getX();
        int z1 = originZ + far.getZ();
        return new int[] {
                Math.min(originX, x1), Math.max(originX, x1),
                Math.min(originZ, z1), Math.max(originZ, z1)
        };
    }

    private static int averageGround(ChunkGeneratorAtum generator, int[] box) {
        Map<Long, ChunkPrimer> primers = new HashMap<>();
        int sum = 0;
        int count = 0;
        int spanX = Math.max(1, box[1] - box[0]);
        int spanZ = Math.max(1, box[3] - box[2]);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                int x = box[0] + spanX * i / 4;
                int z = box[2] + spanZ * j / 4;
                int cx = x >> 4;
                int cz = z >> 4;
                long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
                ChunkPrimer primer = primers.get(key);
                if (primer == null) {
                    primer = new ChunkPrimer();
                    try {
                        generator.setBlocksInChunk(cx, cz, primer);
                    } catch (Throwable ignored) {
                        continue;
                    }
                    primers.put(key, primer);
                }
                int top = topSolid(primer, x & 15, z & 15);
                if (top <= 0) continue;
                sum += top;
                count++;
            }
        }
        if (count == 0) return -1;
        return (int) Math.round(sum / (double) count);
    }

    private static int topSolid(ChunkPrimer primer, int localX, int localZ) {
        for (int y = 255; y >= 1; y--) {
            net.minecraft.block.state.IBlockState state = primer.getBlockState(localX, y, localZ);
            if (state != null && state.getBlock() != net.minecraft.init.Blocks.AIR) return y;
        }
        return 0;
    }

    public static int margin() {
        int apron = ArcanaQuestTweaksConfig.AtumModuleConfig.pyramidApronBlocks;
        int falloff = ArcanaQuestTweaksConfig.AtumModuleConfig.pyramidFalloffBlocks;
        int corridor = ArcanaQuestTweaksConfig.AtumModuleConfig.enablePyramidEntranceCorridor
                ? ArcanaQuestTweaksConfig.AtumModuleConfig.pyramidEntranceCorridorLength : 0;
        return Math.max(0, apron) + Math.max(0, falloff) + Math.max(0, corridor);
    }

    static int doorLift() {
        int lift = ArcanaQuestTweaksConfig.AtumModuleConfig.pyramidDoorLift;
        if (lift < 0) return 0;
        return Math.min(lift, 8);
    }

    private static boolean enabled() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.enablePyramidTerrainSmoothing;
    }

    private static final class Sample {
        World world;
        ChunkGeneratorAtum generator;
    }

    private static final class Key {
        final int dimension;
        final int minX;
        final int minZ;

        Key(int dimension, int minX, int minZ) {
            this.dimension = dimension;
            this.minX = minX;
            this.minZ = minZ;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Key)) return false;
            Key key = (Key) other;
            return dimension == key.dimension && minX == key.minX && minZ == key.minZ;
        }

        @Override
        public int hashCode() {
            return dimension * 31 + minX * 17 + minZ;
        }
    }
}
