package com.apocollis.aqtweaks.aurorian;

/** Pure arithmetic of a column of stacked snow layer blocks (8 layers per block). No Minecraft types. */
public final class SnowStack {
    private SnowStack() {}

    public static final int PER_BLOCK = 8;

    /** Total layers from the blocks of a column read bottom up (a full 8 layer block continues the stack). */
    public static int depth(int... layersBottomUp) {
        int d = 0;
        for (int l : layersBottomUp) {
            if (l <= 0) break;
            d += Math.min(PER_BLOCK, l);
            if (l < PER_BLOCK) break;
        }
        return d;
    }

    /** Index (from the bottom) of the block that receives the next layer when the column is {@code depth} deep. */
    public static int addBlockIndex(int depth) {
        return Math.max(0, depth) / PER_BLOCK;
    }

    /** Layers that block holds after the next layer is added. */
    public static int layersAfterAdd(int depth) {
        return Math.max(0, depth) % PER_BLOCK + 1;
    }

    /** Layers held by block {@code blockIndex} of a column {@code depth} deep. */
    public static int layersIn(int depth, int blockIndex) {
        return Math.max(0, Math.min(PER_BLOCK, depth - PER_BLOCK * blockIndex));
    }
}
