package com.apocollis.aqtweaks.world;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Light diagnostics. {@code /aqlight} reports the light at the block you look at; {@code /aqlight scan [radius]}
 * runs RTG's column test (the overhang rule) over the chunks around you and counts the cells that would be repaired;
 * {@code /aqlight fix [direct|wool] [radius] [all]} repairs them on the spot so you can see whether the light
 * returns on your client. All values are server-side: a cell that reads fine here but looks black on your client is
 * client-stale light (the direct repair sends no block-change packet).
 *
 * <p>The scan mirrors RTG 9.0.3 {@code fixLighting(Chunk, x, z)}: skip a column whose block at Y 255 is not air;
 * from Y 255 down to Y 49, remember the last air cell, and at each opaque block compare the sky light at that air cell
 * with the brightest in-chunk neighbour's sky light minus 1 (neighbours outside the chunk read as 0). {@code all}
 * also counts and repairs what RTG misses: cells below Y 49 and cells where only a neighbour across the chunk edge
 * is brighter.
 */
public final class CommandAqLight extends CommandBase {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-Light");
    private static final int RTG_MIN_Y = 49;
    private static final int TOP_Y = 255;

    @Override
    public String getName() {
        return "aqlight";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/aqlight | /aqlight scan [radius] | /aqlight fix [direct|wool] [radius] [all]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        World world = player.world;
        if (args.length == 0) {
            look(player, world);
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("scan".equals(sub)) {
            int radius = args.length > 1 ? parseInt(args[1], 0, 4) : 2;
            boolean all = args.length > 2 && "all".equalsIgnoreCase(args[2]);
            scanOrFix(player, world, radius, all, null);
        } else if ("fix".equals(sub)) {
            String method = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "direct";
            if (!"direct".equals(method) && !"wool".equals(method)) {
                throw new CommandException(getUsage(sender));
            }
            int radius = args.length > 2 ? parseInt(args[2], 0, 4) : 2;
            boolean all = args.length > 3 && "all".equalsIgnoreCase(args[3]);
            scanOrFix(player, world, radius, all, method);
        } else {
            throw new CommandException(getUsage(sender));
        }
    }

    private static void say(ICommandSender sender, String line) {
        sender.sendMessage(new TextComponentString(line));
        LOGGER.info("[AQ-LIGHT] {}", line);
    }

    // --- look-at report ---

    private static void look(EntityPlayerMP player, World world) {
        RayTraceResult hit = player.rayTrace(64.0, 1.0F);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            say(player, "No block in range. Look at a block (64 blocks).");
            return;
        }
        BlockPos block = hit.getBlockPos();
        EnumFacing side = hit.sideHit == null ? EnumFacing.UP : hit.sideHit;
        report(player, world, block, "target");
        report(player, world, block.offset(side), "in front");
    }

    private static void report(ICommandSender sender, World world, BlockPos pos, String label) {
        if (!world.isBlockLoaded(pos)) {
            say(sender, label + " " + fmt(pos) + ": chunk not loaded");
            return;
        }
        Chunk chunk = world.getChunk(pos);
        IBlockState state = world.getBlockState(pos);
        int sky = world.getLightFor(EnumSkyBlock.SKY, pos);
        int blockLight = world.getLightFor(EnumSkyBlock.BLOCK, pos);
        int n = world.getLightFor(EnumSkyBlock.SKY, pos.north());
        int e = world.getLightFor(EnumSkyBlock.SKY, pos.east());
        int s = world.getLightFor(EnumSkyBlock.SKY, pos.south());
        int w = world.getLightFor(EnumSkyBlock.SKY, pos.west());
        int up = world.getLightFor(EnumSkyBlock.SKY, pos.up());
        int height = chunk.getHeightValue(pos.getX() & 15, pos.getZ() & 15);
        int maxWorld = Math.max(Math.max(n, e), Math.max(s, w));
        int maxChunk = inChunkNeighbourSky(chunk, pos.getX() & 15, pos.getY(), pos.getZ() & 15);
        boolean air = state.getBlock() == Blocks.AIR;
        say(sender, String.format(Locale.ROOT,
                "%s %s dim=%d chunk=%d,%d %s | sky=%d block=%d | neighbours N%d E%d S%d W%d up%d | height=%d (%s) canSeeSky=%b"
                        + " | lightPopulated=%b terrainPopulated=%b queued=%b",
                label, fmt(pos), world.provider.getDimension(), chunk.x, chunk.z,
                state.getBlock().getRegistryName(), sky, blockLight, n, e, s, w, up, height,
                pos.getY() < height ? "below the height map, under cover" : "at or above the height map",
                world.canSeeSky(pos), chunk.isLightPopulated(), chunk.isTerrainPopulated(),
                RtgLightFixQueue.isQueued(world, chunk.getPos())));
        if (air) {
            say(sender, String.format(Locale.ROOT,
                    "  RTG rule (in-chunk neighbours): sky %d < %d - 1 -> %s | with neighbours across the chunk edge: sky %d < %d - 1 -> %s",
                    sky, maxChunk, sky < maxChunk - 1 ? "WOULD REPAIR" : "no", sky, maxWorld, sky < maxWorld - 1 ? "would repair" : "no"));
        }
    }

    /** Brightest sky light of the four orthogonal neighbours inside the chunk; outside x/z 0..15 reads 0 (as RTG does). */
    private static int inChunkNeighbourSky(Chunk chunk, int x, int y, int z) {
        return Math.max(Math.max(skyIn(chunk, x - 1, y, z), skyIn(chunk, x + 1, y, z)),
                Math.max(skyIn(chunk, x, y, z - 1), skyIn(chunk, x, y, z + 1)));
    }

    private static int skyIn(Chunk chunk, int x, int y, int z) {
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y > 255) return 0;
        return chunk.getLightFor(EnumSkyBlock.SKY, new BlockPos(x, y, z));
    }

    private static String fmt(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    // --- scan and fix ---

    private static final class Cell {
        final BlockPos pos;
        final boolean rtgRange;
        final boolean inChunkRule;

        Cell(BlockPos pos, boolean rtgRange, boolean inChunkRule) {
            this.pos = pos;
            this.rtgRange = rtgRange;
            this.inChunkRule = inChunkRule;
        }
    }

    private static void scanOrFix(EntityPlayerMP player, World world, int radius, boolean all, String method) {
        int pcx = MathHelper.floor(player.posX) >> 4;
        int pcz = MathHelper.floor(player.posZ) >> 4;
        long t0 = System.nanoTime();
        int chunks = 0;
        int rtgInChunk = 0;
        int crossEdgeExtra = 0;
        int below = 0;
        List<Cell> toFix = new ArrayList<>();
        List<String> samples = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(pcx + dx, pcz + dz);
                if (chunk == null) continue;
                chunks++;
                Set<Long> seen = new LinkedHashSet<>();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        if (chunk.getBlockState(x, TOP_Y, z).getBlock() != Blocks.AIR) continue;
                        int lastAir = TOP_Y;
                        for (int y = TOP_Y; y >= 1; y--) {
                            IBlockState state = chunk.getBlockState(x, y, z);
                            Block b = state.getBlock();
                            if (b == Blocks.AIR) {
                                lastAir = y;
                            } else if (state.isOpaqueCube()) {
                                int sky = skyIn(chunk, x, lastAir, z);
                                int inChunk = inChunkNeighbourSky(chunk, x, lastAir, z);
                                boolean ruleIn = sky < inChunk - 1;
                                BlockPos pos = new BlockPos(chunk.x * 16 + x, lastAir, chunk.z * 16 + z);
                                int cross = Math.max(
                                        Math.max(world.getLightFor(EnumSkyBlock.SKY, pos.north()), world.getLightFor(EnumSkyBlock.SKY, pos.south())),
                                        Math.max(world.getLightFor(EnumSkyBlock.SKY, pos.east()), world.getLightFor(EnumSkyBlock.SKY, pos.west())));
                                boolean ruleCross = sky < cross - 1;
                                boolean inRtgRange = y >= RTG_MIN_Y;
                                if (!ruleIn && !ruleCross) continue;
                                if (!seen.add(pos.toLong())) continue;
                                if (inRtgRange && ruleIn) {
                                    rtgInChunk++;
                                    toFix.add(new Cell(pos, true, true));
                                } else if (inRtgRange) {
                                    crossEdgeExtra++;
                                    if (all) toFix.add(new Cell(pos, true, false));
                                } else {
                                    below++;
                                    if (all) toFix.add(new Cell(pos, false, ruleIn));
                                }
                                if (samples.size() < 8) {
                                    samples.add(fmt(pos) + " sky=" + sky + " inChunk=" + inChunk + " cross=" + cross);
                                }
                            }
                        }
                    }
                }
            }
        }
        double ms = (System.nanoTime() - t0) / 1_000_000.0;
        say(player, String.format(Locale.ROOT,
                "scan dim=%d around chunk %d,%d radius %d: %d chunks, %d cells RTG's test would repair (Y>=49, in-chunk rule), "
                        + "%d more only with neighbours across the chunk edge, %d below Y 49 (RTG does not scan there); %.0f ms",
                world.provider.getDimension(), pcx, pcz, radius, chunks, rtgInChunk, crossEdgeExtra, below, ms));
        for (String sample : samples) {
            say(player, "  " + sample);
        }
        if (method == null) return;
        int repaired = 0;
        for (Cell cell : toFix) {
            if (!all && !(cell.rtgRange && cell.inChunkRule)) continue;
            if (!world.isBlockLoaded(cell.pos)) continue;
            if ("wool".equals(method)) {
                world.setBlockState(cell.pos, Blocks.WOOL.getDefaultState(), 3);
                world.setBlockToAir(cell.pos);
            } else {
                world.checkLightFor(EnumSkyBlock.SKY, cell.pos);
            }
            repaired++;
        }
        say(player, "fix (" + method + (all ? ", all" : "") + "): " + repaired
                + " cells. Look at the area; if the server light is now right but your client still shows black, it is client-stale (direct sends no packet).");
    }
}
