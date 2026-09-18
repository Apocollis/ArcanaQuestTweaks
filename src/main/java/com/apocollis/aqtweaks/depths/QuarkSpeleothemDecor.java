package com.apocollis.aqtweaks.depths;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.chunk.ChunkPrimer;
import vazkii.quark.world.block.BlockSpeleothem;
import vazkii.quark.world.block.BlockSpeleothem.EnumSize;
import vazkii.quark.world.feature.Basalt;
import vazkii.quark.world.feature.RevampStoneGen;
import vazkii.quark.world.feature.Speleothems;

import java.util.Random;

/**
 * Quark-style speleothem clusters in the Depths lower cavern primer.
 *
 * <p>Stock {@code SpeleothemGenerator} only samples Y 6–55 and stops walking at Y 4, and Tweaks
 * does not trust chunk writes below 0. This pass stays in-chunk and writes the primer only.
 * Call only after {@code Loader.isModLoaded("quark")}.
 */
public final class QuarkSpeleothemDecor {

    private static final int TRIES = 60;
    private static final int CLUSTER_COUNT = 10;
    private static final int INNER_SPREAD = 6;
    private static final int WALK_LIMIT = 10;
    private static final int AIR_MIN_Y = -59;
    private static final int AIR_MAX_Y = -24;
    private static final int WALK_MIN_Y = -60;
    private static final int WALK_MAX_Y = -1;
    private static final int AIR_SPAN = AIR_MAX_Y - AIR_MIN_Y + 1;

    private QuarkSpeleothemDecor() {}

    public static void decorate(ChunkPrimer primer, int chunkX, int chunkZ, long worldSeed) {
        if (primer == null) return;
        if (Speleothems.stone_speleothem == null) return;

        Random random = new Random(worldSeed
                ^ ((long) chunkX * 341873128712L)
                ^ ((long) chunkZ * 132897987541L)
                ^ 0x51E10E1L);

        Block airBlock = Reflect.getAirBlock();
        Block bedrockBlock = Reflect.getBedrockBlock();

        for (int i = 0; i < TRIES; i++) {
            int lx = random.nextInt(16);
            int lz = random.nextInt(16);
            int y = AIR_MIN_Y + random.nextInt(AIR_SPAN);
            if (placeCluster(random, primer, lx, lz, y, airBlock, bedrockBlock)) {
                i++;
            }
        }
    }

    private static boolean placeCluster(Random random, ChunkPrimer primer, int lx, int lz, int y,
                                        Block airBlock, Block bedrockBlock) {
        if (!findAndPlace(random, primer, lx, lz, y, airBlock, bedrockBlock)) {
            return false;
        }
        for (int n = 0; n < CLUSTER_COUNT; n++) {
            int tx = lx + random.nextInt(INNER_SPREAD * 2 + 1) - INNER_SPREAD;
            int ty = y + random.nextInt(INNER_SPREAD + 1) - INNER_SPREAD;
            int tz = lz + random.nextInt(INNER_SPREAD * 2 + 1) - INNER_SPREAD;
            if (tx < 0 || tx > 15 || tz < 0 || tz > 15) continue;
            findAndPlace(random, primer, tx, tz, ty, airBlock, bedrockBlock);
        }
        return true;
    }

    private static boolean findAndPlace(Random random, ChunkPrimer primer, int lx, int lz, int y,
                                        Block airBlock, Block bedrockBlock) {
        if (y < AIR_MIN_Y || y > AIR_MAX_Y) return false;
        if (isRoofBreachColumn(primer, lx, lz, airBlock)) return false;
        if (!isAir(primer, lx, y, lz, airBlock)) return false;

        boolean walkUp = random.nextBoolean();
        EnumFacing walk = walkUp ? EnumFacing.UP : EnumFacing.DOWN;
        int off = 0;
        int cx = lx;
        int cy = y;
        int cz = lz;
        IBlockState at;
        do {
            cy += walk.getYOffset();
            off++;
            if (cy <= WALK_MIN_Y || cy >= WALK_MAX_Y || off > WALK_LIMIT) return false;
            at = PrimerAccess.getBlockState(primer, cx, cy, cz);
        } while (!isFullSupport(at, bedrockBlock));

        Block type = speleothemFor(at);
        if (type == null) return false;

        boolean placeUp = !walkUp;
        placeSpeleothem(random, primer, cx, cy, cz, type, placeUp, airBlock);
        return true;
    }

    private static void placeSpeleothem(Random random, ChunkPrimer primer, int lx, int supportY, int lz,
                                        Block type, boolean up, Block airBlock) {
        int dir = up ? 1 : -1;
        int size = random.nextInt(3) == 0 ? 2 : 3;
        if (!up && random.nextInt(20) == 0) {
            size = 1;
        }
        int y = supportY;
        for (int i = 0; i < size; i++) {
            y += dir;
            if (y < WALK_MIN_Y || y > WALK_MAX_Y) return;
            if (!isAir(primer, lx, y, lz, airBlock)) return;
            EnumSize sizeType = EnumSize.values()[size - i - 1];
            PrimerAccess.setBlockState(primer, lx, y, lz, withSize(type, sizeType));
        }
    }

    @SuppressWarnings("unchecked")
    private static IBlockState withSize(Block type, EnumSize size) {
        return type.getDefaultState().withProperty(BlockSpeleothem.SIZE, size);
    }

    /**
     * Roof shell Y -27..-23 is solid except sparse lower-breach shafts. Skip those columns so
     * speleothems do not hang in the shaft.
     */
    private static boolean isRoofBreachColumn(ChunkPrimer primer, int lx, int lz, Block airBlock) {
        int open = 0;
        for (int y = -27; y <= -23; y++) {
            if (isAir(primer, lx, y, lz, airBlock)) open++;
        }
        return open >= 2;
    }

    private static boolean isAir(ChunkPrimer primer, int x, int y, int z, Block airBlock) {
        Block b = PrimerAccess.getBlock(PrimerAccess.getBlockState(primer, x, y, z));
        if (b == null) return true;
        return airBlock != null && b == airBlock;
    }

    private static boolean isFullSupport(IBlockState state, Block bedrockBlock) {
        if (state == null) return false;
        Block b = PrimerAccess.getBlock(state);
        if (b == null || b == Blocks.AIR) return false;
        if (bedrockBlock != null && b == bedrockBlock) return false;
        try {
            return state.isFullBlock();
        } catch (Throwable t) {
            return false;
        }
    }

    private static Block speleothemFor(IBlockState state) {
        Block block = PrimerAccess.getBlock(state);
        if (block == null) return null;

        Block deepslate = Reflect.getDeepslateBlock();
        if (deepslate != null && block == deepslate) {
            return Speleothems.stone_speleothem;
        }
        if (block == Blocks.STONE) {
            try {
                switch (state.getValue(BlockStone.VARIANT)) {
                    case STONE:
                        return Speleothems.stone_speleothem;
                    case ANDESITE:
                        return Speleothems.andesite_speleothem;
                    case GRANITE:
                        return Speleothems.granite_speleothem;
                    case DIORITE:
                        return Speleothems.diorite_speleothem;
                    default:
                        return Speleothems.stone_speleothem;
                }
            } catch (Throwable t) {
                return Speleothems.stone_speleothem;
            }
        }
        if (block == Basalt.basalt) return Speleothems.basalt_speleothem;
        if (block == RevampStoneGen.marble) return Speleothems.marble_speleothem;
        if (block == RevampStoneGen.limestone) return Speleothems.limestone_speleothem;
        if (block == RevampStoneGen.jasper) return Speleothems.jasper_speleothem;
        if (block == RevampStoneGen.slate) return Speleothems.slate_speleothem;
        return null;
    }
}
