package com.apocollis.aqtweaks.depths;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/**
 * Block constants for the Depths carve and seam passes (formerly looked up by registry name through
 * {@code Reflect} on every call). Vanilla blocks are plain constants; Deepslate is Depths Update's
 * block with stone as the fallback, resolved once on first use (worldgen runs after registries).
 */
public final class DepthsBlocks {

    public static final Block AIR = Blocks.AIR;
    public static final Block BEDROCK = Blocks.BEDROCK;
    public static final Block LAVA = Blocks.LAVA;
    public static final IBlockState AIR_STATE = Blocks.AIR.getDefaultState();
    public static final IBlockState BEDROCK_STATE = Blocks.BEDROCK.getDefaultState();
    public static final IBlockState LAVA_STATE = Blocks.LAVA.getDefaultState();

    private DepthsBlocks() {}

    private static final class Deepslate {
        static final Block BLOCK = resolve();
        static final IBlockState STATE = BLOCK.getDefaultState();

        private static Block resolve() {
            Block block = Block.getBlockFromName("depthsupdate:deepslate");
            return block != null && block != Blocks.AIR ? block : Blocks.STONE;
        }
    }

    public static Block deepslate() {
        return Deepslate.BLOCK;
    }

    public static IBlockState deepslateState() {
        return Deepslate.STATE;
    }
}
