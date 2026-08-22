package com.apocollis.aqtweaks.rtg;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenVillage;
import rtg.world.gen.ChunkGeneratorRTG;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * OP {@code /aqvillage}: teleport onto the village plate, a few blocks off the well.
 * Prefers an unexplored allowed well; falls back to a known Start.
 */
public final class CommandAqVillage extends CommandBase {

    private static final int CELL_RADIUS = 16;
    private static final int STAND_OFFSET = 6;
    private static final int[][] STAND_OFFSETS = {
            {STAND_OFFSET, 2},
            {2, STAND_OFFSET},
            {-STAND_OFFSET, 2},
            {2, -STAND_OFFSET}
    };

    @Override
    public String getName() {
        return "aqvillage";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("aqtvillage");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aqvillage [unexplored|known]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException("Only a player can teleport with /aqvillage");
        }
        Mode mode = Mode.PREFER_UNEXPLORED;
        if (args.length > 1) {
            throw new CommandException(getUsage(sender));
        }
        if (args.length == 1) {
            if ("unexplored".equalsIgnoreCase(args[0])) {
                mode = Mode.UNEXPLORED_ONLY;
            } else if ("known".equalsIgnoreCase(args[0])) {
                mode = Mode.KNOWN_ONLY;
            } else {
                throw new CommandException(getUsage(sender));
            }
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        World world = player.world;
        if (world == null || world.isRemote) {
            throw new CommandException("No server world");
        }
        Object rawGen = StructureVillageOverlap.findVillageGenerator(world);
        if (!(rawGen instanceof MapGenVillage)) {
            Object chunkGen = Reflect.getChunkGenerator(world);
            VillageDebug.log("aqvillage missing gen provider=%s generator=%s",
                    world.getChunkProvider() != null ? world.getChunkProvider().getClass().getName() : "null",
                    chunkGen != null ? chunkGen.getClass().getName() : "null");
            throw new CommandException("No village generator on this world");
        }
        MapGenVillage village = (MapGenVillage) rawGen;
        Reflect.setMapGenWorld(village, world);
        Reflect.initializeStructureData(village, world);

        ChunkGeneratorRTG rtg = StructureVillageOverlap.findRtgGenerator(world);
        boolean pushedGen = false;
        VillageLandHelper.pushWorld(world);
        if (rtg != null) {
            VillageLandHelper.pushGenerator(rtg);
            pushedGen = true;
        }
        Hit hit;
        try {
            hit = findHit(world, village, player.getPosition(), mode);
        } finally {
            if (pushedGen) {
                VillageLandHelper.popGenerator();
            }
            VillageLandHelper.popWorld();
        }
        if (hit == null) {
            throw new CommandException(mode == Mode.UNEXPLORED_ONLY
                    ? "No unexplored village well in range"
                    : "No allowed village well found");
        }

        world.getChunk(hit.chunkX, hit.chunkZ);
        int wellX = hit.wellX;
        int wellZ = hit.wellZ;
        int[] stand = pickStand(world, hit);
        int standX = stand[0];
        int standZ = stand[1];
        int y = stand[2];
        world.getChunk(standX >> 4, standZ >> 4);
        Biome biome = Reflect.getBiome(world.getBiomeProvider(), wellX, wellZ);
        String kind = hit.unexplored ? "unexplored" : "known";
        TextComponentString message = new TextComponentString(String.format(
                "Village well (%s) at %d %d %d biome=%s",
                kind, wellX, y, wellZ, VillageLandHelper.biomeId(biome)));
        message.getStyle().setColor(TextFormatting.GOLD);
        player.sendMessage(message);
        player.setPositionAndUpdate(standX + 0.5, y, standZ + 0.5);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "unexplored", "known");
        }
        return Collections.emptyList();
    }

    private static Hit findHit(World world, MapGenVillage village, BlockPos from, Mode mode) {
        Hit unexplored = mode == Mode.KNOWN_ONLY ? null : nearestUnexplored(world, village, from);
        if (unexplored != null && mode != Mode.KNOWN_ONLY) {
            return unexplored;
        }
        if (mode == Mode.UNEXPLORED_ONLY) {
            return null;
        }
        return nearestKnown(world, village, from);
    }

    private static Hit nearestUnexplored(World world, MapGenVillage village, BlockPos from) {
        int spacing = Reflect.getVillageDistance(village);
        if (spacing < 9) spacing = 32;
        int minTown = Reflect.getVillageMinDistance(village);
        if (minTown < 1 || minTown >= spacing) minTown = 8;
        int originCx = from.getX() >> 4;
        int originCz = from.getZ() >> 4;
        int originCellX = VillageLandHelper.villageCell(originCx, spacing);
        int originCellZ = VillageLandHelper.villageCell(originCz, spacing);
        long seed = Reflect.getSeed(world);
        Hit best = null;
        double bestDist = Double.MAX_VALUE;
        for (int cellX = originCellX - CELL_RADIUS; cellX <= originCellX + CELL_RADIUS; cellX++) {
            for (int cellZ = originCellZ - CELL_RADIUS; cellZ <= originCellZ + CELL_RADIUS; cellZ++) {
                int[] well = VillageLandHelper.villageWellChunk(seed, cellX, cellZ, spacing, minTown);
                int gx = well[0];
                int gz = well[1];
                if (world.isChunkGeneratedAt(gx, gz) || Reflect.hasStructureStart(village, gx, gz)) {
                    continue;
                }
                if (!Reflect.canSpawnVillage(village, gx, gz)) {
                    continue;
                }
                int[] wellXZ = VillageLandHelper.resolvedWellForChunk(world, gx, gz);
                double dist = distanceSq(from, wellXZ[0], wellXZ[1]);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new Hit(gx, gz, wellXZ[0], wellXZ[1], true);
                }
            }
        }
        return best;
    }

    private static Hit nearestKnown(World world, MapGenVillage village, BlockPos from) {
        Hit best = null;
        double bestDist = Double.MAX_VALUE;
        for (Object start : Reflect.getMapGenStructureStarts(village)) {
            int cx = Reflect.getStructureStartChunkX(start);
            int cz = Reflect.getStructureStartChunkZ(start);
            if (cx == Integer.MIN_VALUE || cz == Integer.MIN_VALUE) continue;
            String reason = VillageLandHelper.startRejectReason(world, cx, cz);
            if (reason != null) continue;
            int[] wellXZ = VillageLandHelper.resolvedWellForChunk(world, cx, cz);
            for (VillagePlate.Record rec : VillagePlate.starts(Reflect.getSeed(world))) {
                if (rec.start == start) {
                    wellXZ = new int[] {rec.wellX, rec.wellZ};
                    break;
                }
            }
            double dist = distanceSq(from, wellXZ[0], wellXZ[1]);
            if (dist < bestDist) {
                bestDist = dist;
                best = new Hit(cx, cz, wellXZ[0], wellXZ[1], false);
            }
        }
        return best;
    }

    private static int[] pickStand(World world, Hit hit) {
        int plateFeet = plateFeetY(world, hit);
        int[] fallback = null;
        for (int[] offset : STAND_OFFSETS) {
            int x = hit.wellX + offset[0];
            int z = hit.wellZ + offset[1];
            world.getChunk(x >> 4, z >> 4);
            int y = plateFeet > 0 ? plateFeet : standY(world, x, z);
            if (fallback == null) {
                fallback = new int[] {x, z, y};
            }
            if (isStandSafe(world, x, y, z)) {
                return new int[] {x, z, y};
            }
        }
        return fallback != null ? fallback : new int[] {hit.wellX + STAND_OFFSET, hit.wellZ + 2, Math.max(1, plateFeet)};
    }

    private static int plateFeetY(World world, Hit hit) {
        long seed = Reflect.getSeed(world);
        for (VillagePlate.Record rec : VillagePlate.starts(seed)) {
            boolean match = rec.wellX == hit.wellX && rec.wellZ == hit.wellZ;
            if (!match && rec.start != null) {
                match = Reflect.getStructureStartChunkX(rec.start) == hit.chunkX
                        && Reflect.getStructureStartChunkZ(rec.start) == hit.chunkZ;
            }
            if (!match) continue;
            float plate = VillagePlate.resolvePlate(world, rec.xz);
            if (!Float.isNaN(plate)) {
                return Math.round(plate) + 1;
            }
        }
        return 0;
    }

    private static boolean isStandSafe(World world, int x, int y, int z) {
        if (y < 2 || y > 254) return false;
        if (VillageLandHelper.isNeverRaiseAt(world, x, z)) return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y - 1, z);
        IBlockState below = world.getBlockState(pos);
        if (below.getMaterial().isLiquid() || !below.getMaterial().blocksMovement()) return false;
        if (below.getBlock().isLeaves(below, world, pos)) return false;
        pos.setPos(x, y, z);
        IBlockState feet = world.getBlockState(pos);
        pos.setPos(x, y + 1, z);
        IBlockState head = world.getBlockState(pos);
        return !feet.getMaterial().blocksMovement() && !head.getMaterial().blocksMovement()
                && !feet.getMaterial().isLiquid() && !head.getMaterial().isLiquid();
    }

    private static double distanceSq(BlockPos from, int x, int z) {
        double dx = from.getX() - x;
        double dz = from.getZ() - z;
        return dx * dx + dz * dz;
    }

    private static int standY(World world, int x, int z) {
        int y = Math.max(1, world.getHeight(x, z));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y - 1, z);
        while (y > 1) {
            pos.setPos(x, y - 1, z);
            IBlockState below = world.getBlockState(pos);
            if (below.getMaterial().blocksMovement() && !below.getBlock().isLeaves(below, world, pos)) {
                break;
            }
            y--;
        }
        return y;
    }

    private enum Mode {
        PREFER_UNEXPLORED,
        UNEXPLORED_ONLY,
        KNOWN_ONLY
    }

    private static final class Hit {
        final int chunkX;
        final int chunkZ;
        final int wellX;
        final int wellZ;
        final boolean unexplored;

        Hit(int chunkX, int chunkZ, int wellX, int wellZ, boolean unexplored) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.wellX = wellX;
            this.wellZ = wellZ;
            this.unexplored = unexplored;
        }
    }
}
