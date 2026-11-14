package com.apocollis.aqtweaks.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.weather.LayerAccumulator;
import com.teammetallurgy.atum.blocks.BlockSandLayers;
import com.teammetallurgy.atum.init.AtumBiomes;
import com.teammetallurgy.atum.init.AtumBlocks;
import com.teammetallurgy.atum.world.WorldProviderAtum;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

/**
 * Sand that piles up while an Atum sandstorm rages: smooth, wind-shadowed {@code atum:sand_layer} (Atum's Strange Sand
 * layers) accumulation across the loaded chunks around players, up to {@code Max Sand Layers} (default 6, the deepest
 * that is still walked over: collision is {@code (layers - 2) x 0.125}). It replaces Atum's own weather sand loop (see
 * {@code MixinWorldProviderAtum}), which walks every chunk every tick and stops at three layers. Nothing is ever
 * removed: no clear-weather erosion, so naturally placed gradients and dunes outside storms are never touched.
 */
public class AtumSandHandler extends LayerAccumulator {

    @Override
    protected boolean worldMatches(World world) {
        return world.provider instanceof WorldProviderAtum;
    }

    @Override
    protected boolean enabled() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.enableSandAccumulation;
    }

    @Override
    protected float strength(WorldServer world) {
        return ((WorldProviderAtum) world.provider).stormStrength;
    }

    @Override
    protected double ratePerChunkPerSecond() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.sandLayersPerChunkPerSecond;
    }

    @Override
    protected int maxChangesPerTick() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.sandMaxChangesPerTick;
    }

    @Override
    protected int radius() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.sandAccumulationRadius;
    }

    @Override
    protected int cap() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.maxSandLayers;
    }

    @Override
    protected int step() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.sandMaxStep;
    }

    @Override
    protected boolean windShadow() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.sandWindShadow;
    }

    @Override
    protected double shadowAngle() {
        return ArcanaQuestTweaksConfig.AtumModuleConfig.sandWindShadowAngle;
    }

    @Override
    protected Vec3d wind(WorldServer world) {
        return AtumWindHelper.getWindVector(world, true);
    }

    @Override
    protected boolean isLayerBlock(IBlockState state) {
        return state.getBlock() == AtumBlocks.SAND_LAYERED;
    }

    @Override
    protected int layersOf(IBlockState state) {
        return state.getValue(BlockSandLayers.LAYERS);
    }

    @Override
    protected IBlockState withLayers(IBlockState state, int layers) {
        return state.withProperty(BlockSandLayers.LAYERS, layers);
    }

    @Override
    protected IBlockState firstLayer() {
        return AtumBlocks.SAND_LAYERED.getDefaultState();
    }

    @Override
    protected boolean canPlaceLayer(WorldServer world, BlockPos pos) {
        return AtumBlocks.SAND_LAYERED.canPlaceBlockAt(world, pos);
    }

    /**
     * Read before the stack is climbed, at the ground block under the bottom layer: oasis sanctuaries stay clear, and
     * so do the cracked limestone floors of pyramid traps. Strange Sand and limestone gravel accumulate (the stock
     * rule skipped them).
     */
    @Override
    protected boolean columnAllowed(WorldServer world, Chunk chunk, int x, int y, int z) {
        BlockPos at = new BlockPos(x, y, z);
        if (ArcanaQuestTweaksConfig.AtumModuleConfig.skipOasisBiome
                && chunk.getBiome(at, world.getBiomeProvider()) == AtumBiomes.OASIS) {
            return false;
        }
        Block base = chunk.getBlockState(x, y - 1, z).getBlock();
        if (base == AtumBlocks.LIMESTONE_CRACKED) return false;
        if (!ArcanaQuestTweaksConfig.AtumModuleConfig.accumulateOnStrangeSand
                && (base == AtumBlocks.SAND || base == AtumBlocks.LIMESTONE_GRAVEL)) {
            return false;
        }
        return true;
    }
}
